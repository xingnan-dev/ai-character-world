package com.companion.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.memory.ExtractedMemory;
import com.companion.ai.memory.RuleBasedMemoryExtractor;
import com.companion.common.utils.RedisUtils;
import com.companion.entity.UserMemory;
import com.companion.mapper.UserMemoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class MemoryEngine {

    private final UserMemoryMapper userMemoryMapper;
    private final RuleBasedMemoryExtractor memoryExtractor;

    @Autowired(required = false)
    private RedisUtils redisUtils;

    public MemoryEngine(UserMemoryMapper userMemoryMapper, RuleBasedMemoryExtractor memoryExtractor) {
        this.userMemoryMapper = userMemoryMapper;
        this.memoryExtractor = memoryExtractor;
    }

    @Transactional
    public void extractMemory(String userMessage, String aiResponse, Long userId) {
        if (userMessage == null || userMessage.isBlank() || userId == null) {
            return;
        }

        try {
            for (ExtractedMemory memory : memoryExtractor.extract(userMessage)) {
                saveExtractedMemory(userId, memory);
            }
            evictContextCache(userId);
        } catch (Exception error) {
            log.error("Memory extraction failed for userId={}", userId, error);
        }
    }

    public String getMemoryContext(Long userId) {
        if (userId == null) {
            return "";
        }

        try {
            String redisKey = contextCacheKey(userId);
            if (redisUtils != null) {
                Object cached = redisUtils.get(redisKey);
                if (cached != null) {
                    return cached.toString();
                }
            }

            List<UserMemory> memories = userMemoryMapper.selectList(
                    new QueryWrapper<UserMemory>()
                            .eq("user_id", userId)
                            .eq("status", 1)
                            .orderByDesc("importance")
                            .last("LIMIT 20")
            );
            if (memories == null || memories.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder();
            for (UserMemory memory : memories) {
                String value = normalizeValue(memory.getValue());
                if (memory.getMemoryKey() != null && !memory.getMemoryKey().isBlank() && !value.isEmpty()) {
                    context.append("- ")
                            .append(memory.getMemoryKey().trim())
                            .append(": ")
                            .append(value)
                            .append('\n');
                }
            }

            String result = context.toString().trim();
            if (redisUtils != null) {
                redisUtils.setWithExpire(redisKey, result, 30L * 24 * 60 * 60);
            }
            return result;
        } catch (Exception error) {
            log.error("Failed to get memory context for userId={}", userId, error);
            return "";
        }
    }

    @Transactional
    public void updateMemory(Long userId, String key, String value, Float importance) {
        if (userId == null || key == null || key.isBlank()) {
            return;
        }

        try {
            saveExtractedMemory(userId, new ExtractedMemory(
                    key.trim(), normalizeValue(value), 1,
                    importance == null ? 0.5f : importance, true
            ));
            evictContextCache(userId);
        } catch (Exception error) {
            log.error("Failed to update memory for userId={}, key={}", userId, key, error);
        }
    }

    @Transactional
    public void decayMemories(Long userId) {
        if (userId == null) {
            return;
        }

        try {
            int affected = userMemoryMapper.delete(
                    new QueryWrapper<UserMemory>()
                            .eq("user_id", userId)
                            .lt("importance", 0.3f)
                            .eq("status", 1)
            );
            log.info("Decayed {} low-importance memories for userId={}", affected, userId);
            evictContextCache(userId);
        } catch (Exception error) {
            log.error("Failed to decay memories for userId={}", userId, error);
        }
    }

    private void saveExtractedMemory(Long userId, ExtractedMemory extracted) {
        String value = normalizeValue(extracted.value());
        if (value.isEmpty()) {
            return;
        }

        QueryWrapper<UserMemory> query = new QueryWrapper<UserMemory>()
                .eq("user_id", userId)
                .eq("memory_key", extracted.memoryKey())
                .eq("status", 1);
        if (!extracted.singleValued()) {
            query.eq("value", value);
        }

        UserMemory existing = userMemoryMapper.selectOne(query.last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setValue(value);
            existing.setCategory(extracted.category());
            existing.setImportance(extracted.importance());
            existing.setLastAccessTime(now);
            existing.setUpdateTime(now);
            userMemoryMapper.updateById(existing);
            return;
        }

        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setCategory(extracted.category());
        memory.setMemoryKey(extracted.memoryKey());
        memory.setValue(value);
        memory.setImportance(extracted.importance());
        memory.setLastAccessTime(now);
        memory.setCreateTime(now);
        memory.setUpdateTime(now);
        memory.setStatus(1);
        userMemoryMapper.insert(memory);
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }

    private void evictContextCache(Long userId) {
        if (redisUtils != null) {
            redisUtils.delete(contextCacheKey(userId));
        }
    }

    private String contextCacheKey(Long userId) {
        return "user:memory:context:" + userId;
    }
}
