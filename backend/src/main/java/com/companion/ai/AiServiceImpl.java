package com.companion.ai;

import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.UserMemory;
import com.companion.ai.prompt.ChatPromptComposer;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ChatPromptComposer chatPromptComposer;
    private final MemoryEngine memoryEngine;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Flux<String> chatStream(Long userId, Long sessionId, String userMessage, Personality personality) {
        String memoryContext = memoryEngine.getMemoryContext(userId);
        List<ChatMessage> history = loadHistory(sessionId);
        ComposedChatPrompt prompt = chatPromptComposer.compose(
                userId, sessionId, personality.getAvatarId(), personality, memoryContext, history, userMessage
        );

        StringBuilder responseBuffer = new StringBuilder();

        return llmClient.streamChat(prompt)
                .doOnNext(responseBuffer::append)
                .doOnComplete(() -> {
                    String aiResponse = responseBuffer.toString();
                    try {
                        memoryEngine.extractMemory(userMessage, aiResponse, userId);
                    } catch (Exception e) {
                        log.warn("Failed to extract memory for userId={}: {}", userId, e.getMessage());
                    }
                    saveMessage(sessionId, 1, userMessage);
                    saveMessage(sessionId, 2, aiResponse);
                })
                .doOnError(e -> log.error("Stream chat error for sessionId={}", sessionId, e));
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

    private List<ChatMessage> loadHistory(Long sessionId) {
        return chatMessageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByDesc("id")
                        .last("LIMIT 20")
        );
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
