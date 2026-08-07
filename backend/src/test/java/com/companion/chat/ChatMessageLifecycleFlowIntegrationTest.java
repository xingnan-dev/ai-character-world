package com.companion.chat;

import com.companion.ai.AiService;
import com.companion.ai.LlmClient;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.enums.ChatMessageStatus;
import com.companion.mapper.ChatMessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
@Import(ChatMessageLifecycleFlowIntegrationTest.LlmTestConfiguration.class)
class ChatMessageLifecycleFlowIntegrationTest {

    private static final long USER_ID = 1001L;
    private static final long SESSION_ID = 2001L;

    @Autowired
    private ChatMessageLifecycleService lifecycleService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private AiService aiService;

    @Autowired
    private TestLlmClient llmClient;

    @Test
    void normalChatCompletesBothMessages() {
        ChatMessageExchange exchange = lifecycleService.createExchange(SESSION_ID, "hello");
        llmClient.respondWith(Flux.just("hel", "lo"));

        String response = aiService.chatStream(
                USER_ID, SESSION_ID, "hello", personality(), exchange
        ).collectList().map(parts -> String.join("", parts)).block();

        assertThat(response).isEqualTo("hello");
        ChatMessage userMessage = chatMessageMapper.selectById(exchange.userMessageId());
        ChatMessage assistantMessage = chatMessageMapper.selectById(exchange.assistantMessageId());
        assertThat(userMessage.getStatus()).isEqualTo(ChatMessageStatus.COMPLETED.getCode());
        assertThat(userMessage.getCompletionTime()).isNotNull();
        assertThat(assistantMessage.getStatus()).isEqualTo(ChatMessageStatus.COMPLETED.getCode());
        assertThat(assistantMessage.getContent()).isEqualTo("hello");
        assertThat(assistantMessage.getErrorCode()).isNull();
        assertThat(assistantMessage.getCompletionTime()).isNotNull();
    }

    @Test
    void providerFailureKeepsUserMessageAndFailsAssistantWithoutSensitiveMessage() {
        ChatMessageExchange exchange = lifecycleService.createExchange(SESSION_ID, "hello");
        llmClient.respondWith(Flux.error(
                new LlmProviderException(
                        "test", LlmErrorType.TIMEOUT, null, true, "sensitive upstream detail"
                )
        ));

        assertThatThrownBy(() -> aiService.chatStream(
                USER_ID, SESSION_ID, "hello", personality(), exchange
        ).blockLast()).isInstanceOf(LlmProviderException.class);

        ChatMessage userMessage = chatMessageMapper.selectById(exchange.userMessageId());
        ChatMessage assistantMessage = chatMessageMapper.selectById(exchange.assistantMessageId());
        assertThat(userMessage.getStatus()).isEqualTo(ChatMessageStatus.COMPLETED.getCode());
        assertThat(assistantMessage.getStatus()).isEqualTo(ChatMessageStatus.FAILED.getCode());
        assertThat(assistantMessage.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(assistantMessage.getErrorMessage()).isNull();
    }

    @Test
    void partialOutputFailureStoresPartialContentAndFailsAssistant() {
        ChatMessageExchange exchange = lifecycleService.createExchange(SESSION_ID, "hello");
        llmClient.respondWith(Flux.concat(
                Flux.just("partial"),
                Flux.error(new LlmProviderException(
                        "test", LlmErrorType.RATE_LIMIT, 429, true, "provider body"
                ))
        ));

        assertThatThrownBy(() -> aiService.chatStream(
                USER_ID, SESSION_ID, "hello", personality(), exchange
        ).blockLast()).isInstanceOf(LlmProviderException.class);

        ChatMessage assistantMessage = chatMessageMapper.selectById(exchange.assistantMessageId());
        assertThat(assistantMessage.getStatus()).isEqualTo(ChatMessageStatus.FAILED.getCode());
        assertThat(assistantMessage.getContent()).isEqualTo("partial");
        assertThat(assistantMessage.getErrorCode()).isEqualTo("RATE_LIMIT");
        assertThat(assistantMessage.getErrorMessage()).isNull();
    }

    @Test
    void terminalStateCannotBeModifiedAgain() {
        ChatMessageExchange exchange = lifecycleService.createExchange(SESSION_ID, "hello");

        assertThat(lifecycleService.complete(exchange.assistantMessageId(), "invalid")).isFalse();
        assertThat(lifecycleService.fail(exchange.assistantMessageId(), "TIMEOUT", "")).isFalse();
        assertThat(lifecycleService.cancel(exchange.assistantMessageId())).isFalse();
        assertThat(lifecycleService.markStreaming(exchange.assistantMessageId())).isTrue();
        assertThat(lifecycleService.complete(exchange.assistantMessageId(), "done")).isTrue();
        assertThat(lifecycleService.complete(exchange.assistantMessageId(), "changed")).isFalse();
        assertThat(lifecycleService.fail(exchange.assistantMessageId(), "TIMEOUT", "changed")).isFalse();
        assertThat(lifecycleService.cancel(exchange.assistantMessageId())).isFalse();

        ChatMessage assistantMessage = chatMessageMapper.selectById(exchange.assistantMessageId());
        assertThat(assistantMessage.getStatus()).isEqualTo(ChatMessageStatus.COMPLETED.getCode());
        assertThat(assistantMessage.getContent()).isEqualTo("done");
    }

    private Personality personality() {
        Personality personality = new Personality();
        personality.setId(3001L);
        personality.setAvatarId(4001L);
        personality.setName("Test personality");
        personality.setCorePersonality("helpful");
        personality.setIdentity("assistant");
        personality.setLanguageStyle("clear");
        return personality;
    }

    @TestConfiguration
    static class LlmTestConfiguration {

        @Bean
        @Primary
        TestLlmClient testLlmClient() {
            return new TestLlmClient();
        }
    }

    static class TestLlmClient extends LlmClient {

        private Flux<String> response = Flux.empty();

        TestLlmClient() {
            super(null, null);
        }

        void respondWith(Flux<String> response) {
            this.response = response;
        }

        @Override
        public Flux<String> streamChat(ComposedChatPrompt prompt) {
            return response;
        }
    }
}
