package com.companion.entity.enums;

import java.util.Arrays;

public enum ChatMessageStatus {
    PENDING(0),
    STREAMING(1),
    COMPLETED(2),
    FAILED(3),
    CANCELLED(4),
    INTERRUPTED(5);

    private final int code;

    ChatMessageStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ChatMessageStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("Chat message status code must not be null");
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown chat message status code: " + code));
    }
}
