package com.companion.chat;

import com.companion.chat.model.ChatMessageExchange;

public interface ChatMessageLifecycleService {

    ChatMessageExchange createExchange(Long sessionId, String userContent);

    boolean markStreaming(Long assistantMessageId);

    boolean complete(Long assistantMessageId, String content);

    boolean fail(Long assistantMessageId, String errorCode, String partialContent);

    boolean cancel(Long assistantMessageId);
}
