package com.companion.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.common.utils.RedisUtils;
import com.companion.entity.UserMemory;
import com.companion.mapper.UserMemoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MemoryEngine {

    private final UserMemoryMapper userMemoryMapper;

    @Autowired(required = false)
    private RedisUtils redisUtils;

    public MemoryEngine(UserMemoryMapper userMemoryMapper) {
        this.userMemoryMapper = userMemoryMapper;
    }

    private static final Pattern NAME_PATTERN = Pattern.compile("我叫([^，。,\\s！!？?]+)");
    private static final Pattern NAME_PATTERN_2 = Pattern.compile("我的名字是([^，。,\\s！!？?]+)");
    private static final Pattern LIKE_PATTERN = Pattern.compile("我(?:很|特别|比较)?(?:喜欢|爱|痴迷于|热衷于)([^，。,\\s！!？?]+)");
    private static final Pattern LIKE_DO_PATTERN = Pattern.compile("我(?:平时|通常|经常)?(?:喜欢|爱)(?:做|玩|听|看|吃|喝)?([^，。,\\s！!？?]+)");
    private static final Pattern AGE_PATTERN = Pattern.compile("我今年(\\d{1,3})岁");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("我(?:来自|住在|在)([^，。,\\s！!？?]+)");
    private static final Pattern JOB_PATTERN = Pattern.compile("我是(?:一名|一个)?([^，。,\\s！!？?]+?)(?:工作|上班|的|人)");

    @Transactional
    public void extractMemory(String userMessage, String aiResponse, Long userId) {
        if (userMessage == null || userMessage.isEmpty() || userId == null) {
            return;
        }

        try {
            extractAndSave(userMessage, userId, NAME_PATTERN, "名字", 0.9f);
            extractAndSave(userMessage, userId, NAME_PATTERN_2, "名字", 0.9f);
            extractAndSave(userMessage, userId, LIKE_PATTERN, "喜好", 0.7f);
            extractAndSave(userMessage, userId, LIKE_DO_PATTERN, "喜好", 0.7f);
            extractAndSave(userMessage, userId, AGE_PATTERN, "年龄", 0.8f);
            extractAndSave(userMessage, userId, LOCATION_PATTERN, "地域", 0.6f);
            extractAndSave(userMessage, userId, JOB_PATTERN, "职业", 0.6f);
        } catch (Exception e) {
            log.error("Memory extraction failed for userId={}", userId, e);
        }
    }

    public String getMemoryContext(Long userId) {
        if (userId == null) {
            return "";
        }

        try {
            String redisKey = "user:memory:context:" + userId;
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

            StringBuilder sb = new StringBuilder();
            for (UserMemory m : memories) {
                if (m.getMemoryKey() != null && m.getValue() != null) {
                    sb.append("- ").append(m.getMemoryKey()).append("：").append(m.getValue()).append("\n");
                }
            }

            String context = sb.toString().trim();
            if (redisUtils != null) {
                redisUtils.setWithExpire(redisKey, context, 30L * 24 * 60 * 60);
            }
            return context;
        } catch (Exception e) {
            log.error("Failed to get memory context for userId={}", userId, e);
            return "";
        }
    }

    @Transactional
    public void updateMemory(Long userId, String key, String value, Float importance) {
        if (userId == null || key == null || key.isEmpty()) {
            return;
        }

        try {
            UserMemory existing = userMemoryMapper.selectOne(
                    new QueryWrapper<UserMemory>()
                            .eq("user_id", userId)
                            .eq("memory_key", key)
                            .eq("status", 1)
                            .last("LIMIT 1")
            );

            if (existing != null) {
                existing.setValue(value);
                existing.setImportance(importance);
                existing.setLastAccessTime(LocalDateTime.now());
                existing.setUpdateTime(LocalDateTime.now());
                userMemoryMapper.updateById(existing);
            } else {
                UserMemory memory = new UserMemory();
                memory.setUserId(userId);
                memory.setMemoryKey(key);
                memory.setValue(value);
                memory.setImportance(importance != null ? importance : 0.5f);
                memory.setLastAccessTime(LocalDateTime.now());
                memory.setCreateTime(LocalDateTime.now());
                memory.setUpdateTime(LocalDateTime.now());
                memory.setStatus(1);
                userMemoryMapper.insert(memory);
            }

            // 清除缓存
            if (redisUtils != null) {
                String redisKey = "user:memory:context:" + userId;
                redisUtils.delete(redisKey);
            }
        } catch (Exception e) {
            log.error("Failed to update memory for userId={}, key={}", userId, key, e);
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

            if (redisUtils != null) {
                String redisKey = "user:memory:context:" + userId;
                redisUtils.delete(redisKey);
            }
        } catch (Exception e) {
            log.error("Failed to decay memories for userId={}", userId, e);
        }
    }

    private void extractAndSave(String text, Long userId, Pattern pattern, String categoryKey, Float importance) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            if (!value.isEmpty()) {
                String key = categoryKey + ":" + value;
                updateMemory(userId, key, value, importance);
            }
        }
    }
}
