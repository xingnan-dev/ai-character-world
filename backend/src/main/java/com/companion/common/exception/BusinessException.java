package com.companion.common.exception;

import com.companion.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.SERVER_ERROR.getCode();
    }
}