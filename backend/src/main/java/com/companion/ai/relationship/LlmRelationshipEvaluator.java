package com.companion.ai.relationship;

import com.companion.ai.LlmClient;
import com.companion.ai.json.LlmJsonObjectExtractor;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.PromptTemplateKey;
import com.companion.ai.prompt.PromptTemplateLoader;
import com.companion.ai.prompt.PromptTemplateRenderer;
import com.companion.entity.CharacterRelationship;
import com.companion.entity.enums.RelationshipStage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LlmRelationshipEvaluator {
    static final int MAX_SUMMARY = 1000;
    static final int MAX_INTERACTION_STYLE = 500;
    static final int MAX_RECENT_CHANGE = 1000;
    private static final int MAX_CONVERSATION_INPUT = 4000;
    private static final int MAX_OUTPUT_TOKENS = 700;
    private static final Set<String> CASUAL_GREETINGS = Set.of(
            "hi", "hello", "hey", "你好", "您好", "嗨", "哈喽", "在吗", "早上好", "下午好", "晚上好"
    );

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final PromptTemplateLoader templateLoader;
    private final PromptTemplateRenderer templateRenderer;

    public RelationshipEvaluation evaluate(CharacterRelationship current,
                                           String userMessage,
                                           String assistantResponse) {
        if (current == null || userMessage == null || userMessage.isBlank() || isOrdinaryGreeting(userMessage)) {
            return RelationshipEvaluation.unchanged();
        }
        String systemPrompt = templateRenderer.render(
                templateLoader.load(PromptTemplateKey.RELATIONSHIP_EVALUATION_SYSTEM), Map.of());
        String userPrompt = templateRenderer.render(
                templateLoader.load(PromptTemplateKey.RELATIONSHIP_EVALUATION_USER), Map.of(
                        "stage", safe(current.getStage()),
                        "summary", safe(current.getSummary()),
                        "interactionStyle", safe(current.getInteractionStyle()),
                        "recentChange", safe(current.getRecentChange()),
                        "userMessage", bounded(userMessage),
                        "assistantResponse", bounded(assistantResponse)
                ));
        try {
            LlmResponse response = llmClient.complete(systemPrompt, userPrompt, MAX_OUTPUT_TOKENS);
            String json = LlmJsonObjectExtractor.extract(response == null ? null : response.content());
            JsonNode root = objectMapper.readTree(json);
            JsonNode changedNode = root.get("changed");
            if (changedNode == null || !changedNode.isBoolean() || !changedNode.booleanValue()) {
                return RelationshipEvaluation.unchanged();
            }
            RelationshipStage stage = RelationshipStage.parse(text(root, "stage")).orElse(null);
            String summary = text(root, "summary");
            String interactionStyle = text(root, "interactionStyle");
            String recentChange = text(root, "recentChange");
            if (stage == null || !valid(summary, MAX_SUMMARY)
                    || !valid(interactionStyle, MAX_INTERACTION_STYLE)
                    || !valid(recentChange, MAX_RECENT_CHANGE)) {
                return RelationshipEvaluation.unchanged();
            }
            return new RelationshipEvaluation(true, stage, summary, interactionStyle, recentChange);
        } catch (Exception ignored) {
            return RelationshipEvaluation.unchanged();
        }
    }

    private boolean isOrdinaryGreeting(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}。！？、，]+", "");
        return CASUAL_GREETINGS.contains(normalized);
    }

    private boolean valid(String value, int maxLength) {
        return value != null && !value.isBlank() && value.length() <= maxLength;
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        return value == null || !value.isTextual() ? "" : value.textValue().trim();
    }

    private String bounded(String value) {
        if (value == null) return "";
        return value.substring(0, Math.min(value.length(), MAX_CONVERSATION_INPUT));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
