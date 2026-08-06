package com.companion.ai.prompt;

import java.util.Set;

public enum PromptTemplateKey {

    CHAT_SAFETY("prompts/chat/safety-v1.md", "1", Set.of()),
    CHAT_PERSONALITY(
            "prompts/chat/personality-v1.md",
            "1",
            Set.of("name", "corePersonality", "identity", "languageStyle", "hobbies", "relationship")
    ),
    CHAT_MEMORY("prompts/chat/memory-v1.md", "1", Set.of("memoryContext")),
    AVATAR_GENERATION_SYSTEM("prompts/avatar/generation-system-v1.md", "1", Set.of()),
    AVATAR_GENERATION_USER("prompts/avatar/generation-user-v1.md", "1", Set.of("userDescription"));

    private final String resourcePath;
    private final String version;
    private final Set<String> variables;

    PromptTemplateKey(String resourcePath, String version, Set<String> variables) {
        this.resourcePath = resourcePath;
        this.version = version;
        this.variables = Set.copyOf(variables);
    }

    public String resourcePath() {
        return resourcePath;
    }

    public String version() {
        return version;
    }

    public Set<String> variables() {
        return variables;
    }
}
