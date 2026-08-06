package com.companion.ai.prompt;

import com.companion.ai.model.LlmMessage;

import java.util.List;
import java.util.Map;

public record ComposedChatPrompt(
        List<LlmMessage> messages,
        Map<String, Object> metadata
) {
    public ComposedChatPrompt {
        messages = messages == null ? List.of() : List.copyOf(messages);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
