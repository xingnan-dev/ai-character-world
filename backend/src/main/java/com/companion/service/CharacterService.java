package com.companion.service;

import com.companion.dto.request.CharacterCreateRequest;
import com.companion.dto.request.CharacterUpdateRequest;
import com.companion.dto.response.CharacterResponse;

import java.util.List;

public interface CharacterService {
    CharacterResponse create(Long userId, CharacterCreateRequest request);
    List<CharacterResponse> list(Long userId, String type);
    CharacterResponse get(Long userId, Long characterId);
    CharacterResponse update(Long userId, Long characterId, CharacterUpdateRequest request);
    void delete(Long userId, Long characterId);
}
