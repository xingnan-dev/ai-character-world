package com.companion.agent;

import com.companion.agent.model.AgentDecision;
import com.companion.ai.json.LlmJsonObjectExtractor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.springframework.stereotype.Component;

@Component
public class AgentDecisionParser {

    private final ObjectMapper mapper;

    public AgentDecisionParser(ObjectMapper base) {
        mapper = base.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    public AgentDecision parse(String raw) {
        try {
            AgentDecision decision = mapper.readValue(
                    LlmJsonObjectExtractor.extract(raw), AgentDecision.class);
            validate(decision);
            return decision;
        } catch (AgentExecutionException exception) {
            throw exception;
        } catch (UnrecognizedPropertyException exception) {
            throw contractViolation("Unknown fields are not allowed", exception);
        } catch (Exception exception) {
            throw new AgentExecutionException(
                    "MALFORMED_DECISION",
                    "Response must be one valid JSON object matching an allowed decision structure",
                    true,
                    exception);
        }
    }

    private void validate(AgentDecision decision) {
        if (decision == null || decision.type() == null || decision.type().isBlank()) {
            invalid("type is required and must be TOOL_CALL or FINAL");
        }

        String summary = decision.decisionSummary() == null
                ? ""
                : decision.decisionSummary().trim();
        if (summary.isEmpty() || summary.length() > 500) {
            invalid("decisionSummary is required and must contain 1-500 characters");
        }

        if ("TOOL_CALL".equals(decision.type())) {
            if (decision.toolName() == null || decision.toolName().isBlank()) {
                invalid("TOOL_CALL requires a non-blank toolName");
            }
            if (decision.arguments() == null || !decision.arguments().isObject()) {
                invalid("TOOL_CALL requires an arguments object");
            }
            if (decision.finalResult() != null) {
                invalid("TOOL_CALL must not include finalResult");
            }
            return;
        }

        if ("FINAL".equals(decision.type())) {
            if (decision.finalResult() == null
                    || decision.finalResult().isBlank()
                    || decision.finalResult().length() > 12000) {
                invalid("FINAL requires a non-blank finalResult of at most 12000 characters");
            }
            if (decision.toolName() != null || decision.arguments() != null) {
                invalid("FINAL must not include toolName or arguments");
            }
            return;
        }

        invalid("type must be TOOL_CALL or FINAL");
    }

    private void invalid(String message) {
        throw contractViolation(message, null);
    }

    private AgentExecutionException contractViolation(String message, Throwable cause) {
        return new AgentExecutionException("INVALID_DECISION", message, true, cause);
    }
}
