package com.companion.ai;

import com.companion.entity.Personality;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.context.ChatContextAssembler;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.chat.ChatMessageLifecycleService;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.character.snapshot.CharacterSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final LlmClient llmClient;
    private final ChatContextAssembler chatContextAssembler;
    private final MemoryEngine memoryEngine;
    private final ChatMessageLifecycleService chatMessageLifecycleService;

    @Override
    public Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                                   Personality personality, ChatMessageExchange exchange) {
        return chatStream(userId, sessionId, userMessage, personality, exchange, null);
    }

    @Override
    public Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                                   Personality personality, ChatMessageExchange exchange, Long characterId) {
        StringBuilder responseBuffer = new StringBuilder();
        AtomicBoolean streamingStarted = new AtomicBoolean(false);

        return Flux.defer(() -> {
            ComposedChatPrompt prompt = chatContextAssembler.assemble(
                    userId, sessionId, userMessage, personality, exchange
            );

            return llmClient.streamChat(prompt)
                    .doOnNext(token -> {
                        ensureStreaming(exchange.assistantMessageId(), streamingStarted);
                        responseBuffer.append(token);
                    })
                    .doOnComplete(() -> {
                        ensureStreaming(exchange.assistantMessageId(), streamingStarted);
                        String aiResponse = responseBuffer.toString();
                        if (!chatMessageLifecycleService.complete(exchange.assistantMessageId(), aiResponse)) {
                            throw new IllegalStateException("Unable to complete assistant message lifecycle");
                        }
                        try {
                            memoryEngine.extractMemory(userMessage, aiResponse, userId, characterId);
                        } catch (Exception e) {
                            log.warn("Failed to extract memory for userId={}: {}", userId, e.getMessage());
                        }
                    });
        }).doOnError(error -> {
            ensureStreaming(exchange.assistantMessageId(), streamingStarted);
            String code = errorCode(error);
            chatMessageLifecycleService.fail(
                    exchange.assistantMessageId(), code, responseBuffer.toString()
            );
            log.error("Stream chat failed for sessionId={}, errorCode={}", sessionId, code);
        }).doOnCancel(() -> {
            ensureStreaming(exchange.assistantMessageId(), streamingStarted);
            chatMessageLifecycleService.cancel(exchange.assistantMessageId());
        });
    }

    @Override
    public Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                                   CharacterSnapshot character, ChatMessageExchange exchange) {
        return chatStream(userId, sessionId, userMessage, character, exchange, character == null ? null : character.sourceCharacterId());
    }

    @Override
    public Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                                   CharacterSnapshot character, ChatMessageExchange exchange, Long characterId) {
        return stream(userId, sessionId, userMessage, exchange, characterId,
                () -> chatContextAssembler.assembleCharacter(
                        userId, sessionId, userMessage, character, exchange));
    }

    private Flux<String> stream(Long userId, Long sessionId, String userMessage,
                                ChatMessageExchange exchange,
                                Long characterId,
                                java.util.function.Supplier<ComposedChatPrompt> promptSupplier) {
        StringBuilder responseBuffer = new StringBuilder();
        AtomicBoolean streamingStarted = new AtomicBoolean(false);
        return Flux.defer(() -> llmClient.streamChat(promptSupplier.get())
                .doOnNext(token -> { ensureStreaming(exchange.assistantMessageId(), streamingStarted); responseBuffer.append(token); })
                .doOnComplete(() -> {
                    ensureStreaming(exchange.assistantMessageId(), streamingStarted);
                    String response = responseBuffer.toString();
                    if (!chatMessageLifecycleService.complete(exchange.assistantMessageId(), response)) {
                        throw new IllegalStateException("Unable to complete assistant message lifecycle");
                    }
                    try { memoryEngine.extractMemory(userMessage, response, userId, characterId); }
                    catch (Exception error) { log.warn("Failed to extract memory for userId={}: {}", userId, error.getMessage()); }
                }))
                .doOnError(error -> {
                    ensureStreaming(exchange.assistantMessageId(), streamingStarted);
                    chatMessageLifecycleService.fail(exchange.assistantMessageId(), errorCode(error), responseBuffer.toString());
                    log.error("Stream chat failed for sessionId={}, errorCode={}", sessionId, errorCode(error));
                })
                .doOnCancel(() -> { ensureStreaming(exchange.assistantMessageId(), streamingStarted); chatMessageLifecycleService.cancel(exchange.assistantMessageId()); });
    }

    private void ensureStreaming(Long assistantMessageId, AtomicBoolean streamingStarted) {
        if (streamingStarted.compareAndSet(false, true)) {
            if (!chatMessageLifecycleService.markStreaming(assistantMessageId)) {
                throw new IllegalStateException("Unable to start assistant message lifecycle");
            }
        }
    }

    private String errorCode(Throwable error) {
        if (error instanceof LlmProviderException providerException) {
            return providerException.getErrorType().name();
        }
        return "INTERNAL_ERROR";
    }
}
