package com.companion.ai.relationship;

import com.companion.ai.LlmClient;
import com.companion.ai.model.LlmResponse;
import com.companion.entity.CharacterRelationship;
import com.companion.entity.enums.RelationshipStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.companion.ai.prompt.ClasspathPromptTemplateLoader;
import com.companion.ai.prompt.PromptTemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LlmRelationshipEvaluatorTest {
    private LlmClient client;
    private LlmRelationshipEvaluator evaluator;

    @BeforeEach void setUp() {
        client = mock(LlmClient.class);
        PromptTemplateRenderer renderer = new PromptTemplateRenderer();
        evaluator = new LlmRelationshipEvaluator(client, new ObjectMapper(),
                new ClasspathPromptTemplateLoader(new DefaultResourceLoader(), renderer), renderer);
    }

    @Test void parsesValidBoundedChange() {
        respond("{\"changed\":true,\"stage\":\"TRUSTED\",\"summary\":\"Mutual trust formed\","
                + "\"interactionStyle\":\"Warm and candid\",\"recentChange\":\"User shared a fear\"}");
        RelationshipEvaluation result = evaluator.evaluate(current(), "I need to tell you something difficult", "I am listening");
        assertThat(result.changed()).isTrue();
        assertThat(result.stage()).isEqualTo(RelationshipStage.TRUSTED);
        assertThat(result.summary()).isEqualTo("Mutual trust formed");
    }

    @Test void changedFalseIsUnchanged() {
        respond("{\"changed\":false}");
        assertThat(evaluator.evaluate(current(), "How is the weather?", "Sunny").changed()).isFalse();
    }

    @Test void malformedEmptyProviderFailureAndInvalidStageAreSafe() {
        respond("not-json");
        assertThat(evaluator.evaluate(current(), "I had a difficult day", "Tell me more").changed()).isFalse();
        respond("");
        assertThat(evaluator.evaluate(current(), "I had a difficult day", "Tell me more").changed()).isFalse();
        respond("{\"changed\":true,\"stage\":\"SOULMATE\",\"summary\":\"x\",\"interactionStyle\":\"x\",\"recentChange\":\"x\"}");
        assertThat(evaluator.evaluate(current(), "I had a difficult day", "Tell me more").changed()).isFalse();
        when(client.complete(anyString(), anyString(), anyInt())).thenThrow(new RuntimeException("provider down"));
        assertThatCode(() -> evaluator.evaluate(current(), "I had a difficult day", "Tell me more")).doesNotThrowAnyException();
        assertThat(evaluator.evaluate(current(), "I had a difficult day", "Tell me more").changed()).isFalse();
    }

    @Test void ordinaryGreetingCannotPromoteEvenIfProviderWouldSaySo() {
        respond("{\"changed\":true,\"stage\":\"CLOSE\",\"summary\":\"x\",\"interactionStyle\":\"x\",\"recentChange\":\"x\"}");
        assertThat(evaluator.evaluate(current(), "你好！", "你好").changed()).isFalse();
        verifyNoInteractions(client);
    }

    @Test void overlongFieldsAreRejected() {
        respond("{\"changed\":true,\"stage\":\"FAMILIAR\",\"summary\":\"" + "x".repeat(1001)
                + "\",\"interactionStyle\":\"warm\",\"recentChange\":\"shared\"}");
        assertThat(evaluator.evaluate(current(), "I shared an important story", "Thank you").changed()).isFalse();
    }

    private void respond(String json) {
        reset(client);
        when(client.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new LlmResponse(json, "mock", "m", "stop", null, null));
    }

    private CharacterRelationship current() {
        CharacterRelationship value = new CharacterRelationship();
        value.setStage("NEW"); value.setSummary("New"); value.setInteractionStyle("Reserved"); value.setRecentChange("None");
        return value;
    }
}
