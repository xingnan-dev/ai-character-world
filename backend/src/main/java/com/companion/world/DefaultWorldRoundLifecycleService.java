package com.companion.world;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldRound;
import com.companion.entity.enums.WorldEventStatus;
import com.companion.entity.enums.WorldEventType;
import com.companion.entity.enums.WorldRoundStatus;
import com.companion.mapper.WorldEventMapper;
import com.companion.mapper.WorldRoundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultWorldRoundLifecycleService implements WorldRoundLifecycleService {

    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);

    private final WorldRoundMapper roundMapper;
    private final WorldEventMapper eventMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorldExecutionClaim claim(Long worldId, Long roundId) {
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<WorldRound> update = new UpdateWrapper<>();
        update.eq("id", roundId).eq("world_id", worldId)
                .and(state -> state.eq("status", WorldRoundStatus.PENDING.name())
                        .or(expired -> expired.eq("status", WorldRoundStatus.RUNNING.name())
                                .and(lease -> lease.isNull("lease_until").or()
                                        .le("lease_until", now))))
                .set("status", WorldRoundStatus.RUNNING.name())
                .setSql("started_time = COALESCE(started_time, CURRENT_TIMESTAMP)")
                .set("lease_until", now.plus(LEASE_DURATION))
                .set("update_time", now)
                .setSql("execution_version = execution_version + 1");
        if (roundMapper.update(null, update) != 1) {
            return WorldExecutionClaim.notAcquired();
        }
        WorldRound claimed = roundMapper.selectById(roundId);
        return new WorldExecutionClaim(true, claimed.getExecutionVersion());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean renew(Long roundId, long executionVersion) {
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<WorldRound> update = new UpdateWrapper<>();
        update.eq("id", roundId)
                .eq("status", WorldRoundStatus.RUNNING.name())
                .eq("execution_version", executionVersion)
                .set("lease_until", now.plus(LEASE_DURATION))
                .set("update_time", now);
        return roundMapper.update(null, update) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveEvent(Long worldId, Long roundId, long executionVersion, int sequenceNo,
                             Long participantId, String content, String status, String errorCode) {
        LocalDateTime now = LocalDateTime.now();
        try {
            int inserted = eventMapper.insertIfExecutionOwned(
                    worldId, roundId, executionVersion, sequenceNo, participantId,
                    WorldEventType.AI_MESSAGE.name(), content == null ? "" : content,
                    status, errorCode, now, now);
            return inserted == 1;
        } catch (DuplicateKeyException duplicate) {
            WorldEvent concurrent = findExactEvent(roundId, participantId, sequenceNo);
            if (concurrent != null) {
                return true;
            }
            throw duplicate;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean finish(Long roundId, long executionVersion, int expectedParticipants) {
        List<WorldEvent> aiEvents = eventMapper.selectList(new LambdaQueryWrapper<WorldEvent>()
                .eq(WorldEvent::getRoundId, roundId)
                .isNotNull(WorldEvent::getParticipantId));
        if (aiEvents.size() != expectedParticipants) {
            return false;
        }
        long succeeded = aiEvents.stream()
                .filter(event -> WorldEventStatus.COMPLETED.name().equals(event.getStatus()))
                .count();
        String status;
        String errorCode;
        if (expectedParticipants == 0 || succeeded == 0) {
            status = WorldRoundStatus.FAILED.name();
            errorCode = expectedParticipants == 0 ? "WORLD_NO_AI_PARTICIPANT" : "WORLD_ALL_PARTICIPANTS_FAILED";
        } else if (succeeded == expectedParticipants) {
            status = WorldRoundStatus.COMPLETED.name();
            errorCode = null;
        } else {
            status = WorldRoundStatus.PARTIAL_FAILED.name();
            errorCode = "WORLD_PARTICIPANT_FAILURE";
        }

        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<WorldRound> update = new UpdateWrapper<>();
        update.eq("id", roundId)
                .eq("status", WorldRoundStatus.RUNNING.name())
                .eq("execution_version", executionVersion)
                .set("status", status)
                .set("error_code", errorCode)
                .set("completion_time", now)
                .set("lease_until", null)
                .set("update_time", now);
        return roundMapper.update(null, update) == 1;
    }

    @Override
    public List<WorldEvent> events(Long roundId) {
        return eventMapper.selectList(new LambdaQueryWrapper<WorldEvent>()
                .eq(WorldEvent::getRoundId, roundId)
                .orderByAsc(WorldEvent::getSequenceNo)
                .orderByAsc(WorldEvent::getId));
    }

    private WorldEvent findExactEvent(Long roundId, Long participantId, int sequenceNo) {
        return eventMapper.selectOne(new LambdaQueryWrapper<WorldEvent>()
                .eq(WorldEvent::getRoundId, roundId)
                .eq(WorldEvent::getParticipantId, participantId)
                .eq(WorldEvent::getSequenceNo, sequenceNo));
    }
}
