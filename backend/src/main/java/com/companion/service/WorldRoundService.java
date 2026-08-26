package com.companion.service;

import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.dto.response.WorldEventResponse;
import com.companion.dto.response.WorldRoundResponse;

import java.util.List;

public interface WorldRoundService {
    WorldRoundResponse create(Long userId, Long worldId, WorldRoundCreateRequest request);
    WorldRoundResponse get(Long userId, Long worldId, Long roundId);
    List<WorldEventResponse> getEvents(Long userId, Long worldId, Long roundId);
    boolean tryStart(Long userId, Long worldId, Long roundId);
}
