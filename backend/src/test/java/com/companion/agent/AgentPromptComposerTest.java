package com.companion.agent;

import com.companion.agent.tool.AgentTool;
import com.companion.agent.tool.AgentToolRegistry;
import com.companion.entity.AgentRun;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPromptComposerTest {

    @Test
    void initialPromptContainsStrictCompleteDecisionContract() {
        AgentPromptComposer composer = new AgentPromptComposer(
                new AgentToolRegistry(List.of(characterTool())));

        String system = composer.compose(run(), List.of(), null)
                .messages().get(0).content();

        assertThat(system)
                .contains("Return JSON only")
                .contains("Do not use markdown or a " + "\u0060\u0060\u0060json fence")
                .contains("decisionSummary is REQUIRED for every decision")
                .contains("between 1 and 500 characters")
                .contains("\"type\": \"TOOL_CALL\"")
                .contains("\"decisionSummary\": \"Need character context to answer the goal.\"")
                .contains("\"toolName\": \"get_character_context\"")
                .contains("\"arguments\"")
                .contains("\"type\": \"FINAL\"")
                .contains("\"finalResult\": \"The final answer for the user.\"")
                .contains("Do not include unknown fields, chainOfThought, reasoning, or analysis")
                .contains("Tools should only be called when needed to obtain missing information")
                .contains("Do not call the exact same Tool with the same arguments more than once")
                .contains("Reuse completed Tool results already present in the context")
                .contains("If the goal can be answered without a Tool")
                .contains("return FINAL directly")
                .contains("- get_character_context: Read character context.")
                .doesNotContain("CORRECTION REQUIRED");
    }

    @Test
    void correctionPromptIncludesSanitizedValidationReasonAndFullContract() {
        AgentPromptComposer composer = new AgentPromptComposer(
                new AgentToolRegistry(List.of(characterTool())));

        String system = composer.compose(
                        run(),
                        List.of(),
                        "decisionSummary is required and must contain 1-500 characters")
                .messages().get(0).content();

        assertThat(system)
                .contains("CORRECTION REQUIRED")
                .contains("Your previous response did not satisfy the Agent Decision JSON contract")
                .contains("Validation error:")
                .contains("decisionSummary is required and must contain 1-500 characters")
                .contains("Return the corrected decision only")
                .contains("allowed Tool names")
                .contains("Tool argument schemas")
                .contains("\"type\": \"TOOL_CALL\"")
                .contains("\"type\": \"FINAL\"");
    }

    private AgentRun run() {
        AgentRun run = new AgentRun();
        run.setId(7L);
        run.setUserId(11L);
        run.setGoal("Test goal");
        return run;
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
            }

            @Override
            public String execute(Long userId, JsonNode arguments) {
                return "";
            }
        };
    }
}
