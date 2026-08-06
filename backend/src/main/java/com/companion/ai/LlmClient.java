package com.companion.ai;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRequest;
import com.companion.ai.model.LlmRole;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.ai.provider.LlmProvider;
import com.companion.ai.provider.LlmProviderRouter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Compatibility facade for existing AI services. Provider-specific behavior lives behind
 * {@link LlmProvider}; callers can continue using the existing String and Flux<String> API.
 */
@Component
public class LlmClient {

    private final LlmProviderRouter providerRouter;
    private final LlmProperties properties;

    public LlmClient(LlmProviderRouter providerRouter, LlmProperties properties) {
        this.providerRouter = providerRouter;
        this.properties = properties;
    }

    public Flux<String> streamChat(String systemPrompt, String userPrompt) {
        return streamChat(new ComposedChatPrompt(
                legacyMessages(systemPrompt, userPrompt),
                Map.of()
        ));
    }

    public Flux<String> streamChat(ComposedChatPrompt prompt) {
        LlmProvider provider = providerRouter.current();
        return provider.stream(buildRequest(prompt))
                .map(chunk -> chunk.content() == null ? "" : chunk.content())
                .filter(content -> !content.isEmpty());
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(new ComposedChatPrompt(
                legacyMessages(systemPrompt, userPrompt),
                Map.of()
        ));
    }

    public String chat(ComposedChatPrompt prompt) {
        LlmProvider provider = providerRouter.current();
        return provider.complete(buildRequest(prompt)).content();
    }

    private List<LlmMessage> legacyMessages(String systemPrompt, String userPrompt) {
        List<LlmMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new LlmMessage(LlmRole.SYSTEM, systemPrompt));
        }
        messages.add(new LlmMessage(LlmRole.USER, userPrompt == null ? "" : userPrompt));
        return messages;
    }

    private LlmRequest buildRequest(ComposedChatPrompt prompt) {
        return new LlmRequest(
                UUID.randomUUID().toString(),
                properties.getModelName(),
                prompt.messages(),
                properties.getTemperature(),
                properties.getMaxTokens(),
                prompt.metadata()
        );
    }
}
