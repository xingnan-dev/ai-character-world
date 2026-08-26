package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.dto.response.WorldEventResponse;
import com.companion.dto.response.WorldRoundResponse;
import com.companion.entity.CharacterWorld;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldRound;
import com.companion.entity.enums.WorldEventStatus;
import com.companion.entity.enums.WorldEventType;
import com.companion.entity.enums.WorldRoundStatus;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.WorldEventMapper;
import com.companion.mapper.WorldRoundMapper;
import com.companion.service.WorldRoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorldRoundServiceImpl implements WorldRoundService {

    private static final int FIRST_SEQUENCE = 1;

    private final CharacterWorldMapper worldMapper;
    private final WorldRoundMapper roundMapper;
    private final WorldEventMapper eventMapper;

    @Override
    @Transactional
    public WorldRoundResponse create(Long userId, Long worldId, WorldRoundCreateRequest request) {
        requireOwnedWorld(userId, worldId);
        String requestId = normalizeRequired(request.getRequestId(), 64, "requestId不能为空", "requestId不能超过64字符");
        String userInput = normalizeRequired(request.getUserInput(), 4000, "用户输入不能为空", "用户输入不能超过4000字符");

        WorldRound existing = findByRequestId(worldId, requestId);
        if (existing != null) {
            return resolveIdempotent(existing, userInput);
        }

        LocalDateTime now = LocalDateTime.now();
        WorldRound round = new WorldRound();
        round.setWorldId(worldId);
        round.setRequestId(requestId);
        round.setUserInput(userInput);
        round.setStatus(WorldRoundStatus.PENDING.name());
        round.setCreateTime(now);
        round.setUpdateTime(now);

        try {
            roundMapper.insert(round);
        } catch (DuplicateKeyException duplicate) {
            WorldRound concurrent = findByRequestId(worldId, requestId);
            if (concurrent != null) {
                return resolveIdempotent(concurrent, userInput);
            }
            throw duplicate;
        }

        WorldEvent event = new WorldEvent();
        event.setRoundId(round.getId());
        event.setSequenceNo(FIRST_SEQUENCE);
        event.setParticipantId(null);
        event.setEventType(WorldEventType.USER_MESSAGE.name());
        event.setContent(userInput);
        event.setStatus(WorldEventStatus.COMPLETED.name());
        event.setCompletionTime(now);
        event.setCreateTime(now);
        eventMapper.insert(event);
        return toRoundResponse(round);
    }

    @Override
    public WorldRoundResponse get(Long userId, Long worldId, Long roundId) {
        requireOwnedWorld(userId, worldId);
        return toRoundResponse(requireRound(worldId, roundId));
    }

    @Override
    public List<WorldEventResponse> getEvents(Long userId, Long worldId, Long roundId) {
        requireOwnedWorld(userId, worldId);
        requireRound(worldId, roundId);
        return eventMapper.selectList(new LambdaQueryWrapper<WorldEvent>()
                        .eq(WorldEvent::getRoundId, roundId)
                        .orderByAsc(WorldEvent::getSequenceNo)
                        .orderByAsc(WorldEvent::getId))
                .stream().map(this::toEventResponse).toList();
    }

    @Override
    public boolean tryStart(Long userId, Long worldId, Long roundId) {
        requireOwnedWorld(userId, worldId);
        requireRound(worldId, roundId);
        LocalDateTime now = LocalDateTime.now();
        int updated = roundMapper.update(null, new LambdaUpdateWrapper<WorldRound>()
                .eq(WorldRound::getId, roundId)
                .eq(WorldRound::getWorldId, worldId)
                .eq(WorldRound::getStatus, WorldRoundStatus.PENDING.name())
                .set(WorldRound::getStatus, WorldRoundStatus.RUNNING.name())
                .set(WorldRound::getStartedTime, now)
                .set(WorldRound::getUpdateTime, now));
        return updated == 1;
    }

    private CharacterWorld requireOwnedWorld(Long userId, Long worldId) {
        CharacterWorld world = worldMapper.selectOne(new LambdaQueryWrapper<CharacterWorld>()
                .eq(CharacterWorld::getId, worldId)
                .eq(CharacterWorld::getOwnerUserId, userId)
                .eq(CharacterWorld::getStatus, 1));
        if (world == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return world;
    }

    private WorldRound requireRound(Long worldId, Long roundId) {
        WorldRound round = roundMapper.selectOne(new LambdaQueryWrapper<WorldRound>()
                .eq(WorldRound::getId, roundId)
                .eq(WorldRound::getWorldId, worldId));
        if (round == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return round;
    }

    private WorldRound findByRequestId(Long worldId, String requestId) {
        return roundMapper.selectOne(new LambdaQueryWrapper<WorldRound>()
                .eq(WorldRound::getWorldId, worldId)
                .eq(WorldRound::getRequestId, requestId));
    }

    private WorldRoundResponse resolveIdempotent(WorldRound existing, String userInput) {
        if (!existing.getUserInput().equals(userInput)) {
            throw new BusinessException(409, "requestId已用于其他世界输入");
        }
        return toRoundResponse(existing);
    }

    private String normalizeRequired(String value, int maxLength, String emptyMessage, String lengthMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), emptyMessage);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), lengthMessage);
        }
        return normalized;
    }

    private WorldRoundResponse toRoundResponse(WorldRound round) {
        WorldRoundResponse response = new WorldRoundResponse();
        response.setId(round.getId());
        response.setWorldId(round.getWorldId());
        response.setRequestId(round.getRequestId());
        response.setUserInput(round.getUserInput());
        response.setStatus(round.getStatus());
        response.setErrorCode(round.getErrorCode());
        response.setStartedTime(round.getStartedTime());
        response.setCompletionTime(round.getCompletionTime());
        response.setCreateTime(round.getCreateTime());
        response.setUpdateTime(round.getUpdateTime());
        return response;
    }

    private WorldEventResponse toEventResponse(WorldEvent event) {
        WorldEventResponse response = new WorldEventResponse();
        response.setId(event.getId());
        response.setRoundId(event.getRoundId());
        response.setSequenceNo(event.getSequenceNo());
        response.setParticipantId(event.getParticipantId());
        response.setEventType(event.getEventType());
        response.setContent(event.getContent());
        response.setStatus(event.getStatus());
        response.setErrorCode(event.getErrorCode());
        response.setCompletionTime(event.getCompletionTime());
        response.setCreateTime(event.getCreateTime());
        return response;
    }
}
