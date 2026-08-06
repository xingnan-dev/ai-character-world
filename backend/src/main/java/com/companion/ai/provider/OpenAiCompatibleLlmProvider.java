package com.companion.ai.provider;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmChunk;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRequest;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.model.LlmUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OpenAiCompatibleLlmProvider implements LlmProvider {

    public static final String NAME = "openai-compatible";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public OpenAiCompatibleLlmProvider(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(properties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(properties.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        Request httpRequest = buildRequest(request, false);
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            ensureSuccessful(response);
            JsonNode root = readBody(response);
            JsonNode choice = firstChoice(root);
            JsonNode message = choice.path("message");
            if (!message.has("content") || message.get("content").isNull()) {
                throw invalidResponse("LLM response does not contain message content", null);
            }
            return new LlmResponse(
                    message.get("content").asText(),
                    NAME,
                    root.path("model").asText(request.model()),
                    textOrNull(choice.get("finish_reason")),
                    providerRequestId(response, root),
                    parseUsage(root.get("usage"))
            );
        } catch (LlmProviderException e) {
            throw e;
        } catch (IOException e) {
            throw transportException(e);
        }
    }

    @Override
    public Flux<LlmChunk> stream(LlmRequest request) {
        return Flux.create(sink -> {
            Request httpRequest;
            try {
                httpRequest = buildRequest(request, true);
            } catch (LlmProviderException e) {
                sink.error(e);
                return;
            }

            Call call = httpClient.newCall(httpRequest);
            sink.onCancel(call::cancel);
            sink.onDispose(call::cancel);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call failedCall, IOException e) {
                    if (failedCall.isCanceled()) {
                        sink.error(new LlmProviderException(
                                NAME, LlmErrorType.CANCELLED, null, false, "LLM request was cancelled", e
                        ));
                    } else {
                        sink.error(transportException(e));
                    }
                }

                @Override
                public void onResponse(Call completedCall, Response response) {
                    try (response) {
                        ensureSuccessful(response);
                        ResponseBody body = response.body();
                        if (body == null) {
                            throw invalidResponse("LLM stream response body is empty", null);
                        }
                        parseStream(response, body, sink, request);
                    } catch (LlmProviderException e) {
                        sink.error(e);
                    } catch (Exception e) {
                        sink.error(invalidResponse("Unable to parse LLM stream response", e));
                    }
                }
            });
        });
    }

    private void parseStream(Response response, ResponseBody body,
                             reactor.core.publisher.FluxSink<LlmChunk> sink,
                             LlmRequest request) throws IOException {
        AtomicLong sequence = new AtomicLong();
        String responseRequestId = response.header("x-request-id");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {
            String line;
            while (!sink.isCancelled() && (line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    sink.complete();
                    return;
                }

                JsonNode root = objectMapper.readTree(data);
                JsonNode choice = firstChoice(root);
                JsonNode delta = choice.path("delta");
                String content = delta.has("content") && !delta.get("content").isNull()
                        ? delta.get("content").asText() : "";
                String finishReason = textOrNull(choice.get("finish_reason"));
                if (!content.isEmpty() || finishReason != null) {
                    sink.next(new LlmChunk(
                            content,
                            sequence.getAndIncrement(),
                            finishReason != null,
                            finishReason,
                            responseRequestId != null ? responseRequestId : textOrNull(root.get("id")),
                            parseUsage(root.get("usage"))
                    ));
                }
            }
            if (!sink.isCancelled()) {
                sink.complete();
            }
        }
    }

    private Request buildRequest(LlmRequest request, boolean stream) {
        validateConfiguration();
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            for (LlmMessage message : request.messages()) {
                messages.add(Map.of(
                        "role", message.role().wireName(),
                        "content", message.content() == null ? "" : message.content()
                ));
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", request.model());
            payload.put("messages", messages);
            payload.put("stream", stream);
            if (request.temperature() != null) {
                payload.put("temperature", request.temperature());
            }
            if (request.maxTokens() != null) {
                payload.put("max_tokens", request.maxTokens());
            }

            return new Request.Builder()
                    .url(properties.getApiUrl())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("X-Client-Request-Id", request.requestId())
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(payload), JSON))
                    .build();
        } catch (IllegalArgumentException | IOException e) {
            throw new LlmProviderException(
                    NAME, LlmErrorType.CONFIGURATION, null, false, "Invalid LLM provider configuration", e
            );
        }
    }

    private void validateConfiguration() {
        if (properties.getApiUrl() == null || properties.getApiUrl().isBlank()
                || properties.getApiKey() == null || properties.getApiKey().isBlank()
                || properties.getModelName() == null || properties.getModelName().isBlank()) {
            throw new LlmProviderException(
                    NAME, LlmErrorType.CONFIGURATION, null, false, "LLM provider configuration is incomplete"
            );
        }
    }

    private void ensureSuccessful(Response response) {
        if (response.isSuccessful()) {
            return;
        }
        int status = response.code();
        LlmErrorType type = status == 401 || status == 403 ? LlmErrorType.AUTHENTICATION
                : status == 429 ? LlmErrorType.RATE_LIMIT
                : status == 408 || status == 504 ? LlmErrorType.TIMEOUT
                : LlmErrorType.UPSTREAM_ERROR;
        boolean retryable = status == 429 || status == 408 || status == 504 || status >= 500;
        throw new LlmProviderException(NAME, type, status, retryable, "LLM provider request failed");
    }

    private JsonNode readBody(Response response) throws IOException {
        ResponseBody body = response.body();
        if (body == null) {
            throw invalidResponse("LLM response body is empty", null);
        }
        try {
            return objectMapper.readTree(body.byteStream());
        } catch (IOException e) {
            throw invalidResponse("Unable to parse LLM response", e);
        }
    }

    private JsonNode firstChoice(JsonNode root) {
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw invalidResponse("LLM response does not contain choices", null);
        }
        return choices.get(0);
    }

    private String providerRequestId(Response response, JsonNode root) {
        String header = response.header("x-request-id");
        return header != null ? header : textOrNull(root.get("id"));
    }

    private LlmUsage parseUsage(JsonNode usage) {
        if (usage == null || usage.isNull() || usage.isMissingNode()) {
            return LlmUsage.empty();
        }
        return new LlmUsage(
                integerOrNull(usage.get("prompt_tokens")),
                integerOrNull(usage.get("completion_tokens")),
                integerOrNull(usage.get("total_tokens"))
        );
    }

    private Integer integerOrNull(JsonNode value) {
        return value != null && value.isNumber() ? value.asInt() : null;
    }

    private String textOrNull(JsonNode value) {
        return value != null && !value.isNull() && !value.isMissingNode() ? value.asText() : null;
    }

    private LlmProviderException transportException(IOException e) {
        boolean timeout = e instanceof SocketTimeoutException;
        return new LlmProviderException(
                NAME,
                timeout ? LlmErrorType.TIMEOUT : LlmErrorType.NETWORK,
                null,
                true,
                timeout ? "LLM provider request timed out" : "Unable to reach LLM provider",
                e
        );
    }

    private LlmProviderException invalidResponse(String message, Throwable cause) {
        return new LlmProviderException(
                NAME, LlmErrorType.INVALID_RESPONSE, null, false, message, cause
        );
    }
}
