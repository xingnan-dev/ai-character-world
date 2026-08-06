package com.companion.ai.provider;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRequest;
import com.companion.ai.model.LlmRole;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MockLlmProviderTest {

    @Test
    void returnsDeterministicChatResponseForSyncAndStreamCalls() {
        LlmProperties properties = properties();
        MockLlmProvider provider = new MockLlmProvider(properties);
        LlmRequest request = request("你是一个温柔的伙伴", "你好");

        String complete = provider.complete(request).content();
        String streamed = String.join("", provider.stream(request)
                .map(chunk -> chunk.content() == null ? "" : chunk.content())
                .collectList()
                .block());

        assertThat(complete).isEqualTo(streamed).contains("你好");
    }

    @Test
    void returnsValidAvatarJsonForAvatarGenerationPrompt() {
        MockLlmProvider provider = new MockLlmProvider(properties());

        String response = provider.complete(request(
                "输出包含appearanceConfig的人物JSON", "请创建银发温柔女性角色"
        )).content();

        assertThat(response)
                .contains("\"appearanceConfig\"")
                .contains("\"hairColor\":\"silver\"")
                .contains("\"type\":\"gentle\"");
    }

    private LlmProperties properties() {
        LlmProperties properties = new LlmProperties();
        properties.setModelName("mock-model");
        properties.getMock().setDelay(Duration.ZERO);
        return properties;
    }

    private LlmRequest request(String system, String user) {
        return new LlmRequest(
                "test-request", "mock-model",
                List.of(new LlmMessage(LlmRole.SYSTEM, system), new LlmMessage(LlmRole.USER, user)),
                null, null, Map.of()
        );
    }
}
