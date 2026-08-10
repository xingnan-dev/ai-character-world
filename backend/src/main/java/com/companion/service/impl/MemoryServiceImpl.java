package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.common.utils.RedisUtils;
import com.companion.dto.request.MemoryUpdateRequest;
import com.companion.dto.response.MemoryVO;
import com.companion.entity.UserMemory;
import com.companion.mapper.UserMemoryMapper;
import com.companion.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MemoryServiceImpl implements MemoryService {

    private final UserMemoryMapper userMemoryMapper;

    @Autowired(required = false)
    private RedisUtils redisUtils;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public MemoryServiceImpl(UserMemoryMapper userMemoryMapper) {
        this.userMemoryMapper = userMemoryMapper;
    }

    @Override
    public List<MemoryVO> getMemoryList(Long userId) {
        List<UserMemory> memories = userMemoryMapper.selectList(
                new QueryWrapper<UserMemory>()
                        .eq("user_id", userId)
                        .orderByDesc("importance")
                        .orderByDesc("last_access_time")
        );
        return memories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public MemoryVO updateMemory(Long userId, MemoryUpdateRequest request) {
        validateUpdate(request);
        UserMemory memory = userMemoryMapper.selectById(request.getId());
        if (memory == null || !memory.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (request.getValue() != null) {
            memory.setValue(request.getValue().trim());
        }
        if (request.getImportance() != null) {
            memory.setImportance(request.getImportance());
        }
        memory.setLastAccessTime(LocalDateTime.now());
        memory.setUpdateTime(LocalDateTime.now());
        userMemoryMapper.updateById(memory);

        cacheMemory(memory);

        return convertToVO(memory);
    }

    private void validateUpdate(MemoryUpdateRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (request.getValue() != null) {
            String value = request.getValue().trim();
            if (value.isEmpty() || value.length() > 2000) {
                throw new BusinessException(ResultCode.PARAM_ERROR);
            }
        }
        if (request.getImportance() != null
                && (!Float.isFinite(request.getImportance())
                || request.getImportance() < 0.0f
                || request.getImportance() > 1.0f)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
    }

    @Override
    public void deleteMemory(Long userId, Long memoryId) {
        UserMemory memory = userMemoryMapper.selectById(memoryId);
        if (memory == null || !memory.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        userMemoryMapper.deleteById(memoryId);
        evictMemoryCache(userId, memoryId);
    }

    @Override
    public void clearMemory(Long userId) {
        userMemoryMapper.delete(
                new QueryWrapper<UserMemory>().eq("user_id", userId)
        );
        evictAllMemoryCache(userId);
    }

    private void cacheMemory(UserMemory memory) {
        if (redisUtils != null) {
            String cacheKey = "user:memory:" + memory.getUserId() + ":" + memory.getId();
            redisUtils.set(cacheKey, memory, 30L * 24 * 60 * 60, TimeUnit.SECONDS);
        }
    }

    private void evictMemoryCache(Long userId, Long memoryId) {
        if (redisUtils != null) {
            String cacheKey = "user:memory:" + userId + ":" + memoryId;
            redisUtils.delete(cacheKey);
        }
    }

    private void evictAllMemoryCache(Long userId) {
        if (redisTemplate != null) {
            String pattern = "user:memory:" + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    private MemoryVO convertToVO(UserMemory memory) {
        MemoryVO vo = new MemoryVO();
        vo.setId(memory.getId());
        vo.setCategory(memory.getCategory());
        vo.setMemoryKey(memory.getMemoryKey());
        vo.setValue(memory.getValue());
        vo.setImportance(memory.getImportance());
        vo.setLastAccessTime(memory.getLastAccessTime() != null ? memory.getLastAccessTime().toString() : null);
        vo.setCreateTime(memory.getCreateTime() != null ? memory.getCreateTime().toString() : null);
        return vo;
    }
}
