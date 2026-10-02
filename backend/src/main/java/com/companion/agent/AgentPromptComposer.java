package com.companion.agent;

import com.companion.agent.tool.AgentToolRegistry;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRole;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.entity.AgentRun;
import com.companion.entity.AgentStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AgentPromptComposer {

    private final AgentToolRegistry registry;

    public AgentPromptComposer(AgentToolRegistry registry) {
        this.registry = registry;
    }

    public ComposedChatPrompt compose(AgentRun run, List<AgentStep> steps,
                                      String correctionReason) {
        String tools = registry.all().stream()
                .map(tool -> "- " + tool.name() + ": " + tool.description()
                        + " input " + tool.inputSchema())
                .collect(Collectors.joining("\n"));
        String history = steps.stream()
                .filter(step -> "COMPLETED".equals(step.getStatus()))
                .map(step -> "Step " + step.getStepNumber()
                        + " decision=" + step.getDecisionType()
                        + " summary=" + safe(step.getDecisionSummary())
                        + " tool=" + safe(step.getToolName())
                        + " arguments=" + safe(step.getToolArguments())
                        + " result=" + limit(step.getToolResult(), 8000))
                .collect(Collectors.joining("\n"));

        String system = """
                You are a bounded goal agent. Choose exactly one next action.

                Available read-only tools:
                %s

                AGENT DECISION JSON CONTRACT
                Return JSON only. Return exactly one JSON object.
                Do not use markdown or a ```json fence.
                Do not include explanations outside the JSON object.
                Do not include unknown fields, chainOfThought, reasoning, or analysis.
                decisionSummary is REQUIRED for every decision. It must be a non-empty
                string between 1 and 500 characters and contain only a brief execution
                summary, never private chain-of-thought.

                Tools should only be called when needed to obtain missing information.
                Do not call the exact same Tool with the same arguments more than once
                in a run after it has completed successfully.
                Reuse completed Tool results already present in the context.
                If the goal can be answered without a Tool, or sufficient information is
                already available, return FINAL directly.

                The only allowed decision structures are:

                TOOL_CALL (all shown fields are required):
                {
                  "type": "TOOL_CALL",
                  "decisionSummary": "Need character context to answer the goal.",
                  "toolName": "get_character_context",
                  "arguments": {
                    "characterId": 3
                  }
                }
                TOOL_CALL must not include finalResult. toolName must be one of the
                allowed Tool names above. arguments must exactly match that Tool's input schema.

                FINAL (all shown fields are required):
                {
                  "type": "FINAL",
                  "decisionSummary": "Enough information is available to answer.",
                  "finalResult": "The final answer for the user."
                }
                FINAL must not include toolName or arguments. finalResult must be a
                non-empty string of at most 12000 characters.
                """.formatted(tools);

        if (correctionReason != null) {
            system += """


                    CORRECTION REQUIRED
                    Your previous response did not satisfy the Agent Decision JSON contract.
                    Validation error:
                    %s
                    Return the corrected decision only. Follow the complete contract above,
                    including allowed decision types, required fields, allowed Tool names,
                    Tool argument schemas, decisionSummary, and JSON-only output.
                    """.formatted(correctionReason);
        }

        String user = "Goal: " + run.getGoal()
                + "\nCompleted checkpoints:\n"
                + (history.isBlank() ? "none" : history)
                + "\nChoose TOOL_CALL or FINAL.";
        return new ComposedChatPrompt(
                List.of(
                        new LlmMessage(LlmRole.SYSTEM, system),
                        new LlmMessage(LlmRole.USER, user)
                ),
                Map.of("userId", run.getUserId(), "agentRunId", run.getId())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String limit(String value, int length) {
        return value == null ? "" : value.substring(0, Math.min(length, value.length()));
    }
}
