package com.companion.chat;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.entity.ChatMessage;
import com.companion.entity.enums.ChatMessageStatus;
import com.companion.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultChatMessageLifecycleService implements ChatMessageLifecycleService {

    private static final int USER_ROLE = 1;
    private static final int ASSISTANT_ROLE = 2;

    private final ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional
    public ChatMessageExchange createExchange(Long sessionId, String userContent) {
        return createExchange(sessionId, userContent, null);
    }

    @Override
    @Transactional
    public ChatMessageExchange createExchange(Long sessionId, String userContent, String requestId) {
        String effectiveRequestId = normalizeRequestId(requestId);
        ChatMessageExchange existing = findExistingExchange(sessionId, effectiveRequestId);
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRequestId(effectiveRequestId);
        userMessage.setRole(USER_ROLE);
        userMessage.setContent(userContent);
        userMessage.setStatus(ChatMessageStatus.COMPLETED.getCode());
        userMessage.setCreateTime(now);
        userMessage.setUpdateTime(now);
        userMessage.setCompletionTime(now);
        try {
            chatMessageMapper.insert(userMessage);
        } catch (DuplicateKeyException duplicate) {
            ChatMessageExchange concurrent = findExistingExchange(sessionId, effectiveRequestId);
            if (concurrent != null) {
                return concurrent;
            }
            throw duplicate;
        }

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRequestId(effectiveRequestId);
        assistantMessage.setRole(ASSISTANT_ROLE);
        assistantMessage.setContent("");
        assistantMessage.setStatus(ChatMessageStatus.PENDING.getCode());
        assistantMessage.setCreateTime(now);
        assistantMessage.setUpdateTime(now);
        chatMessageMapper.insert(assistantMessage);

        return new ChatMessageExchange(
                userMessage.getId(), assistantMessage.getId(), effectiveRequestId, true
        );
    }

    @Override
    public boolean markStreaming(Long assistantMessageId) {
        return transition(
                assistantMessageId,
                ChatMessageStatus.PENDING,
                ChatMessageStatus.STREAMING,
                null,
                null
        );
    }

    @Override
    public boolean complete(Long assistantMessageId, String content) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ChatMessage> update = transitionWrapper(
                assistantMessageId, ChatMessageStatus.STREAMING, ChatMessageStatus.COMPLETED, now
        ).set(ChatMessage::getContent, content == null ? "" : content)
                .set(ChatMessage::getErrorCode, null)
                .set(ChatMessage::getErrorMessage, null)
                .set(ChatMessage::getCompletionTime, now);
        return chatMessageMapper.update(null, update) == 1;
    }

    @Override
    public boolean fail(Long assistantMessageId, String errorCode, String partialContent) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ChatMessage> update = transitionWrapper(
                assistantMessageId, ChatMessageStatus.STREAMING, ChatMessageStatus.FAILED, now
        ).set(ChatMessage::getErrorCode, errorCode)
                .set(ChatMessage::getErrorMessage, null)
                .set(ChatMessage::getCompletionTime, now);
        if (partialContent != null && !partialContent.isEmpty()) {
            update.set(ChatMessage::getContent, partialContent);
        }
        return chatMessageMapper.update(null, update) == 1;
    }

    @Override
    public boolean cancel(Long assistantMessageId) {
        return transition(
                assistantMessageId,
                ChatMessageStatus.STREAMING,
                ChatMessageStatus.CANCELLED,
                null,
                LocalDateTime.now()
        );
    }

    private boolean transition(Long messageId,
                               ChatMessageStatus expected,
                               ChatMessageStatus target,
                               String errorCode,
                               LocalDateTime completionTime) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ChatMessage> update = transitionWrapper(messageId, expected, target, now)
                .set(errorCode != null, ChatMessage::getErrorCode, errorCode)
                .set(completionTime != null, ChatMessage::getCompletionTime, completionTime);
        return chatMessageMapper.update(null, update) == 1;
    }

    private LambdaUpdateWrapper<ChatMessage> transitionWrapper(Long messageId,
                                                                ChatMessageStatus expected,
                                                                ChatMessageStatus target,
                                                                LocalDateTime updateTime) {
        return new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getId, messageId)
                .eq(ChatMessage::getRole, ASSISTANT_ROLE)
                .eq(ChatMessage::getStatus, expected.getCode())
                .set(ChatMessage::getStatus, target.getCode())
                .set(ChatMessage::getUpdateTime, updateTime);
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String normalized = requestId.trim();
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("requestId must not exceed 64 characters");
        }
        return normalized;
    }

    private ChatMessageExchange findExistingExchange(Long sessionId, String requestId) {
        ChatMessage userMessage = chatMessageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getRequestId, requestId)
                        .eq(ChatMessage::getRole, USER_ROLE)
        );
        if (userMessage == null) {
            return null;
        }
        ChatMessage assistantMessage = chatMessageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getRequestId, requestId)
                        .eq(ChatMessage::getRole, ASSISTANT_ROLE)
        );
        if (assistantMessage == null) {
            throw new IllegalStateException("Idempotent chat exchange is incomplete");
        }
        return new ChatMessageExchange(
                userMessage.getId(), assistantMessage.getId(), requestId, false
        );
    }
}
