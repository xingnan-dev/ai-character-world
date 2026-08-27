package com.companion.world;

import com.companion.entity.WorldEvent;

import java.util.List;

public interface WorldRoundLifecycleService {
    WorldExecutionClaim claim(Long worldId, Long roundId);
    boolean renew(Long roundId, long executionVersion);
    boolean saveEvent(Long worldId, Long roundId, long executionVersion, int sequenceNo,
                      Long participantId, String content, String status, String errorCode);
    boolean finish(Long roundId, long executionVersion, int expectedParticipants);
    List<WorldEvent> events(Long roundId);
}
