package com.companion.chat;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.entity.ChatMessage;
import com.companion.entity.enums.ChatMessageStatus;
import com.companion.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DefaultChatMessageLifecycleService implements ChatMessageLifecycleService {

    private static final int USER_ROLE = 1;
    private static final int ASSISTANT_ROLE = 2;

    private final ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional
    public ChatMessageExchange createExchange(Long sessionId, String userContent) {
        LocalDateTime now = LocalDateTime.now();

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole(USER_ROLE);
        userMessage.setContent(userContent);
        userMessage.setStatus(ChatMessageStatus.COMPLETED.getCode());
        userMessage.setCreateTime(now);
        userMessage.setUpdateTime(now);
        userMessage.setCompletionTime(now);
        chatMessageMapper.insert(userMessage);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRole(ASSISTANT_ROLE);
        assistantMessage.setContent("");
        assistantMessage.setStatus(ChatMessageStatus.PENDING.getCode());
        assistantMessage.setCreateTime(now);
        assistantMessage.setUpdateTime(now);
        chatMessageMapper.insert(assistantMessage);

        return new ChatMessageExchange(userMessage.getId(), assistantMessage.getId());
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
}
