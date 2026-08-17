package com.companion.ai;

import com.companion.ai.config.LlmProperties;
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
import com.companion.ai.usage.AiUsageService;
import com.companion.entity.AiUsageRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Compatibility facade for existing AI services. Provider-specific behavior lives behind
 * {@link LlmProvider}; callers continue using the existing String and Flux<String> API.
 */
@Slf4j
@Component
public class LlmClient {

    private final LlmProviderRouter providerRouter;
    private final LlmProperties properties;
    private final AiUsageService aiUsageService;

    @Autowired
    public LlmClient(LlmProviderRouter providerRouter,
                     LlmProperties properties,
                     AiUsageService aiUsageService) {
        this.providerRouter = providerRouter;
        this.properties = properties;
        this.aiUsageService = aiUsageService;
    }

    /** Kept for lightweight test subclasses that replace provider behavior. */
    protected LlmClient(LlmProviderRouter providerRouter, LlmProperties properties) {
        this(providerRouter, properties, null);
    }

    public Flux<String> streamChat(String systemPrompt, String userPrompt) {
        return streamChat(new ComposedChatPrompt(
                legacyMessages(systemPrompt, userPrompt),
                Map.of()
        ));
    }

    public Flux<String> streamChat(ComposedChatPrompt prompt) {
        return Flux.defer(() -> {
            LlmRequest request = buildRequest(prompt);
            long startedAt = System.nanoTime();
            AtomicBoolean recorded = new AtomicBoolean(false);
            AtomicReference<LlmUsage> lastUsage = new AtomicReference<>();

            LlmProvider provider;
            try {
                provider = providerRouter.current();
            } catch (RuntimeException error) {
                recordOnce(recorded, failureRecord(
                        request, providerName(error), request.model(), null,
                        elapsedMillis(startedAt), errorCode(error)
                ));
                return Flux.error(error);
            }

            return provider.stream(request)
                    .doOnNext(chunk -> captureUsage(lastUsage, chunk))
                    .doOnComplete(() -> recordOnce(recorded, successRecord(
                            request, provider.name(), request.model(), lastUsage.get(), elapsedMillis(startedAt)
                    )))
                    .doOnError(error -> recordOnce(recorded, failureRecord(
                            request, providerName(error, provider.name()), request.model(), lastUsage.get(),
                            elapsedMillis(startedAt), errorCode(error)
                    )))
                    .doOnCancel(() -> recordOnce(recorded, failureRecord(
                            request, provider.name(), request.model(), lastUsage.get(),
                            elapsedMillis(startedAt), "CANCELLED"
                    )))
                    .map(chunk -> chunk.content() == null ? "" : chunk.content())
                    .filter(content -> !content.isEmpty());
        });
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(new ComposedChatPrompt(
                legacyMessages(systemPrompt, userPrompt),
                Map.of()
        ));
    }

    public String chat(ComposedChatPrompt prompt) {
        return complete(prompt, null).content();
    }

    /**
     * Executes a synchronous request while preserving provider response metadata such as
     * finishReason. The optional maxTokens override is intended for bounded structured-output
     * use cases and does not change the global chat configuration.
     */
    public LlmResponse complete(String systemPrompt, String userPrompt, Integer maxTokens) {
        return complete(new ComposedChatPrompt(
                legacyMessages(systemPrompt, userPrompt),
                Map.of()
        ), maxTokens);
    }

    public LlmResponse complete(ComposedChatPrompt prompt, Integer maxTokens) {
        LlmRequest request = buildRequest(prompt, maxTokens);
        long startedAt = System.nanoTime();
        LlmProvider provider;
        try {
            provider = providerRouter.current();
        } catch (RuntimeException error) {
            safeRecord(failureRecord(
                    request, providerName(error), request.model(), null,
                    elapsedMillis(startedAt), errorCode(error)
            ));
            throw error;
        }

        try {
            LlmResponse response = provider.complete(request);
            safeRecord(successRecord(
                    request,
                    valueOrDefault(response.provider(), provider.name()),
                    valueOrDefault(response.model(), request.model()),
                    response.usage(),
                    elapsedMillis(startedAt)
            ));
            return response;
        } catch (RuntimeException error) {
            safeRecord(failureRecord(
                    request, providerName(error, provider.name()), request.model(), null,
                    elapsedMillis(startedAt), errorCode(error)
            ));
            throw error;
        }
    }

