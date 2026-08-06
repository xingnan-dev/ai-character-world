package com.companion.ai.provider;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmChunk;
import com.companion.ai.model.LlmRequest;
import com.companion.ai.model.LlmResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmProviderRouterTest {

    @Test
    void resolvesConfiguredProvider() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("TEST");
        LlmProvider expected = provider("test");

        assertThat(new LlmProviderRouter(properties, List.of(expected)).current()).isSameAs(expected);
    }

    @Test
    void legacyMockFlagSelectsMockProvider() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("test");
        properties.getMock().setEnabled(true);
        LlmProvider mock = provider("mock");

        assertThat(new LlmProviderRouter(properties, List.of(provider("test"), mock)).current()).isSameAs(mock);
    }

    @Test
    void unknownProviderProducesSafeConfigurationException() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("missing");

        assertThatThrownBy(() -> new LlmProviderRouter(properties, List.of()).current())
                .isInstanceOfSatisfying(LlmProviderException.class, exception -> {
                    assertThat(exception.getErrorType()).isEqualTo(LlmErrorType.CONFIGURATION);
                    assertThat(exception.isRetryable()).isFalse();
                });
    }

    private LlmProvider provider(String name) {
        return new LlmProvider() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public LlmResponse complete(LlmRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<LlmChunk> stream(LlmRequest request) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
