package com.companion.ai.exception;

public enum LlmErrorType {
    CONFIGURATION,
    INVALID_REQUEST,
    AUTHENTICATION,
    RATE_LIMIT,
    TIMEOUT,
    NETWORK,
    UPSTREAM_ERROR,
    INVALID_RESPONSE,
    CANCELLED
}
