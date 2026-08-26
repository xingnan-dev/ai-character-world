package com.companion.entity.enums;

import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;

import java.util.Arrays;

public enum CharacterVisualType {
    INITIAL(0),
    IMAGE(1),
    VRM(2);

    private final int code;

    CharacterVisualType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CharacterVisualType fromName(String value) {
        if (value == null || value.isBlank()) return INITIAL;
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.PARAM_ERROR.getCode(), "visualType只能是INITIAL、IMAGE或VRM"
                ));
    }

    public static CharacterVisualType fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(type -> type.code == (code == null ? -1 : code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown character visual type code"));
    }
}
