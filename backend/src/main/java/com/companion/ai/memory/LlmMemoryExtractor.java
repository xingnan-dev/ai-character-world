package com.companion.ai.memory;

import com.companion.ai.LlmClient;
import com.companion.ai.json.LlmJsonObjectExtractor;
import com.companion.ai.model.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmMemoryExtractor {
    private static final int MAX_INPUT = 4000;
    private static final int MAX_OUTPUT = 700;
    private final LlmClient llmClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public List<ExtractedMemory> extract(String userMessage, String aiResponse) {
        if (userMessage == null || userMessage.isBlank()) return List.of();
        String input = userMessage.substring(0, Math.min(MAX_INPUT, userMessage.length()));
        String prompt = "Extract durable user memories from this conversation. Return only JSON "
                + "{\"memories\":[{\"type\":\"USER_FACT|PREFERENCE|EXPERIENCE|RELATIONSHIP_CONTEXT\","
                + "\"key\":\"...\",\"value\":\"...\",\"importance\":0.0,\"confidence\":0.0}]}\n"
                + "USER: " + input;
        try {
            LlmResponse response = llmClient.complete("You extract concise structured memories.", prompt, MAX_OUTPUT);
            String json = LlmJsonObjectExtractor.extract(response == null ? null : response.content());
            JsonNode items = mapper.readTree(json).path("memories");
            if (!items.isArray()) return List.of();
            List<ExtractedMemory> result = new ArrayList<>();
            for (JsonNode item : items) {
                String key = text(item, "key"); String value = text(item, "value");
                if (key.isBlank() || value.isBlank()) continue;
                int category = switch (text(item, "type")) {
                    case "PREFERENCE" -> 2; case "EXPERIENCE" -> 3;
                    case "RELATIONSHIP_CONTEXT" -> 4; default -> 1;
                };
                float importance = bounded(item.path("importance").asDouble(0.5));
                result.add(new ExtractedMemory(key, value, category, importance, category != 2));
            }
            return result;
        } catch (Exception ignored) { return List.of(); }
    }

    private String text(JsonNode node, String field) { return node.path(field).asText("").trim(); }
    private float bounded(double value) { return (float) Math.max(0, Math.min(1, value)); }
}
