package com.companion.chat.model;

public record ChatMessageExchange(Long userMessageId, Long assistantMessageId,
                                  String requestId, boolean created) {

    public ChatMessageExchange(Long userMessageId, Long assistantMessageId) {
        this(userMessageId, assistantMessageId, null, true);
    }
}
