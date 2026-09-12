package com.companion.service;

import com.companion.entity.CharacterRelationship;

import java.util.Optional;

public interface CharacterRelationshipService {
    CharacterRelationship getOrCreate(Long userId, Long characterId);
    Optional<CharacterRelationship> get(Long userId, Long characterId);
    CharacterRelationship evaluateAndUpdate(Long userId, Long characterId,
                                            String userMessage, String assistantResponse);
}
