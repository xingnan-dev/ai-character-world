package com.companion.agent;

import com.companion.agent.model.AgentDecision;
import com.companion.agent.tool.AgentToolRegistry;
import com.companion.ai.LlmClient;
import com.companion.ai.exception.LlmProviderException;
import com.companion.entity.AgentRun;
import com.companion.entity.AgentStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class AgentDecisionClient {

    static final int MAX_DECISION_ATTEMPTS = 2;
    private static final Set<String> MODEL_CONTRACT_CODES = Set.of(
            "MALFORMED_DECISION",
            "INVALID_DECISION",
            "UNKNOWN_TOOL",
            "INVALID_TOOL_ARGUMENTS"
    );

    private final LlmClient llm;
    private final AgentPromptComposer prompts;
    private final AgentDecisionParser parser;
    private final AgentToolRegistry tools;

    public AgentDecisionClient(LlmClient llm, AgentPromptComposer prompts,
                               AgentDecisionParser parser, AgentToolRegistry tools) {
        this.llm = llm;
        this.prompts = prompts;
        this.parser = parser;
        this.tools = tools;
    }

    public AgentDecision decide(AgentRun run, List<AgentStep> steps) {
        String correctionReason = null;
        RuntimeException lastFailure = null;

        for (int attempt = 0; attempt < MAX_DECISION_ATTEMPTS; attempt++) {
            try {
                AgentDecision decision = parser.parse(
                        llm.complete(prompts.compose(run, steps, correctionReason), 2048).content());
                validateToolContract(decision);
                return decision;
            } catch (AgentExecutionException exception) {
                if (!isModelContractViolation(exception)) {
                    throw exception;
                }
                lastFailure = exception;
                if (attempt == MAX_DECISION_ATTEMPTS - 1) {
                    throw exhaustedContract(exception);
                }
                correctionReason = sanitizedReason(exception);
            } catch (LlmProviderException exception) {
                lastFailure = exception;
                if (!exception.isRetryable() || attempt == MAX_DECISION_ATTEMPTS - 1) {
                    throw exception;
                }
                // No model output exists to correct. Retry the same decision prompt while
                // consuming the shared global decision-attempt budget.
                correctionReason = null;
            }
        }

        throw lastFailure;
    }

    private void validateToolContract(AgentDecision decision) {
        if ("TOOL_CALL".equals(decision.type())) {
            tools.require(decision.toolName()).validate(decision.arguments());
        }
    }

    private boolean isModelContractViolation(AgentExecutionException exception) {
        return MODEL_CONTRACT_CODES.contains(exception.getCode());
    }

    private AgentExecutionException exhaustedContract(AgentExecutionException exception) {
        return new AgentExecutionException(
                "INVALID_DECISION",
                sanitizedReason(exception),
                false,
                MAX_DECISION_ATTEMPTS - 1,
                exception);
    }

    private String sanitizedReason(AgentExecutionException exception) {
        return switch (exception.getCode()) {
            case "MALFORMED_DECISION" ->
                    "Response must be one valid JSON object matching an allowed decision structure";
            case "UNKNOWN_TOOL" ->
                    "toolName must be one of the allowed Tool names";
            case "INVALID_TOOL_ARGUMENTS" ->
                    "arguments must exactly match the selected Tool input schema";
            case "INVALID_DECISION" -> sanitizeControlledMessage(exception.getMessage());
            default -> "Response does not satisfy the Agent Decision JSON contract";
        };
    }

    private String sanitizeControlledMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Response does not satisfy the Agent Decision JSON contract";
        }
        String sanitized = message.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("[^\\p{L}\\p{N} _.,:-]", "")
                .trim();
        if (sanitized.isBlank()) {
            return "Response does not satisfy the Agent Decision JSON contract";
        }
        return sanitized.substring(0, Math.min(200, sanitized.length()));
    }
}
