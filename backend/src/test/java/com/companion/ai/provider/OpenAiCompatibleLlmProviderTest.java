package com.companion.ai.provider;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRequest;
import com.companion.ai.model.LlmRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleLlmProviderTest {

    private HttpServer server;
    private String requestBody;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void mapsCompletionResponseAndUsage() throws Exception {
        startServer(200, """
                {"id":"provider-1","model":"test-model","choices":[{"message":{"content":"hello"},"finish_reason":"stop"}],"usage":{"prompt_tokens":3,"completion_tokens":2,"total_tokens":5}}
                """);

        var response = provider().complete(request());

        assertThat(response.content()).isEqualTo("hello");
        assertThat(response.providerRequestId()).isEqualTo("provider-1");
        assertThat(response.usage().totalTokens()).isEqualTo(5);
    }

    @Test
    void parsesStreamingDataAndDoneMarker() throws Exception {
        startServer(200, """
                data: {"id":"stream-1","choices":[{"delta":{"content":"hel"},"finish_reason":null}]}

                data: {"id":"stream-1","choices":[{"delta":{"content":"lo"},"finish_reason":"stop"}]}

                data: [DONE]

                """);

        String content = provider().stream(request())
                .map(chunk -> chunk.content() == null ? "" : chunk.content())
                .collectList()
                .map(parts -> String.join("", parts))
                .block(Duration.ofSeconds(3));

        assertThat(content).isEqualTo("hello");
        assertThat(requestBody).contains("\"stream_options\":{\"include_usage\":true}");
    }

    @Test
    void parsesUsageOnlyTailWithEmptyChoices() throws Exception {
        startServer(200, """
                data: {"id":"stream-usage","choices":[{"delta":{"content":"hello"},"finish_reason":"stop"}]}

                data: {"id":"stream-usage","choices":[],"usage":{"prompt_tokens":7,"completion_tokens":2,"total_tokens":9}}

                data: [DONE]

                """);

        var chunks = provider().stream(request()).collectList().block(Duration.ofSeconds(3));

        assertThat(chunks).isNotNull();
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(1).content()).isEmpty();
        assertThat(chunks.get(1).usage().totalTokens()).isEqualTo(9);
    }

    @Test
    void rejectsUnexpectedEndWithoutDoneOrFinishReason() throws Exception {
        startServer(200, """
                data: {"id":"stream-cut","choices":[{"delta":{"content":"partial"},"finish_reason":null}]}

                """);

        assertThatThrownBy(() -> provider().stream(request()).blockLast(Duration.ofSeconds(3)))
                .isInstanceOfSatisfying(LlmProviderException.class, exception ->
                        assertThat(exception.getErrorType()).isEqualTo(LlmErrorType.INVALID_RESPONSE));
    }

    @Test
    void mapsRateLimitWithoutExposingProviderBody() throws Exception {
        startServer(429, "{\"error\":\"secret provider detail\"}");

        assertThatThrownBy(() -> provider().complete(request()))
                .isInstanceOfSatisfying(LlmProviderException.class, exception -> {
                    assertThat(exception.getErrorType()).isEqualTo(LlmErrorType.RATE_LIMIT);
                    assertThat(exception.isRetryable()).isTrue();
                    assertThat(exception.getMessage()).doesNotContain("secret provider detail");
                });
    }

    @Test
    void mapsInvalidRequestAndRetryableServerFailure() throws Exception {
        startServer(422, "{\"error\":\"invalid input detail\"}");
        assertThatThrownBy(() -> provider().complete(request()))
                .isInstanceOfSatisfying(LlmProviderException.class, exception -> {
                    assertThat(exception.getErrorType()).isEqualTo(LlmErrorType.INVALID_REQUEST);
                    assertThat(exception.isRetryable()).isFalse();
                    assertThat(exception.getMessage()).doesNotContain("invalid input detail");
                });

        stopServer();
        startServer(503, "{\"error\":\"internal provider detail\"}");
        assertThatThrownBy(() -> provider().complete(request()))
                .isInstanceOfSatisfying(LlmProviderException.class, exception -> {
                    assertThat(exception.getErrorType()).isEqualTo(LlmErrorType.UPSTREAM_ERROR);
                    assertThat(exception.isRetryable()).isTrue();
                });
    }

    private OpenAiCompatibleLlmProvider provider() {
        LlmProperties properties = new LlmProperties();
        properties.setApiUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/chat");
        properties.setApiKey("test-api-key");
        properties.setModelName("test-model");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(2));
        return new OpenAiCompatibleLlmProvider(properties, new ObjectMapper());
    }

    private LlmRequest request() {
        return new LlmRequest(
                "client-request", "test-model",
                List.of(new LlmMessage(LlmRole.USER, "hello")),
                null, null, Map.of()
        );
    }

    private void startServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat", exchange -> respond(exchange, status, responseBody));
        server.start();
    }

    private void respond(HttpExchange exchange, int status, String responseBody) throws IOException {
        requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
