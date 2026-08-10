package com.companion.ai.usage;

import com.companion.ai.LlmClient;
import com.companion.ai.config.LlmProperties;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmChunk;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRequest;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.model.LlmRole;
import com.companion.ai.model.LlmUsage;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.ai.provider.LlmProvider;
import com.companion.ai.provider.LlmProviderRouter;
import com.companion.entity.AiUsageRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmClientAiUsageTest {

    private TestProvider provider;
    private RecordingUsageService usageService;
    private LlmClient client;

    @BeforeEach
    void setUp() {
        provider = new TestProvider();
        usageService = new RecordingUsageService();

        LlmProperties properties = new LlmProperties();
        properties.setProvider(TestProvider.NAME);
        properties.setModelName("request-model");
        LlmProviderRouter router = new LlmProviderRouter(properties, List.of(provider));
        client = new LlmClient(router, properties, usageService);
    }

    @Test
    void synchronousSuccessSavesUsage() {
        provider.response = new LlmResponse(
                "answer", "actual-provider", "actual-model", "stop", "provider-request",
                new LlmUsage(10, 4, 14)
        );

        assertThat(client.chat(prompt())).isEqualTo("answer");

        AiUsageRecord record = onlyRecord();
        assertThat(record.getProvider()).isEqualTo("actual-provider");
        assertThat(record.getModel()).isEqualTo("actual-model");
        assertThat(record.getUserId()).isEqualTo(101L);
        assertThat(record.getSessionId()).isEqualTo(202L);
        assertThat(record.getChatMessageId()).isEqualTo(303L);
        assertThat(record.getPromptTokens()).isEqualTo(10);
        assertThat(record.getCompletionTokens()).isEqualTo(4);
        assertThat(record.getTotalTokens()).isEqualTo(14);
        assertThat(record.getSuccess()).isEqualTo(1);
        assertThat(record.getErrorCode()).isNull();
        assertThat(record.getLatencyMs()).isNotNegative();
    }

    @Test
    void synchronousFailureSavesFailureRecordAndKeepsOriginalError() {
        LlmProviderException failure = failure(LlmErrorType.TIMEOUT);
        provider.failure = failure;

        assertThatThrownBy(() -> client.chat(prompt())).isSameAs(failure);

        AiUsageRecord record = onlyRecord();
        assertThat(record.getSuccess()).isZero();
        assertThat(record.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(record.getPromptTokens()).isNull();
    }

    @Test
    void streamingSuccessSavesLastNonEmptyUsage() {
        provider.stream = Flux.just(
                new LlmChunk("hel", 0, false, null, "p1", new LlmUsage(3, null, null)),
                new LlmChunk("lo", 1, true, "stop", "p1", new LlmUsage(8, 2, 10))
        );

        assertThat(client.streamChat(prompt()).collectList().block())
                .containsExactly("hel", "lo");

        AiUsageRecord record = onlyRecord();
        assertThat(record.getSuccess()).isEqualTo(1);
        assertThat(record.getPromptTokens()).isEqualTo(8);
        assertThat(record.getCompletionTokens()).isEqualTo(2);
        assertThat(record.getTotalTokens()).isEqualTo(10);
    }

    @Test
    void streamingFailureSavesFailedUsage() {
        LlmProviderException failure = failure(LlmErrorType.RATE_LIMIT);
        provider.stream = Flux.error(failure);

        assertThatThrownBy(() -> client.streamChat(prompt()).blockLast()).isSameAs(failure);

        AiUsageRecord record = onlyRecord();
        assertThat(record.getSuccess()).isZero();
        assertThat(record.getErrorCode()).isEqualTo("RATE_LIMIT");
    }

    @Test
    void streamingCancellationSavesCancelledUsage() {
        provider.stream = Flux.never();

        Disposable disposable = client.streamChat(prompt()).subscribe();
        disposable.dispose();

        AiUsageRecord record = onlyRecord();
        assertThat(record.getSuccess()).isZero();
        assertThat(record.getErrorCode()).isEqualTo("CANCELLED");
    }

    @Test
    void missingUsageKeepsTokenColumnsNull() {
        provider.stream = Flux.just(
                new LlmChunk("answer", 0, true, "stop", "p1", LlmUsage.empty())
        );

        client.streamChat(prompt()).blockLast();

        AiUsageRecord record = onlyRecord();
        assertThat(record.getPromptTokens()).isNull();
        assertThat(record.getCompletionTokens()).isNull();
        assertThat(record.getTotalTokens()).isNull();
    }

    @Test
    void eachStreamingRequestIsRecordedOnlyOnce() {
        provider.stream = Flux.just(
                new LlmChunk("a", 0, false, null, "p1", new LlmUsage(1, null, null)),
                new LlmChunk("b", 1, true, "stop", "p1", new LlmUsage(1, 2, 3))
        );

        client.streamChat(prompt()).collectList().block();

        assertThat(usageService.records).hasSize(1);
    }

    @Test
    void usagePersistenceFailureDoesNotAffectLlmResult() {
        provider.response = new LlmResponse(
                "answer", TestProvider.NAME, "request-model", "stop", null, LlmUsage.empty()
        );
        usageService.failOnSave = true;

        assertThat(client.chat(prompt())).isEqualTo("answer");
        assertThat(provider.completeCalls).isEqualTo(1);
    }

    private ComposedChatPrompt prompt() {
        return new ComposedChatPrompt(
                List.of(new LlmMessage(LlmRole.USER, "hello")),
                Map.of("userId", 101L, "sessionId", 202L, "assistantMessageId", 303L)
        );
    }

    private LlmProviderException failure(LlmErrorType type) {
        return new LlmProviderException(TestProvider.NAME, type, null, true, "safe error");
    }

    private AiUsageRecord onlyRecord() {
        assertThat(usageService.records).hasSize(1);
        return usageService.records.get(0);
    }

    private static class RecordingUsageService implements AiUsageService {
        private final List<AiUsageRecord> records = new ArrayList<>();
        private boolean failOnSave;

        @Override
        public void save(AiUsageRecord record) {
            if (failOnSave) {
                throw new IllegalStateException("database unavailable");
            }
            records.add(record);
        }
    }

    private static class TestProvider implements LlmProvider {
        private static final String NAME = "test-provider";
        private LlmResponse response;
        private Flux<LlmChunk> stream = Flux.empty();
        private RuntimeException failure;
        private int completeCalls;

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public LlmResponse complete(LlmRequest request) {
            completeCalls++;
            if (failure != null) {
                throw failure;
            }
            return response;
        }

        @Override
        public Flux<LlmChunk> stream(LlmRequest request) {
            return failure == null ? stream : Flux.error(failure);
        }
    }
}
