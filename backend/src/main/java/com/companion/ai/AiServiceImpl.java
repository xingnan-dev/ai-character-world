package com.companion.ai;

import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.UserMemory;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.prompt.ChatPromptComposer;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.chat.ChatMessageLifecycleService;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ChatPromptComposer chatPromptComposer;
    private final MemoryEngine memoryEngine;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessageLifecycleService chatMessageLifecycleService;

    @Override
    public Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                                   Personality personality, ChatMessageExchange exchange) {
        StringBuilder responseBuffer = new StringBuilder();
        AtomicBoolean streamingStarted = new AtomicBoolean(false);

        return Flux.defer(() -> {
            String memoryContext = memoryEngine.getMemoryContext(userId);
            List<ChatMessage> history = loadHistory(
                    sessionId, exchange.userMessageId(), exchange.assistantMessageId()
            );
            ComposedChatPrompt prompt = chatPromptComposer.compose(
                    userId, sessionId, personality.getAvatarId(), personality, memoryContext, history, userMessage
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
                            memoryEngine.extractMemory(userMessage, aiResponse, userId);
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
    public String chat(Long userId, Long sessionId, String userMessage, Personality personality) {
        String memoryContext = memoryEngine.getMemoryContext(userId);
        List<ChatMessage> history = loadHistory(sessionId);
        ComposedChatPrompt prompt = chatPromptComposer.compose(
                userId, sessionId, personality.getAvatarId(), personality, memoryContext, history, userMessage
        );

        String aiResponse = llmClient.chat(prompt);

        try {
            memoryEngine.extractMemory(userMessage, aiResponse, userId);
        } catch (Exception e) {
            log.warn("Failed to extract memory for userId={}: {}", userId, e.getMessage());
        }

        saveMessage(sessionId, 1, userMessage);
        saveMessage(sessionId, 2, aiResponse);

        return aiResponse;
    }

    @Override
    public String buildSystemPrompt(Personality personality, UserMemory userMemory) {
        return promptBuilder.buildSystemPrompt(personality);
    }

    private List<ChatMessage> loadHistory(Long sessionId, Long... excludedMessageIds) {
        return chatMessageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .notIn(excludedMessageIds != null && excludedMessageIds.length > 0,
                                "id", (Object[]) excludedMessageIds)
                        .orderByDesc("id")
                        .last("LIMIT 20")
        );
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

    private void saveMessage(Long sessionId, Integer role, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        chatMessageMapper.insert(msg);
    }
}
