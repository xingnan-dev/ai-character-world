package com.companion.world;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.entity.CharacterWorld;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldRound;
import com.companion.entity.enums.WorldEventStatus;
import com.companion.entity.enums.WorldRoundStatus;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.WorldEventMapper;
import com.companion.mapper.WorldRoundMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
class WorldRoundLifecycleIntegrationTest {

    @Autowired private CharacterWorldMapper worldMapper;
    @Autowired private WorldRoundMapper roundMapper;
    @Autowired private WorldEventMapper eventMapper;
    @Autowired private WorldRoundLifecycleService lifecycle;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void claimsOnceRecoversExpiredLeaseAndFencesOldWorker() {
        WorldRound round = createRound("claim");
        WorldExecutionClaim first = lifecycle.claim(round.getWorldId(), round.getId());
        assertThat(first.acquired()).isTrue();
        assertThat(lifecycle.claim(round.getWorldId(), round.getId()).acquired()).isFalse();

        WorldRound expired = roundMapper.selectById(round.getId());
        expired.setLeaseUntil(LocalDateTime.now().minusSeconds(1));
        roundMapper.updateById(expired);
        WorldExecutionClaim recovered = lifecycle.claim(round.getWorldId(), round.getId());

        assertThat(recovered.acquired()).isTrue();
        assertThat(recovered.executionVersion()).isEqualTo(first.executionVersion() + 1);
        assertThat(lifecycle.renew(round.getId(), first.executionVersion())).isFalse();
        assertThat(lifecycle.saveEvent(round.getWorldId(), round.getId(), first.executionVersion(), 2, 101L,
                "late", "COMPLETED", null)).isFalse();
    }

    @Test
    void atomicInsertRejectsPausedOldWorkerAfterConcurrentTakeover() throws Exception {
        WorldRound round = createRound("atomic-fence");
        WorldExecutionClaim first = lifecycle.claim(round.getWorldId(), round.getId());
        CountDownLatch oldReadyToInsert = new CountDownLatch(1);
        CountDownLatch allowOldInsert = new CountDownLatch(1);
        AtomicReference<Boolean> oldResult = new AtomicReference<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            oldReadyToInsert.countDown();
            try {
                if (allowOldInsert.await(5, TimeUnit.SECONDS)) {
                    oldResult.set(lifecycle.saveEvent(round.getWorldId(), round.getId(),
                            first.executionVersion(), 2, 601L, "old", "COMPLETED", null));
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        });

        assertThat(oldReadyToInsert.await(5, TimeUnit.SECONDS)).isTrue();
        expireLease(round.getId());
        WorldExecutionClaim replacement = lifecycle.claim(round.getWorldId(), round.getId());
        assertThat(replacement.acquired()).isTrue();
        allowOldInsert.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(oldResult.get()).isFalse();
        assertThat(findParticipantEvent(round.getId(), 601L)).isNull();
        assertThat(lifecycle.saveEvent(round.getWorldId(), round.getId(),
                replacement.executionVersion(), 2, 602L, "new", "COMPLETED", null)).isTrue();
        assertThat(findParticipantEvent(round.getId(), 602L)).isNotNull();
    }

    @Test
    void expiredLeaseRejectsOwnerEvenBeforeAnotherWorkerTakesOver() {
        WorldRound round = createRound("expired-fence");
        WorldExecutionClaim claim = lifecycle.claim(round.getWorldId(), round.getId());
        expireLease(round.getId());

        assertThat(lifecycle.saveEvent(round.getWorldId(), round.getId(),
                claim.executionVersion(), 2, 701L, "expired", "COMPLETED", null)).isFalse();
        assertThat(findParticipantEvent(round.getId(), 701L)).isNull();
    }