    private void captureUsage(AtomicReference<LlmUsage> lastUsage, LlmChunk chunk) {
        if (chunk != null && hasUsage(chunk.usage())) {
            lastUsage.set(chunk.usage());
        }
    }

    private boolean hasUsage(LlmUsage usage) {
        return usage != null && (usage.promptTokens() != null
                || usage.completionTokens() != null
                || usage.totalTokens() != null);
    }

    private AiUsageRecord successRecord(LlmRequest request, String provider, String model,
                                        LlmUsage usage, long latencyMs) {
        return usageRecord(request, provider, model, usage, latencyMs, true, null);
    }

    private AiUsageRecord failureRecord(LlmRequest request, String provider, String model,
                                        LlmUsage usage, long latencyMs, String errorCode) {
        return usageRecord(request, provider, model, usage, latencyMs, false, errorCode);
    }

    private AiUsageRecord usageRecord(LlmRequest request, String provider, String model,
                                      LlmUsage usage, long latencyMs,
                                      boolean success, String errorCode) {
        AiUsageRecord record = new AiUsageRecord();
        record.setLlmRequestId(request.requestId());
        record.setChatMessageId(metadataLong(request.metadata(), "chatMessageId", "assistantMessageId"));
        record.setProvider(valueOrDefault(provider, "unknown"));
        record.setModel(valueOrDefault(model, "unknown"));
        record.setUserId(metadataLong(request.metadata(), "userId"));
        record.setSessionId(metadataLong(request.metadata(), "sessionId"));
        if (usage != null) {
            record.setPromptTokens(usage.promptTokens());
            record.setCompletionTokens(usage.completionTokens());
            record.setTotalTokens(usage.totalTokens());
        }
        record.setLatencyMs(latencyMs);
        record.setSuccess(success ? 1 : 0);
        record.setErrorCode(errorCode);
        record.setCreatedTime(LocalDateTime.now());
        return record;
    }

    private void recordOnce(AtomicBoolean recorded, AiUsageRecord record) {
        if (recorded.compareAndSet(false, true)) {
            safeRecord(record);
        }
    }

    private void safeRecord(AiUsageRecord record) {
        if (aiUsageService == null) {
            return;
        }
        try {
            aiUsageService.save(record);
        } catch (Exception error) {
            log.warn("Failed to save AI usage for llmRequestId={}: {}",
                    record.getLlmRequestId(), error.getClass().getSimpleName());
        }
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private String providerName(Throwable error) {
        return providerName(error, properties == null ? null : properties.getProvider());
    }

    private String providerName(Throwable error, String fallback) {
        if (error instanceof LlmProviderException providerException) {
            return valueOrDefault(providerException.getProvider(), fallback);
        }
        return valueOrDefault(fallback, "unknown");
    }

    private String errorCode(Throwable error) {
        if (error instanceof LlmProviderException providerException) {
            return providerException.getErrorType().name();
        }
        return "INTERNAL_ERROR";
    }

    private Long metadataLong(Map<String, Object> metadata, String... keys) {
        if (metadata == null) {
            return null;
        }
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text) {
                try {
                    return Long.valueOf(text);
                } catch (NumberFormatException ignored) {
                    // Invalid optional metadata is treated as absent.
                }
            }
        }
        return null;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<LlmMessage> legacyMessages(String systemPrompt, String userPrompt) {
        List<LlmMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new LlmMessage(LlmRole.SYSTEM, systemPrompt));
        }
        messages.add(new LlmMessage(LlmRole.USER, userPrompt == null ? "" : userPrompt));
        return messages;
    }

    private LlmRequest buildRequest(ComposedChatPrompt prompt) {
        return buildRequest(prompt, null);
    }

    private LlmRequest buildRequest(ComposedChatPrompt prompt, Integer maxTokens) {
        return new LlmRequest(
                UUID.randomUUID().toString(),
                properties.getModelName(),
                prompt.messages(),
                properties.getTemperature(),
                maxTokens == null ? properties.getMaxTokens() : maxTokens,
                prompt.metadata()
        );
    }
}
