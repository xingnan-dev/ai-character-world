package com.companion.ai.exception;

public class LlmProviderException extends RuntimeException {

    private final String provider;
    private final LlmErrorType errorType;
    private final Integer httpStatus;
    private final boolean retryable;

    public LlmProviderException(String provider, LlmErrorType errorType, Integer httpStatus,
                                boolean retryable, String message) {
        this(provider, errorType, httpStatus, retryable, message, null);
    }

    public LlmProviderException(String provider, LlmErrorType errorType, Integer httpStatus,
                                boolean retryable, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorType = errorType;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
    }

    public String getProvider() {
        return provider;
    }

    public LlmErrorType getErrorType() {
        return errorType;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
