package com.companion.ai.exception;

public enum LlmErrorType {
    CONFIGURATION,
    AUTHENTICATION,
    RATE_LIMIT,
    TIMEOUT,
    NETWORK,
    UPSTREAM_ERROR,
    INVALID_RESPONSE,
    CANCELLED
}
