package com.companion.ai;

import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.UserMemory;
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
    private final MemoryEngine memoryEngine;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Flux<String> chatStream(Long userId, Long sessionId, String userMessage, Personality personality) {
        String memoryContext = memoryEngine.getMemoryContext(userId);
        UserMemory memory = buildMemoryPlaceholder(memoryContext);

        List<ChatMessage> history = loadHistory(sessionId);

        String systemPrompt = promptBuilder.buildSystemPrompt(personality);
        String userPrompt = promptBuilder.buildUserPrompt(userMessage, history, memory);

        StringBuilder responseBuffer = new StringBuilder();

        return llmClient.streamChat(systemPrompt, userPrompt)
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
        UserMemory memory = buildMemoryPlaceholder(memoryContext);

        List<ChatMessage> history = loadHistory(sessionId);

        String systemPrompt = promptBuilder.buildSystemPrompt(personality);
        String userPrompt = promptBuilder.buildUserPrompt(userMessage, history, memory);

        String aiResponse = llmClient.chat(systemPrompt, userPrompt);

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

    private UserMemory buildMemoryPlaceholder(String context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        UserMemory memory = new UserMemory();
        memory.setMemoryKey("用户背景");
        memory.setValue(context);
        return memory;
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