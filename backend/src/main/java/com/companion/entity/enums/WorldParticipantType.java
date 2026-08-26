package com.companion.entity.enums;

import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;

import java.util.Locale;

public enum WorldParticipantType {
    AI(1), USER(2);

    private final int code;

    WorldParticipantType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static WorldParticipantType fromName(String value) {
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw invalid();
        }
    }

    public static WorldParticipantType fromCode(Integer value) {
        for (WorldParticipantType type : values()) {
            if (value != null && type.code == value) return type;
        }
        throw invalid();
    }

    private static BusinessException invalid() {
        return new BusinessException(ResultCode.PARAM_ERROR.getCode(), "participantType仅支持AI或USER");
    }
}
