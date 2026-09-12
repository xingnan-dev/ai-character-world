package com.companion.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.memory.ExtractedMemory;
import com.companion.ai.memory.MemoryRetrievalScorer;
import com.companion.ai.memory.RuleBasedMemoryExtractor;
import com.companion.ai.memory.LlmMemoryExtractor;
import com.companion.ai.memory.ScoredMemory;
import com.companion.ai.config.LlmProperties;
import com.companion.common.utils.RedisUtils;
import com.companion.entity.UserMemory;
import com.companion.mapper.UserMemoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class MemoryEngine {

    private final UserMemoryMapper userMemoryMapper;
    private final RuleBasedMemoryExtractor memoryExtractor;
    @Autowired(required = false)
    private LlmMemoryExtractor llmMemoryExtractor;
    private final MemoryRetrievalScorer retrievalScorer;
    private final LlmProperties llmProperties;

    @Autowired(required = false)
    private RedisUtils redisUtils;

    public MemoryEngine(UserMemoryMapper userMemoryMapper,
                        RuleBasedMemoryExtractor memoryExtractor,
                        MemoryRetrievalScorer retrievalScorer,
                        LlmProperties llmProperties) {
        this.userMemoryMapper = userMemoryMapper;
        this.memoryExtractor = memoryExtractor;
        this.retrievalScorer = retrievalScorer;
        this.llmProperties = llmProperties;
    }

    @Transactional
    public void extractMemory(String userMessage, String aiResponse, Long userId) {
        extractMemory(userMessage, aiResponse, userId, null);
    }

    @Transactional
    public void extractMemory(String userMessage, String aiResponse, Long userId, Long characterId) {
        if (userMessage == null || userMessage.isBlank() || userId == null) {
            return;
        }

        try {
            List<ExtractedMemory> extracted = llmMemoryExtractor == null ? List.of() : llmMemoryExtractor.extract(userMessage, aiResponse);
            if (extracted.isEmpty()) extracted = memoryExtractor.extract(userMessage);
            for (ExtractedMemory memory : extracted) {
                saveExtractedMemory(userId, characterId, memory);
            }
            evictContextCache(userId);
        } catch (Exception error) {
            log.error("Memory extraction failed for userId={}", userId, error);
        }
    }

    public String getMemoryContext(Long userId, String currentMessage) {
        return getMemoryContext(userId, null, currentMessage);
    }

    public String getMemoryContext(Long userId, Long characterId, String currentMessage) {
        if (userId == null) {
            return "";
        }

        try {
            QueryWrapper<UserMemory> scope = new QueryWrapper<UserMemory>()
                            .eq("user_id", userId)
                            .and(w -> {
                                w.isNull("character_id");
                                if (characterId != null) w.or().eq("character_id", characterId);
                            })
                            .eq("status", 1)
                            .orderByDesc("importance")
                            .orderByDesc("last_access_time")
                            .orderByDesc("id")
                            .last("LIMIT " + candidateLimit());
            List<UserMemory> memories = userMemoryMapper.selectList(scope);
            if (memories == null || memories.isEmpty()) {
                return "";
            }

            List<UserMemory> selectedMemories = memories.stream()
                    .map(memory -> retrievalScorer.score(memory, currentMessage))
                    .sorted(scoredMemoryComparator(characterId))
                    .limit(retrievalLimit())
                    .map(ScoredMemory::memory)
                    .toList();

            StringBuilder context = new StringBuilder();
            for (UserMemory memory : selectedMemories) {
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
        saveExtractedMemory(userId, null, extracted);
    }

    private void saveExtractedMemory(Long userId, Long characterId, ExtractedMemory extracted) {
        String value = normalizeValue(extracted.value());
        if (value.isEmpty()) {
            return;
        }

        QueryWrapper<UserMemory> query = new QueryWrapper<UserMemory>()
                .eq("user_id", userId)
                .eq("memory_key", extracted.memoryKey())
                .eq("status", 1);
        if (characterId == null) query.isNull("character_id"); else query.eq("character_id", characterId);
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
        memory.setCharacterId(characterId);
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

    private Comparator<ScoredMemory> scoredMemoryComparator(Long characterId) {
        return Comparator.comparingDouble(ScoredMemory::totalScore).reversed()
                .thenComparingInt(scored -> characterId != null && characterId.equals(scored.memory().getCharacterId()) ? 0 : 1)
                .thenComparing(
                        scored -> scored.memory().getImportance() == null ? 0.0f : scored.memory().getImportance(),
                        Comparator.reverseOrder()
                )
                .thenComparing(
                        scored -> scored.memory().getLastAccessTime() == null
                                ? LocalDateTime.MIN : scored.memory().getLastAccessTime(),
                        Comparator.reverseOrder()
                )
                .thenComparing(
                        scored -> scored.memory().getId() == null ? Long.MIN_VALUE : scored.memory().getId(),
                        Comparator.reverseOrder()
                );
    }

    private int candidateLimit() {
        return Math.max(1, llmProperties.getMemory().getCandidateLimit());
    }

    private int retrievalLimit() {
        return Math.max(1, Math.min(
                llmProperties.getMemory().getRetrievalLimit(), candidateLimit()
        ));
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
