package com.companion.ai.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmConfigurationValidatorTest {

    @Test
    void acceptsCompleteRealProviderConfiguration() {
        LlmProperties properties = validProperties();

        assertThatCode(() -> new LlmConfigurationValidator(properties).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void mockModeDoesNotRequireRealProviderCredentials() {
        LlmProperties properties = validProperties();
        properties.getMock().setEnabled(true);
        properties.setApiKey(null);
        properties.setApiUrl(null);
        properties.setModelName(null);

        assertThatCode(() -> new LlmConfigurationValidator(properties).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingApiKeyForRealProvider() {
        LlmProperties properties = validProperties();
        properties.setApiKey(" ");

        assertThatThrownBy(() -> new LlmConfigurationValidator(properties).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_API_KEY");
    }

    @Test
    void rejectsInvalidUrlAndInvalidNumericSettings() {
        LlmProperties invalidUrlProperties = validProperties();
        invalidUrlProperties.setApiUrl("file:///tmp/provider");

        assertThatThrownBy(() -> new LlmConfigurationValidator(invalidUrlProperties).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_API_URL");

        LlmProperties invalidTemperatureProperties = validProperties();
        invalidTemperatureProperties.setTemperature(2.1);
        assertThatThrownBy(() -> new LlmConfigurationValidator(invalidTemperatureProperties).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("temperature");

        LlmProperties invalidTimeoutProperties = validProperties();
        invalidTimeoutProperties.setReadTimeout(Duration.ZERO);
        assertThatThrownBy(() -> new LlmConfigurationValidator(invalidTimeoutProperties).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("read-timeout");
    }

    private LlmProperties validProperties() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("openai-compatible");
        properties.setApiUrl("https://api.example.test/v1/chat/completions");
        properties.setApiKey("test-key");
        properties.setModelName("test-model");
        properties.setMaxTokens(1024);
        properties.setTemperature(0.7);
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(2));
        return properties;
    }
}
