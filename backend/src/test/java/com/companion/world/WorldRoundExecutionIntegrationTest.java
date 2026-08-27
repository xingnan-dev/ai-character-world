package com.companion.world;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.character.snapshot.CharacterSnapshotJsonMapper;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.entity.CharacterWorld;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldParticipant;
import com.companion.entity.WorldRound;
import com.companion.entity.enums.WorldParticipantType;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.WorldEventMapper;
import com.companion.mapper.WorldParticipantMapper;
import com.companion.mapper.WorldRoundMapper;
import com.companion.service.WorldRoundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
class WorldRoundExecutionIntegrationTest {

    @Autowired private CharacterWorldMapper worldMapper;
    @Autowired private WorldParticipantMapper participantMapper;
    @Autowired private WorldRoundMapper roundMapper;
    @Autowired private WorldEventMapper eventMapper;
    @Autowired private CharacterSnapshotJsonMapper snapshotMapper;
    @Autowired private WorldRoundService roundService;

    @Test
    void executesSnapshotOnlyActorsSeriallyInDisplayOrderAndIsIdempotent() {
        CharacterWorld world = world();
        WorldParticipant later = participant(world.getId(), 202L, 20, validSnapshot(202L, "later"));
        WorldParticipant earlier = participant(world.getId(), 101L, 10, validSnapshot(101L, "earlier"));
        WorldParticipant userParticipant = participant(world.getId(), 303L, 5,
                validSnapshot(303L, "not-an-ai-participant"));
        userParticipant.setParticipantType(WorldParticipantType.USER.getCode());
        participantMapper.updateById(userParticipant);
        WorldParticipant deleted = participant(world.getId(), 404L, 6, validSnapshot(404L, "deleted"));
        participantMapper.deleteById(deleted.getId());
        long roundId = round(world, "serial").getId();

        roundService.execute(700L, world.getId(), roundId);
        List<WorldEvent> events = events(roundId);

        assertThat(roundMapper.selectById(roundId).getStatus()).isEqualTo("COMPLETED");
        assertThat(events).extracting(WorldEvent::getSequenceNo).containsExactly(1, 2, 3);
        assertThat(events.subList(1, 3)).extracting(WorldEvent::getParticipantId)
                .containsExactly(earlier.getId(), later.getId());
        assertThat(events.subList(1, 3)).allMatch(event -> "COMPLETED".equals(event.getStatus()));

        long version = roundMapper.selectById(roundId).getExecutionVersion();
        roundService.execute(700L, world.getId(), roundId);
        assertThat(events(roundId)).hasSize(3);
        assertThat(roundMapper.selectById(roundId).getExecutionVersion()).isEqualTo(version);
    }

    @Test
    void damagedSnapshotsProducePartialOrAllFailedAndDoNotStopLaterActors() {
        CharacterWorld partialWorld = world();
        WorldParticipant broken = participant(partialWorld.getId(), 301L, 1, "{broken");
        WorldParticipant valid = participant(partialWorld.getId(), 302L, 2, validSnapshot(302L, "valid"));
        long partialRound = round(partialWorld, "partial").getId();
        roundService.execute(700L, partialWorld.getId(), partialRound);
        assertThat(roundMapper.selectById(partialRound).getStatus()).isEqualTo("PARTIAL_FAILED");
        assertThat(events(partialRound).get(1).getParticipantId()).isEqualTo(broken.getId());
        assertThat(events(partialRound).get(1).getErrorCode()).isEqualTo("WORLD_SNAPSHOT_INVALID");
        assertThat(events(partialRound).get(2).getParticipantId()).isEqualTo(valid.getId());

        CharacterWorld failedWorld = world();
        participant(failedWorld.getId(), 401L, 1, "not-json");
        long failedRound = round(failedWorld, "failed").getId();
        roundService.execute(700L, failedWorld.getId(), failedRound);
        assertThat(roundMapper.selectById(failedRound).getStatus()).isEqualTo("FAILED");
        assertThat(roundMapper.selectById(failedRound).getErrorCode())
                .isEqualTo("WORLD_ALL_PARTICIPANTS_FAILED");
    }

    @Test
    void noAiFailsAndConcurrentExecuteProducesOneEventPerActor() throws Exception {
        CharacterWorld emptyWorld = world();
        long emptyRound = round(emptyWorld, "empty").getId();
        roundService.execute(700L, emptyWorld.getId(), emptyRound);
        assertThat(roundMapper.selectById(emptyRound).getErrorCode()).isEqualTo("WORLD_NO_AI_PARTICIPANT");

        CharacterWorld concurrentWorld = world();
        participant(concurrentWorld.getId(), 501L, 1, validSnapshot(501L, "only"));
        long roundId = round(concurrentWorld, "concurrent").getId();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    start.await(5, TimeUnit.SECONDS);
                    roundService.execute(700L, concurrentWorld.getId(), roundId);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(events(roundId)).hasSize(2);
        assertThat(roundMapper.selectById(roundId).getStatus()).isEqualTo("COMPLETED");
    }

    private CharacterWorld world() {
        CharacterWorld world = new CharacterWorld();
        world.setOwnerUserId(700L); world.setName("world-" + System.nanoTime());
        world.setBackground("background"); world.setRules("rules"); world.setStatus(1);
        world.setCreateTime(LocalDateTime.now()); world.setUpdateTime(LocalDateTime.now());
        worldMapper.insert(world);
        return world;
    }

    private WorldParticipant participant(Long worldId, Long sourceId, int order, String snapshot) {
        WorldParticipant participant = new WorldParticipant();
        participant.setWorldId(worldId); participant.setParticipantType(WorldParticipantType.AI.getCode());
        participant.setSourceCharacterId(sourceId); participant.setDisplayOrder(order);
        participant.setCharacterSnapshot(snapshot); participant.setCreateTime(LocalDateTime.now());
        participantMapper.insert(participant);
        return participant;
    }

    private String validSnapshot(Long sourceId, String name) {
        return snapshotMapper.write(new CharacterSnapshot(1, sourceId, "AI", name, null,
                "identity", "calm", "goal", null, "friend", "brief",
                CharacterSnapshot.Profile.empty(), "INITIAL", null, null, "#000"));
    }

    private WorldRound round(CharacterWorld world, String requestId) {
        WorldRoundCreateRequest request = new WorldRoundCreateRequest();
        request.setRequestId(requestId + System.nanoTime()); request.setUserInput("hello");
        roundService.create(700L, world.getId(), request);
        return roundMapper.selectOne(new LambdaQueryWrapper<WorldRound>()
                .eq(WorldRound::getWorldId, world.getId()).eq(WorldRound::getRequestId, request.getRequestId()));
    }

    private List<WorldEvent> events(Long roundId) {
        return eventMapper.selectList(new LambdaQueryWrapper<WorldEvent>()
                .eq(WorldEvent::getRoundId, roundId).orderByAsc(WorldEvent::getSequenceNo));
    }
}
