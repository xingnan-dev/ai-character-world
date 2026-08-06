package com.companion.ai.model;

public record LlmChunk(
        String content,
        long sequence,
        boolean finished,
        String finishReason,
        String providerRequestId,
        LlmUsage usage
) {
}
