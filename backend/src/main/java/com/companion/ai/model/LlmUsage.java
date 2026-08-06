package com.companion.ai.model;

public record LlmUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {

    public static LlmUsage empty() {
        return new LlmUsage(null, null, null);
    }
}
