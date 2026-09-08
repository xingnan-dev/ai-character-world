package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.common.exception.BusinessException;
import com.companion.config.ImageGenerationProperties;
import com.companion.dto.request.CharacterImageConfirmRequest;
import com.companion.dto.request.CharacterImageGenerateRequest;
import com.companion.dto.response.CharacterImageGenerationResponse;
import com.companion.entity.AiCharacter;
import com.companion.entity.CharacterImageGeneration;
import com.companion.entity.enums.CharacterVisualType;
import com.companion.image.GeneratedImageStorage;
import com.companion.image.ImageGenerationClient;
import com.companion.mapper.CharacterImageGenerationMapper;
import com.companion.mapper.CharacterMapper;
import com.companion.service.CharacterImageGenerationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
public class CharacterImageGenerationServiceImpl implements CharacterImageGenerationService {
    private final CharacterImageGenerationMapper generations;
    private final CharacterMapper characters;
    private final ImageGenerationProperties properties;
    private final ImageGenerationClient client;
    private final GeneratedImageStorage storage;
    private final ConcurrentHashMap<Long, Semaphore> locks = new ConcurrentHashMap<>();

    public CharacterImageGenerationServiceImpl(CharacterImageGenerationMapper generations,
                                                CharacterMapper characters,
                                                ImageGenerationProperties properties,
                                                ImageGenerationClient client,
                                                GeneratedImageStorage storage) {
        this.generations = generations;
        this.characters = characters;
        this.properties = properties;
        this.client = client;
        this.storage = storage;
    }

    @Override
    @Transactional
    public CharacterImageGenerationResponse generate(Long userId, CharacterImageGenerateRequest request) {
        String prompt = request.getPrompt().trim();
        String requestHash = hash(prompt);
        CharacterImageGeneration existing = findByRequest(userId, request.getRequestId());
        if (existing != null) {
            if (!requestHash.equals(existing.getRequestHash())) throw new BusinessException(400, "相同 requestId 携带了不同内容");
            return response(existing, null);
        }
        long usedToday = generations.selectCount(new QueryWrapper<CharacterImageGeneration>()
                .eq("user_id", userId).ge("created_at", LocalDate.now().atStartOfDay()));
        if (usedToday >= properties.getDailyLimit()) throw new BusinessException(400, "已达到今日图片生成次数限制");

        Semaphore lock = locks.computeIfAbsent(userId, ignored -> new Semaphore(properties.getPerUserConcurrency()));
        if (!lock.tryAcquire()) throw new BusinessException(429, "当前已有图片生成任务，请稍后重试");
        try {
            existing = findByRequest(userId, request.getRequestId());
            if (existing != null) {
                if (!requestHash.equals(existing.getRequestHash())) throw new BusinessException(400, "相同 requestId 携带了不同内容");
                return response(existing, null);
            }
            CharacterImageGeneration record = newRecord(userId, request, prompt, requestHash);
            generations.insert(record);
            try {
                Path saved = storage.download(client.generate(prompt));
                record.setImagePath(saved.toString());
                record.setImageUrl(normalizedPublicPath() + saved.getFileName());
                record.setStatus("SUCCEEDED");
                record.setUpdatedAt(LocalDateTime.now());
                generations.updateById(record);
                return response(record, null);
            } catch (Exception error) {
                record.setStatus("FAILED");
                record.setUpdatedAt(LocalDateTime.now());
                generations.updateById(record);
                return response(record, safeMessage(error));
            }
        } finally {
            lock.release();
        }
    }

    @Override
    @Transactional
    public void confirm(Long userId, CharacterImageConfirmRequest request) {
        CharacterImageGeneration generation = generations.selectOne(new QueryWrapper<CharacterImageGeneration>()
                .eq("id", request.getGenerationId()).eq("user_id", userId));
        if (generation == null || !"SUCCEEDED".equals(generation.getStatus()) || generation.getImageUrl() == null) {
            throw new BusinessException(400, "图片生成记录不可确认");
        }
        if (generation.getConfirmedAt() != null) {
            if (request.getCharacterId().equals(generation.getCharacterId())) return;
            throw new BusinessException(400, "图片生成记录已绑定其他角色");
        }
        AiCharacter character = characters.selectOne(new QueryWrapper<AiCharacter>()
                .eq("id", request.getCharacterId()).eq("user_id", userId).eq("status", 1));
        if (character == null) throw new BusinessException(404, "角色不存在");
        character.setVisualType(CharacterVisualType.IMAGE.getCode());
        character.setImageUrl(generation.getImageUrl());
        character.setUpdateTime(LocalDateTime.now());
        characters.updateById(character);
        generation.setCharacterId(character.getId());
        generation.setConfirmedAt(LocalDateTime.now());
        generation.setUpdatedAt(LocalDateTime.now());
        generations.updateById(generation);
    }

    private CharacterImageGeneration findByRequest(Long userId, String requestId) {
        return generations.selectOne(new QueryWrapper<CharacterImageGeneration>()
                .eq("user_id", userId).eq("request_id", requestId));
    }

    private CharacterImageGeneration newRecord(Long userId, CharacterImageGenerateRequest request,
                                                String prompt, String requestHash) {
        LocalDateTime now = LocalDateTime.now();
        CharacterImageGeneration record = new CharacterImageGeneration();
        record.setUserId(userId);
        record.setRequestId(request.getRequestId());
        record.setRequestHash(requestHash);
        record.setPrompt(prompt);
        record.setStatus("PROCESSING");
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private String normalizedPublicPath() {
        String path = properties.getPublicPath();
        return path.endsWith("/") ? path : path + "/";
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private String safeMessage(Exception error) {
        return error.getMessage() == null || error.getMessage().isBlank() ? "图片生成失败，请稍后重试" : error.getMessage();
    }

    private CharacterImageGenerationResponse response(CharacterImageGeneration generation, String error) {
        CharacterImageGenerationResponse response = new CharacterImageGenerationResponse();
        response.setId(generation.getId());
        response.setStatus(generation.getStatus());
        response.setImageUrl(generation.getImageUrl());
        response.setError(error);
        return response;
    }
}
