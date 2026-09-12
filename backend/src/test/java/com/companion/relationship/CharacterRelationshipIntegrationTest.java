package com.companion.relationship;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.relationship.LlmRelationshipEvaluator;
import com.companion.ai.relationship.RelationshipEvaluation;
import com.companion.common.exception.BusinessException;
import com.companion.entity.AiCharacter;
import com.companion.entity.CharacterRelationship;
import com.companion.entity.enums.CharacterType;
import com.companion.entity.enums.RelationshipStage;
import com.companion.mapper.CharacterMapper;
import com.companion.mapper.CharacterRelationshipMapper;
import com.companion.service.CharacterRelationshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
class CharacterRelationshipIntegrationTest {
    @Autowired private CharacterRelationshipService service;
    @Autowired private CharacterRelationshipMapper relationships;
    @Autowired private CharacterMapper characters;
    @MockBean private LlmRelationshipEvaluator evaluator;

    private Long characterA;
    private Long characterB;

    @BeforeEach void setUp() {
        characterA = character(101L, CharacterType.AI);
        characterB = character(101L, CharacterType.AI);
        when(evaluator.evaluate(any(), anyString(), anyString())).thenReturn(RelationshipEvaluation.unchanged());
    }

    @Test void getOrCreateIsUniqueAndCharactersAreIsolated() {
        CharacterRelationship first = service.getOrCreate(101L, characterA);
        CharacterRelationship same = service.getOrCreate(101L, characterA);
        CharacterRelationship other = service.getOrCreate(101L, characterB);
        assertThat(same.getId()).isEqualTo(first.getId());
        assertThat(other.getId()).isNotEqualTo(first.getId());
        assertThat(relationships.selectCount(new QueryWrapper<CharacterRelationship>().eq("user_id", 101L))).isEqualTo(2);
        assertThat(first.getStage()).isEqualTo("NEW");
    }

    @Test void changedFalseDoesNotUpdateAndValidChangeOnlyUpdatesA() {
        CharacterRelationship a = service.getOrCreate(101L, characterA);
        CharacterRelationship b = service.getOrCreate(101L, characterB);
        LocalDateTime originalUpdate = relationships.selectById(a.getId()).getUpdateTime();
        service.evaluateAndUpdate(101L, characterA, "weather", "sunny");
        assertThat(relationships.selectById(a.getId()).getUpdateTime()).isEqualTo(originalUpdate);

        when(evaluator.evaluate(any(), eq("I trust you"), anyString())).thenReturn(new RelationshipEvaluation(
                true, RelationshipStage.TRUSTED, "Trust established", "Candid and warm", "User disclosed a concern"));
        service.evaluateAndUpdate(101L, characterA, "I trust you", "Thank you");
        assertThat(relationships.selectById(a.getId()).getStage()).isEqualTo("TRUSTED");
        assertThat(relationships.selectById(b.getId()).getStage()).isEqualTo("NEW");
    }

    @Test void userCharacterAndOtherUsersCharacterAreRejected() {
        Long userCharacter = character(101L, CharacterType.USER);
        Long otherUsersCharacter = character(202L, CharacterType.AI);
        assertThatThrownBy(() -> service.getOrCreate(101L, userCharacter)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getOrCreate(101L, otherUsersCharacter)).isInstanceOf(BusinessException.class);
        assertThat(relationships.selectCount(null)).isZero();
    }

    private Long character(Long userId, CharacterType type) {
        AiCharacter value = new AiCharacter();
        value.setUserId(userId); value.setCharacterType(type.getCode()); value.setName("Character");
        value.setGenerateType(0); value.setVisualType(0); value.setStatus(1); value.setDeleted(0);
        value.setCreateTime(LocalDateTime.now()); value.setUpdateTime(LocalDateTime.now());
        characters.insert(value);
        return value.getId();
    }
}
