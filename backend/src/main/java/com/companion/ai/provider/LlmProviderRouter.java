package com.companion.ai.provider;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LlmProviderRouter {

    private final LlmProperties properties;
    private final Map<String, LlmProvider> providers;

    public LlmProviderRouter(LlmProperties properties, List<LlmProvider> providers) {
        this.properties = properties;
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                provider -> normalize(provider.name()), Function.identity()
        ));
    }

    public LlmProvider current() {
        String configured = properties.getMock().isEnabled() ? "mock" : properties.getProvider();
        LlmProvider provider = providers.get(normalize(configured));
        if (provider == null) {
            throw new LlmProviderException(
                    configured, LlmErrorType.CONFIGURATION, null, false,
                    "Configured LLM provider is unavailable"
            );
        }
        return provider;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
