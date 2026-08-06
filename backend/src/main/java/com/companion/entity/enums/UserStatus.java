package com.companion.entity.enums;

public enum UserStatus {
    DISABLED(0),
    ACTIVE(1),
    LOCKED(2);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static boolean isActive(Integer code) {
        return code != null && code == ACTIVE.code;
    }
}
