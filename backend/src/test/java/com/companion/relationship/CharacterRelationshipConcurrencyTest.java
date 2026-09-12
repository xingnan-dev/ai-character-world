package com.companion.relationship;

import com.companion.ai.relationship.LlmRelationshipEvaluator;
import com.companion.ai.relationship.RelationshipEvaluation;
import com.companion.dto.response.CharacterResponse;
import com.companion.entity.CharacterRelationship;
import com.companion.entity.enums.RelationshipStage;
import com.companion.mapper.CharacterRelationshipMapper;
import com.companion.service.CharacterService;
import com.companion.service.impl.CharacterRelationshipServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CharacterRelationshipConcurrencyTest {
    @Test void staleUpdateIsRejectedAndOneBoundedReevaluationCanWin() {
        CharacterRelationshipMapper mapper = mock(CharacterRelationshipMapper.class);
        CharacterService characters = mock(CharacterService.class);
        LlmRelationshipEvaluator evaluator = mock(LlmRelationshipEvaluator.class);
        when(characters.get(10L, 71L)).thenReturn(aiCharacter(71L));
        when(mapper.selectOne(any())).thenReturn(relationship(0, "old"), relationship(1, "newer"));
        when(evaluator.evaluate(any(), eq("meaningful"), anyString()))
                .thenReturn(change("first"), change("retry"));
        when(mapper.updateIfVersionMatches(any(), eq(0))).thenReturn(0);
        when(mapper.updateIfVersionMatches(any(), eq(1))).thenReturn(1);

        CharacterRelationship result = service(mapper, characters, evaluator)
                .evaluateAndUpdate(10L, 71L, "meaningful", "response");

        assertThat(result.getSummary()).isEqualTo("retry");
        verify(mapper).updateIfVersionMatches(any(), eq(0));
        verify(mapper).updateIfVersionMatches(any(), eq(1));
        verify(evaluator, times(2)).evaluate(any(), eq("meaningful"), eq("response"));
    }

    @Test void conflictAfterSingleRetrySafelyKeepsLatestAndDoesNotLoop() {
        CharacterRelationshipMapper mapper = mock(CharacterRelationshipMapper.class);
        CharacterService characters = mock(CharacterService.class);
        LlmRelationshipEvaluator evaluator = mock(LlmRelationshipEvaluator.class);
        when(characters.get(10L, 71L)).thenReturn(aiCharacter(71L));
        when(mapper.selectOne(any())).thenReturn(relationship(0, "old"), relationship(1, "newer"));
        when(evaluator.evaluate(any(), anyString(), anyString())).thenReturn(change("attempt"));
        when(mapper.updateIfVersionMatches(any(), anyInt())).thenReturn(0);

        CharacterRelationship result = service(mapper, characters, evaluator)
                .evaluateAndUpdate(10L, 71L, "meaningful", "response");

        assertThat(result.getSummary()).isEqualTo("newer");
        verify(mapper, times(2)).updateIfVersionMatches(any(), anyInt());
        verify(evaluator, times(2)).evaluate(any(), anyString(), anyString());
    }

    @Test void unchangedEvaluatorDoesNotAttemptCas() {
        CharacterRelationshipMapper mapper = mock(CharacterRelationshipMapper.class);
        CharacterService characters = mock(CharacterService.class);
        LlmRelationshipEvaluator evaluator = mock(LlmRelationshipEvaluator.class);
        when(characters.get(10L, 71L)).thenReturn(aiCharacter(71L));
        when(mapper.selectOne(any())).thenReturn(relationship(0, "old"));
        when(evaluator.evaluate(any(), anyString(), anyString())).thenReturn(RelationshipEvaluation.unchanged());

        CharacterRelationship result = service(mapper, characters, evaluator)
                .evaluateAndUpdate(10L, 71L, "ordinary", "response");

        assertThat(result.getSummary()).isEqualTo("old");
        verify(mapper, never()).updateIfVersionMatches(any(), anyInt());
    }

    private CharacterRelationshipServiceImpl service(CharacterRelationshipMapper mapper,
                                                       CharacterService characters,
                                                       LlmRelationshipEvaluator evaluator) {
        return new CharacterRelationshipServiceImpl(mapper, characters, evaluator);
    }

    private RelationshipEvaluation change(String summary) {
        return new RelationshipEvaluation(true, RelationshipStage.TRUSTED, summary, "warm", "change");
    }

    private CharacterRelationship relationship(int version, String summary) {
        CharacterRelationship value = new CharacterRelationship();
        value.setId(1L); value.setCharacterId(71L); value.setVersion(version);
        value.setStage("NEW"); value.setSummary(summary);
        value.setInteractionStyle("reserved"); value.setRecentChange("none");
        return value;
    }

    private CharacterResponse aiCharacter(Long id) {
        CharacterResponse value = new CharacterResponse();
        value.setId(id); value.setCharacterType("AI");
        return value;
    }
}
