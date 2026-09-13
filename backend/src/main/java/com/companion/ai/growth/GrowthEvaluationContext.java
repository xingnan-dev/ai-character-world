package com.companion.ai.growth;

public record GrowthEvaluationContext(
        String characterIdentity,
        String relationship,
        String memory,
        String userMessage,
        String assistantResponse
) {}
