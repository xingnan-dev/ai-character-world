package com.companion.ai.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

@Component
public class LlmConfigurationValidator implements InitializingBean {

    private static final String OPENAI_COMPATIBLE = "openai-compatible";

    private final LlmProperties properties;

    public LlmConfigurationValidator(LlmProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        validate();
    }

    public void validate() {
        requireText(properties.getProvider(), "ai.provider must be configured");
        requirePositive(properties.getConnectTimeout(), "ai.connect-timeout must be positive");
        requirePositive(properties.getReadTimeout(), "ai.read-timeout must be positive");
        if (properties.getMaxTokens() == null || properties.getMaxTokens() <= 0) {
            throw new IllegalStateException("ai.max-tokens must be positive");
        }
        if (properties.getTemperature() != null
                && (properties.getTemperature() < 0.0 || properties.getTemperature() > 2.0)) {
            throw new IllegalStateException("ai.temperature must be between 0.0 and 2.0");
        }

        if (properties.getMock().isEnabled()) {
            return;
        }
        if (OPENAI_COMPATIBLE.equals(properties.getProvider().trim().toLowerCase(Locale.ROOT))) {
            validateOpenAiCompatible();
        }
    }

    private void validateOpenAiCompatible() {
        requireText(properties.getApiKey(), "AI_API_KEY is required when mock mode is disabled");
        requireText(properties.getModelName(), "AI_MODEL_NAME is required when mock mode is disabled");
        requireText(properties.getApiUrl(), "AI_API_URL is required when mock mode is disabled");
        try {
            URI uri = URI.create(properties.getApiUrl().trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("AI_API_URL must be a valid HTTP or HTTPS URL");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }

    private void requirePositive(Duration value, String message) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(message);
        }
    }
}
