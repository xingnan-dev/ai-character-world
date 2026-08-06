package com.companion.ai;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRequest;
import com.companion.ai.model.LlmRole;
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
        LlmProvider provider = providerRouter.current();
        return provider.stream(buildRequest(systemPrompt, userPrompt))
                .map(chunk -> chunk.content() == null ? "" : chunk.content())
                .filter(content -> !content.isEmpty());
    }

    public String chat(String systemPrompt, String userPrompt) {
        LlmProvider provider = providerRouter.current();
        return provider.complete(buildRequest(systemPrompt, userPrompt)).content();
    }

    private LlmRequest buildRequest(String systemPrompt, String userPrompt) {
        List<LlmMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new LlmMessage(LlmRole.SYSTEM, systemPrompt));
        }
        messages.add(new LlmMessage(LlmRole.USER, userPrompt == null ? "" : userPrompt));

        return new LlmRequest(
                UUID.randomUUID().toString(),
                properties.getModelName(),
                messages,
                properties.getTemperature(),
                properties.getMaxTokens(),
                Map.of()
        );
    }
}
