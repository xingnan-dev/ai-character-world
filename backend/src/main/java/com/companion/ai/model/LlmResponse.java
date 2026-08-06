package com.companion.ai.model;

public record LlmResponse(
        String content,
        String provider,
        String model,
        String finishReason,
        String providerRequestId,
        LlmUsage usage
) {
}
