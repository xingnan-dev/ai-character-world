package com.companion.ai.memory;

public record ExtractedMemory(
        String memoryKey,
        String value,
        Integer category,
        Float importance,
        boolean singleValued
) {
}
