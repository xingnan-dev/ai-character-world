package com.companion.ai.prompt;

import java.util.Set;

public record PromptTemplateDefinition(
        PromptTemplateKey key,
        String version,
        String content,
        Set<String> variables
) {
    public PromptTemplateDefinition {
        if (key == null) {
            throw new IllegalArgumentException("Prompt template key is required");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Prompt template version is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Prompt template content is required");
        }
        variables = variables == null ? Set.of() : Set.copyOf(variables);
    }
}
