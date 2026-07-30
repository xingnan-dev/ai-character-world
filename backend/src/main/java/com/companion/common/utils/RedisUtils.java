package com.companion.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 * 注意：仅在Redis可用时才加载
 */
@Service
@ConditionalOnBean(RedisTemplate.class)
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long time, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, time, timeUnit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public void setWithExpire(String key, Object value, long expireSeconds) {
        redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}
