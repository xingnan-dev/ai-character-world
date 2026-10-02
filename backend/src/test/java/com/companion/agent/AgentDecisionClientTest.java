package com.companion.agent;

import com.companion.agent.tool.AgentTool;
import com.companion.agent.tool.AgentToolRegistry;
import com.companion.ai.LlmClient;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.entity.AgentRun;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentDecisionClientTest {

    private static final String VALID_FINAL =
            "{\"type\":\"FINAL\",\"decisionSummary\":\"done\",\"finalResult\":\"ok\"}";
    private static final String VALID_TOOL_CALL =
            "{\"type\":\"TOOL_CALL\",\"decisionSummary\":\"need context\","
                    + "\"toolName\":\"get_character_context\",\"arguments\":{\"characterId\":3}}";

    @Test
    void missingDecisionSummaryGetsOneCorrectionRetryWithSanitizedReason() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenReturn(response("{\"type\":\"FINAL\",\"finalResult\":\"ok\"}"),
                        response(VALID_FINAL));

        assertThat(fixture.client.decide(new AgentRun(), List.of()).finalResult()).isEqualTo("ok");

        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
        ArgumentCaptor<String> correction = ArgumentCaptor.forClass(String.class);
        verify(fixture.prompts, times(2)).compose(any(), anyList(), correction.capture());
        assertThat(correction.getAllValues()).containsExactly(
                null,
                "decisionSummary is required and must contain 1-500 characters");
    }

    @Test
    void blankDecisionSummaryGetsOneCorrectionRetry() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenReturn(response("{\"type\":\"FINAL\",\"decisionSummary\":\"  \",\"finalResult\":\"ok\"}"),
                        response(VALID_FINAL));

        assertThat(fixture.client.decide(new AgentRun(), List.of()).finalResult()).isEqualTo("ok");
        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void twoMissingDecisionSummariesEndAsNonRetryableInvalidDecision() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenReturn(response("{\"type\":\"FINAL\",\"finalResult\":\"one\"}"),
                        response("{\"type\":\"FINAL\",\"finalResult\":\"two\"}"));

        assertThatThrownBy(() -> fixture.client.decide(new AgentRun(), List.of()))
                .isInstanceOfSatisfying(AgentExecutionException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("INVALID_DECISION");
                    assertThat(exception.isRetryable()).isFalse();
                    assertThat(exception.getRetryCount()).isEqualTo(1);
                });
        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void malformedJsonGetsOneCorrectionRetry() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenReturn(response("not-json"), response(VALID_FINAL));

        assertThat(fixture.client.decide(new AgentRun(), List.of()).finalResult()).isEqualTo("ok");
        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void unknownFieldGetsOneCorrectionRetry() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenReturn(response("{\"type\":\"FINAL\",\"decisionSummary\":\"done\","
                                + "\"finalResult\":\"ok\",\"analysis\":\"forbidden\"}"),
                        response(VALID_FINAL));

        assertThat(fixture.client.decide(new AgentRun(), List.of()).finalResult()).isEqualTo("ok");
        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void unknownToolGetsOneCorrectionRetry() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenReturn(response("{\"type\":\"TOOL_CALL\",\"decisionSummary\":\"need data\","
                                + "\"toolName\":\"delete_everything\",\"arguments\":{}}"),
                        response(VALID_TOOL_CALL));

        assertThat(fixture.client.decide(new AgentRun(), List.of()).toolName())
                .isEqualTo("get_character_context");
        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void invalidToolArgumentsGetOneCorrectionRetry() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenReturn(response("{\"type\":\"TOOL_CALL\",\"decisionSummary\":\"need data\","
                                + "\"toolName\":\"get_character_context\",\"arguments\":{\"characterId\":0}}"),
                        response(VALID_TOOL_CALL));

        assertThat(fixture.client.decide(new AgentRun(), List.of()).arguments()
                .path("characterId").asLong()).isEqualTo(3);
        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void transientProviderFailureThenValidResponseUsesExactlyTwoCalls() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenThrow(transientFailure())
                .thenReturn(response(VALID_FINAL));

        assertThat(fixture.client.decide(new AgentRun(), List.of()).finalResult()).isEqualTo("ok");
        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void transientProviderFailureThenSchemaInvalidHasNoThirdCall() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenThrow(transientFailure())
                .thenReturn(response("{\"type\":\"FINAL\",\"finalResult\":\"bad\"}"));

        assertThatThrownBy(() -> fixture.client.decide(new AgentRun(), List.of()))
                .isInstanceOfSatisfying(AgentExecutionException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_DECISION"));
        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void schemaInvalidThenTransientProviderFailureHasNoThirdCall() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenReturn(response("{\"type\":\"FINAL\",\"finalResult\":\"bad\"}"))
                .thenThrow(transientFailure());

        assertThatThrownBy(() -> fixture.client.decide(new AgentRun(), List.of()))
                .isInstanceOf(LlmProviderException.class);
        verify(fixture.llm, times(2)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void validFirstResponseUsesExactlyOneCall() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenReturn(response(VALID_FINAL));

        assertThat(fixture.client.decide(new AgentRun(), List.of()).finalResult()).isEqualTo("ok");
        verify(fixture.llm, times(1)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    @Test
    void nonRetryableProviderFailureIsAttemptedOnce() {
        Fixture fixture = fixture();
        when(fixture.llm.complete(any(ComposedChatPrompt.class), eq(2048)))
                .thenThrow(new LlmProviderException(
                        "test", LlmErrorType.AUTHENTICATION, 401, false, "denied"));

        assertThatThrownBy(() -> fixture.client.decide(new AgentRun(), List.of()))
                .isInstanceOf(LlmProviderException.class);
        verify(fixture.llm, times(1)).complete(any(ComposedChatPrompt.class), eq(2048));
    }

    private Fixture fixture() {
        LlmClient llm = mock(LlmClient.class);
        AgentPromptComposer prompts = mock(AgentPromptComposer.class);
        when(prompts.compose(any(), anyList(), nullable(String.class)))
                .thenReturn(new ComposedChatPrompt(List.of(), java.util.Map.of()));
        AgentToolRegistry tools = new AgentToolRegistry(List.of(characterTool()));
        AgentDecisionClient client = new AgentDecisionClient(
                llm, prompts, new AgentDecisionParser(new ObjectMapper()), tools);
        return new Fixture(llm, prompts, client);
    }

    private AgentTool characterTool() {
        return new AgentTool() {
            @Override
            public String name() {
                return "get_character_context";
            }

            @Override
            public String description() {
                return "Read character context.";
            }

            @Override
            public String inputSchema() {
                return "{characterId: positive integer}";
            }

            @Override
            public void validate(JsonNode arguments) {
                Set<String> fields = new java.util.HashSet<>();
                arguments.fieldNames().forEachRemaining(fields::add);
                if (!arguments.isObject()
                        || !fields.equals(Set.of("characterId"))
                        || !arguments.path("characterId").canConvertToLong()
                        || arguments.path("characterId").asLong() <= 0) {
                    throw new AgentExecutionException(
                            "INVALID_TOOL_ARGUMENTS", "Invalid tool arguments", false);
                }
            }

            @Override
            public String execute(Long userId, JsonNode arguments) {
                throw new AssertionError("Decision validation must not execute Tools");
            }
        };
    }

    private LlmProviderException transientFailure() {
        return new LlmProviderException(
                "test", LlmErrorType.TIMEOUT, 504, true, "timeout");
    }

    private LlmResponse response(String content) {
        return new LlmResponse(content, "mock", "mock", "stop", null, null);
    }

    private record Fixture(
            LlmClient llm,
            AgentPromptComposer prompts,
            AgentDecisionClient client
    ) {
    }
}
