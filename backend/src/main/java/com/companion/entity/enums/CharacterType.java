package com.companion.entity.enums;

import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;

import java.util.Arrays;

public enum CharacterType {
    AI(1),
    USER(2);

    private final int code;

    CharacterType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CharacterType fromName(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "characterType只能是AI或USER");
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.PARAM_ERROR.getCode(), "characterType只能是AI或USER"
                ));
    }

    public static CharacterType fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(type -> type.code == (code == null ? -1 : code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown character type code"));
    }
}
