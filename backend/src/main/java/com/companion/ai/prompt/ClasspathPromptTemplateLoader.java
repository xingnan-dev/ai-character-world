package com.companion.ai.prompt;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ClasspathPromptTemplateLoader implements PromptTemplateLoader {

    private final ResourceLoader resourceLoader;
    private final PromptTemplateRenderer renderer;
    private final ConcurrentMap<PromptTemplateKey, PromptTemplateDefinition> cache = new ConcurrentHashMap<>();

    public ClasspathPromptTemplateLoader(ResourceLoader resourceLoader, PromptTemplateRenderer renderer) {
        this.resourceLoader = resourceLoader;
        this.renderer = renderer;
    }

    @Override
    public PromptTemplateDefinition load(PromptTemplateKey key) {
        if (key == null) {
            throw new PromptTemplateException("Prompt template key is required");
        }
        return cache.computeIfAbsent(key, this::readTemplate);
    }

    private PromptTemplateDefinition readTemplate(PromptTemplateKey key) {
        Resource resource = resourceLoader.getResource("classpath:" + key.resourcePath());
        if (!resource.exists() || !resource.isReadable()) {
            throw new PromptTemplateException("Prompt template resource is unavailable: " + key.name());
        }

        try (InputStream input = resource.getInputStream()) {
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
            Set<String> actualVariables = renderer.findVariables(content);
            if (!actualVariables.equals(key.variables())) {
                throw new PromptTemplateException(
                        "Prompt template variable declaration mismatch: " + key.name()
                                + "; expected=" + key.variables() + "; actual=" + actualVariables
                );
            }
            return new PromptTemplateDefinition(key, key.version(), content, actualVariables);
        } catch (IOException e) {
            throw new PromptTemplateException("Unable to read prompt template: " + key.name(), e);
        }
    }
}
