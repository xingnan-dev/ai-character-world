package com.companion.service;

import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.dto.response.WorldEventResponse;
import com.companion.dto.response.WorldRoundResponse;
import com.companion.dto.response.WorldTimelinePageResponse;

import java.util.List;

public interface WorldRoundService {
    WorldRoundResponse create(Long userId, Long worldId, WorldRoundCreateRequest request);
    WorldRoundResponse get(Long userId, Long worldId, Long roundId);
    WorldRoundResponse getActive(Long userId, Long worldId);
    WorldTimelinePageResponse getTimeline(Long userId, Long worldId, Long beforeRoundId, Integer limit);
    List<WorldEventResponse> getEvents(Long userId, Long worldId, Long roundId);
    WorldRoundResponse execute(Long userId, Long worldId, Long roundId);
    boolean tryStart(Long userId, Long worldId, Long roundId);
}
