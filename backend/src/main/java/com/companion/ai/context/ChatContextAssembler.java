package com.companion.ai.context;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.MemoryEngine;
import com.companion.ai.config.LlmProperties;
import com.companion.ai.prompt.ChatPromptComposer;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.enums.ChatMessageStatus;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatContextAssembler {

    private final MemoryEngine memoryEngine;
    private final ConversationContextManager contextManager;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatPromptComposer promptComposer;
    private final LlmProperties llmProperties;

    public ComposedChatPrompt assemble(Long userId,
                                       Long sessionId,
                                       String userMessage,
                                       Personality personality,
                                       ChatMessageExchange exchange) {
        String memoryContext = contextManager.limitMemory(
                memoryEngine.getMemoryContext(userId, userMessage)
        );
        List<ChatMessage> history = loadHistory(
                sessionId, exchange.userMessageId(), exchange.assistantMessageId()
        );
        ComposedChatPrompt requiredPrompt = promptComposer.compose(
                userId, sessionId, personality.getAvatarId(), personality,
                memoryContext, List.of(), userMessage
        );
        List<ChatMessage> selectedHistory = contextManager.selectHistory(
                history, requiredPrompt.messages()
        );
        ComposedChatPrompt prompt = promptComposer.compose(
                userId, sessionId, personality.getAvatarId(), personality,
                memoryContext, selectedHistory, userMessage
        );
        return withAssistantMessageId(prompt, exchange.assistantMessageId());
    }

    public ComposedChatPrompt assembleCharacter(Long userId, Long sessionId, String userMessage,
                                                 CharacterSnapshot character,
                                                 ChatMessageExchange exchange) {
        String memoryContext = contextManager.limitMemory(memoryEngine.getMemoryContext(userId,
                character == null ? null : character.sourceCharacterId(), userMessage));
        List<ChatMessage> history = loadHistory(sessionId, exchange.userMessageId(), exchange.assistantMessageId());
        ComposedChatPrompt required = promptComposer.composeCharacter(
                userId, sessionId, character, memoryContext, List.of(), userMessage);
        List<ChatMessage> selected = contextManager.selectHistory(history, required.messages());
        return withAssistantMessageId(promptComposer.composeCharacter(
                userId, sessionId, character, memoryContext, selected, userMessage), exchange.assistantMessageId());
    }

    private List<ChatMessage> loadHistory(Long sessionId, Long userMessageId, Long assistantMessageId) {
        return chatMessageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .eq("status", ChatMessageStatus.COMPLETED.getCode())
                        .notIn("id", List.of(userMessageId, assistantMessageId))
                        .orderByDesc("id")
                        .last("LIMIT " + llmProperties.getContext().getMaxHistoryMessages())
        );
    }

    private ComposedChatPrompt withAssistantMessageId(ComposedChatPrompt prompt,
                                                       Long assistantMessageId) {
        Map<String, Object> metadata = new LinkedHashMap<>(prompt.metadata());
        metadata.put("assistantMessageId", assistantMessageId);
        return new ComposedChatPrompt(prompt.messages(), metadata);
    }
}
