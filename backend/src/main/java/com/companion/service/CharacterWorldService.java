package com.companion.service;

import com.companion.dto.request.WorldCreateRequest;
import com.companion.dto.response.WorldResponse;

public interface CharacterWorldService {
    WorldResponse create(Long userId, WorldCreateRequest request);
    WorldResponse get(Long userId, Long worldId);
}
