package com.companion.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentDecisionParserTest {

    private final AgentDecisionParser parser = new AgentDecisionParser(new ObjectMapper());

    @Test
    void parsesOnlyStrictToolCallAndFinalStructures() {
        assertThat(parser.parse(
                "{\"type\":\"TOOL_CALL\",\"decisionSummary\":\"look up\","
                        + "\"toolName\":\"get_world_context\",\"arguments\":{\"worldId\":1}}")
                .toolName()).isEqualTo("get_world_context");
        assertThat(parser.parse(
                "{\"type\":\"FINAL\",\"decisionSummary\":\"done\","
                        + "\"finalResult\":\"answer\"}")
                .finalResult()).isEqualTo("answer");
    }

    @Test
    void missingOrBlankDecisionSummaryIsRetryableContractViolation() {
        assertContractViolation(
                "{\"type\":\"FINAL\",\"finalResult\":\"answer\"}",
                "decisionSummary is required");
        assertContractViolation(
                "{\"type\":\"FINAL\",\"decisionSummary\":\"   \","
                        + "\"finalResult\":\"answer\"}",
                "decisionSummary is required");
    }

    @Test
    void invalidTypeAndUnknownFieldsAreRetryableContractViolations() {
        assertContractViolation(
                "{\"type\":\"WAIT\",\"decisionSummary\":\"wait\"}",
                "type must be TOOL_CALL or FINAL");
        assertContractViolation(
                "{\"type\":\"FINAL\",\"decisionSummary\":\"done\","
                        + "\"finalResult\":\"x\",\"secret\":1}",
                "Unknown fields are not allowed");
    }

    @Test
    void malformedJsonIsRetryableContractViolation() {
        assertThatThrownBy(() -> parser.parse("not json"))
                .isInstanceOfSatisfying(AgentExecutionException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("MALFORMED_DECISION");
                    assertThat(exception.isRetryable()).isTrue();
                });
    }

    @Test
    void requiredDecisionFieldsRemainStrict() {
        assertContractViolation(
                "{\"type\":\"TOOL_CALL\",\"decisionSummary\":\"lookup\","
                        + "\"arguments\":{\"worldId\":1}}",
                "TOOL_CALL requires a non-blank toolName");
        assertContractViolation(
                "{\"type\":\"FINAL\",\"decisionSummary\":\"done\"}",
                "FINAL requires a non-blank finalResult");
    }

    private void assertContractViolation(String json, String message) {
        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOfSatisfying(AgentExecutionException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("INVALID_DECISION");
                    assertThat(exception.isRetryable()).isTrue();
                    assertThat(exception.getMessage()).contains(message);
                });
    }
}