    @Test
    void protectsOrderParticipantUniquenessAndCommitsEachEventIndependently() {
        WorldRound round = createRound("events");
        long version = lifecycle.claim(round.getWorldId(), round.getId()).executionVersion();
        assertThat(lifecycle.saveEvent(round.getWorldId(), round.getId(), version, 3, 202L,
                "out of order", "COMPLETED", null)).isFalse();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            assertThat(lifecycle.saveEvent(round.getWorldId(), round.getId(), version, 2, 201L,
                    "committed", "COMPLETED", null)).isTrue();
            throw new IllegalStateException("rollback caller");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(events(round.getId())).hasSize(2);

        WorldEvent duplicateParticipant = aiEvent(round.getId(), 3, 201L);
        assertThatThrownBy(() -> eventMapper.insert(duplicateParticipant))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> lifecycle.saveEvent(round.getWorldId(), round.getId(), version, 3, 201L,
                "wrong slot", "COMPLETED", null)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void computesCompletedPartialFailedFailedAndNoAiStatuses() {
        assertTerminal("complete", new String[]{"COMPLETED", "COMPLETED"}, "COMPLETED", null);
        assertTerminal("partial", new String[]{"COMPLETED", "FAILED"},
                "PARTIAL_FAILED", "WORLD_PARTICIPANT_FAILURE");
        assertTerminal("failed", new String[]{"FAILED", "FAILED"},
                "FAILED", "WORLD_ALL_PARTICIPANTS_FAILED");

        WorldRound empty = createRound("empty");
        long version = lifecycle.claim(empty.getWorldId(), empty.getId()).executionVersion();
        assertThat(lifecycle.finish(empty.getId(), version, 0)).isTrue();
        assertThat(roundMapper.selectById(empty.getId()).getErrorCode()).isEqualTo("WORLD_NO_AI_PARTICIPANT");
    }

    private void assertTerminal(String request, String[] eventStatuses,
                                String expectedStatus, String expectedError) {
        WorldRound round = createRound(request);
        long version = lifecycle.claim(round.getWorldId(), round.getId()).executionVersion();
        for (int index = 0; index < eventStatuses.length; index++) {
            String status = eventStatuses[index];
            assertThat(lifecycle.saveEvent(round.getWorldId(), round.getId(), version, index + 2, 300L + index,
                    status.equals("COMPLETED") ? "ok" : "", status,
                    status.equals("FAILED") ? "LLM_TIMEOUT" : null)).isTrue();
        }
        assertThat(lifecycle.finish(round.getId(), version, eventStatuses.length)).isTrue();
        WorldRound stored = roundMapper.selectById(round.getId());
        assertThat(stored.getStatus()).isEqualTo(expectedStatus);
        assertThat(stored.getErrorCode()).isEqualTo(expectedError);
        assertThat(stored.getCompletionTime()).isNotNull();
        assertThat(stored.getLeaseUntil()).isNull();
    }

    private WorldRound createRound(String requestId) {
        CharacterWorld world = new CharacterWorld();
        world.setOwnerUserId(800L); world.setName("world-" + requestId + System.nanoTime());
        world.setStatus(1); world.setCreateTime(LocalDateTime.now()); world.setUpdateTime(LocalDateTime.now());
        worldMapper.insert(world);
        WorldRound round = new WorldRound();
        round.setWorldId(world.getId()); round.setRequestId(requestId + System.nanoTime());
        round.setUserInput("hello"); round.setStatus(WorldRoundStatus.PENDING.name());
        round.setCreateTime(LocalDateTime.now()); round.setUpdateTime(LocalDateTime.now());
        roundMapper.insert(round);
        WorldEvent user = new WorldEvent();
        user.setRoundId(round.getId()); user.setSequenceNo(1); user.setEventType("USER_MESSAGE");
        user.setContent("hello"); user.setStatus(WorldEventStatus.COMPLETED.name());
        user.setCompletionTime(LocalDateTime.now()); user.setCreateTime(LocalDateTime.now());
        eventMapper.insert(user);
        return round;
    }

    private WorldEvent aiEvent(Long roundId, int sequence, Long participantId) {
        WorldEvent event = new WorldEvent();
        event.setRoundId(roundId); event.setSequenceNo(sequence); event.setParticipantId(participantId);
        event.setEventType("AI_MESSAGE"); event.setContent("duplicate"); event.setStatus("COMPLETED");
        event.setCompletionTime(LocalDateTime.now()); event.setCreateTime(LocalDateTime.now());
        return event;
    }

    private void expireLease(Long roundId) {
        WorldRound expired = roundMapper.selectById(roundId);
        expired.setLeaseUntil(LocalDateTime.now().minusSeconds(1));
        roundMapper.updateById(expired);
    }

    private WorldEvent findParticipantEvent(Long roundId, Long participantId) {
        return eventMapper.selectOne(new LambdaQueryWrapper<WorldEvent>()
                .eq(WorldEvent::getRoundId, roundId)
                .eq(WorldEvent::getParticipantId, participantId));
    }

    private java.util.List<WorldEvent> events(Long roundId) {
        return eventMapper.selectList(new LambdaQueryWrapper<WorldEvent>()
                .eq(WorldEvent::getRoundId, roundId).orderByAsc(WorldEvent::getSequenceNo));
    }
}
