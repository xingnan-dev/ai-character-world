package com.companion.ai.prompt;

public interface PromptTemplateLoader {

    PromptTemplateDefinition load(PromptTemplateKey key);
}
