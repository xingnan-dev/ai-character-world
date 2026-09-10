package com.companion.service;

import com.companion.dto.request.WorldCreateRequest;
import com.companion.dto.request.WorldParticipantReplaceRequest;
import com.companion.dto.request.WorldUpdateRequest;
import com.companion.dto.response.WorldResponse;

import java.util.List;

public interface CharacterWorldService {
    WorldResponse create(Long userId, WorldCreateRequest request);
    List<WorldResponse> list(Long userId);
    WorldResponse get(Long userId, Long worldId);
    WorldResponse update(Long userId, Long worldId, WorldUpdateRequest request);
    void delete(Long userId, Long worldId);
    WorldResponse replaceParticipants(Long userId, Long worldId, WorldParticipantReplaceRequest request);
    WorldResponse setUserCharacter(Long userId, Long worldId, Long characterId);
}
