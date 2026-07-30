package com.companion.common.constants;

public class RedisConstants {

    public static final String TOKEN_PREFIX = "auth:token:";
    public static final Long TOKEN_EXPIRE = 60 * 60 * 24 * 7L;

    public static final String USER_MEMORY_PREFIX = "user:memory:";
    public static final Long USER_MEMORY_EXPIRE = 60 * 60 * 24 * 30L;
}