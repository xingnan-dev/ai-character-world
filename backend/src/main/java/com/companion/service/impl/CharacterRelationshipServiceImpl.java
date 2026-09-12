package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.relationship.LlmRelationshipEvaluator;
import com.companion.ai.relationship.RelationshipEvaluation;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.response.CharacterResponse;
import com.companion.entity.CharacterRelationship;
import com.companion.entity.enums.RelationshipStage;
import com.companion.mapper.CharacterRelationshipMapper;
import com.companion.service.CharacterRelationshipService;
import com.companion.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CharacterRelationshipServiceImpl implements CharacterRelationshipService {
    private final CharacterRelationshipMapper relationshipMapper;
    private final CharacterService characterService;
    private final LlmRelationshipEvaluator evaluator;

    @Override
    @Transactional
    public CharacterRelationship getOrCreate(Long userId, Long characterId) {
        validateOwnedAiCharacter(userId, characterId);
        CharacterRelationship existing = find(userId, characterId);
        if (existing != null) return existing;

        LocalDateTime now = LocalDateTime.now();
        CharacterRelationship relationship = new CharacterRelationship();
        relationship.setUserId(userId);
        relationship.setCharacterId(characterId);
        relationship.setStage(RelationshipStage.NEW.name());
        relationship.setSummary("No meaningful relationship history yet.");
        relationship.setInteractionStyle("Polite, friendly, and appropriately reserved.");
        relationship.setRecentChange("Relationship initialized.");
        relationship.setDeleted(0);
        relationship.setVersion(0);
        relationship.setCreateTime(now);
        relationship.setUpdateTime(now);
        try {
            relationshipMapper.insert(relationship);
            return relationship;
        } catch (DuplicateKeyException concurrentInsert) {
            CharacterRelationship concurrentlyCreated = find(userId, characterId);
            if (concurrentlyCreated != null) return concurrentlyCreated;
            throw concurrentInsert;
        }
    }

    @Override
    public Optional<CharacterRelationship> get(Long userId, Long characterId) {
        validateOwnedAiCharacter(userId, characterId);
        return Optional.ofNullable(find(userId, characterId));
    }

    @Override
    public CharacterRelationship evaluateAndUpdate(Long userId, Long characterId,
                                                   String userMessage, String assistantResponse) {
        CharacterRelationship current = getOrCreate(userId, characterId);
        for (int attempt = 0; attempt < 2; attempt++) {
            RelationshipEvaluation evaluation = evaluator.evaluate(current, userMessage, assistantResponse);
            if (!evaluation.changed()) return current;

            Integer expectedVersion = current.getVersion() == null ? 0 : current.getVersion();
            CharacterRelationship candidate = copyOf(current);
            candidate.setStage(evaluation.stage().name());
            candidate.setSummary(evaluation.summary());
            candidate.setInteractionStyle(evaluation.interactionStyle());
            candidate.setRecentChange(evaluation.recentChange());
            candidate.setUpdateTime(LocalDateTime.now());
            if (relationshipMapper.updateIfVersionMatches(candidate, expectedVersion) == 1) {
                candidate.setVersion(expectedVersion + 1);
                return candidate;
            }

            // CAS conflict: refresh and retry once. No DB lock spans evaluation.
            current = getOrCreate(userId, characterId);
        }
        return current;
    }

    private CharacterRelationship copyOf(CharacterRelationship source) {
        CharacterRelationship copy = new CharacterRelationship();
        copy.setId(source.getId());
        copy.setUserId(source.getUserId());
        copy.setCharacterId(source.getCharacterId());
        copy.setStage(source.getStage());
        copy.setSummary(source.getSummary());
        copy.setInteractionStyle(source.getInteractionStyle());
        copy.setRecentChange(source.getRecentChange());
        copy.setDeleted(source.getDeleted());
        copy.setVersion(source.getVersion());
        copy.setCreateTime(source.getCreateTime());
        copy.setUpdateTime(source.getUpdateTime());
        return copy;
    }

    private CharacterRelationship find(Long userId, Long characterId) {
        return relationshipMapper.selectOne(new QueryWrapper<CharacterRelationship>()
                .eq("user_id", userId)
                .eq("character_id", characterId));
    }

    private void validateOwnedAiCharacter(Long userId, Long characterId) {
        if (userId == null || characterId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        CharacterResponse character = characterService.get(userId, characterId);
        if (!"AI".equals(character.getCharacterType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Relationship只能绑定AI Character");
        }
    }
}
