package com.companion.ai.model;

import java.util.List;
import java.util.Map;

public record LlmRequest(
        String requestId,
        String model,
        List<LlmMessage> messages,
        Double temperature,
        Integer maxTokens,
        Map<String, Object> metadata
) {
    public LlmRequest {
        messages = messages == null ? List.of() : List.copyOf(messages);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
