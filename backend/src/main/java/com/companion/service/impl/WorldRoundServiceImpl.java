package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.dto.response.WorldEventResponse;
import com.companion.dto.response.WorldRoundResponse;
import com.companion.dto.response.WorldTimelineItemResponse;
import com.companion.dto.response.WorldTimelinePageResponse;
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
import com.companion.world.WorldRoundOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorldRoundServiceImpl implements WorldRoundService {

    private static final int FIRST_SEQUENCE = 1;
    private static final int DEFAULT_TIMELINE_LIMIT = 20;
    private static final int MAX_TIMELINE_LIMIT = 50;

    private final CharacterWorldMapper worldMapper;
    private final WorldRoundMapper roundMapper;
    private final WorldEventMapper eventMapper;
    private final WorldRoundOrchestrator roundOrchestrator;
    private final Clock clock;

    @Override
    @Transactional
    public WorldRoundResponse create(Long userId, Long worldId, WorldRoundCreateRequest request) {
        if (worldMapper.selectOwnedForUpdate(userId, worldId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String requestId = normalizeRequired(request.getRequestId(), 64, "requestId不能为空", "requestId不能超过64字符");
        String userInput = normalizeRequired(request.getUserInput(), 4000, "用户输入不能为空", "用户输入不能超过4000字符");

        WorldRound existing = findByRequestId(worldId, requestId);
        if (existing != null) {
            return resolveIdempotent(existing, userInput);
        }

        WorldRound active = roundMapper.selectOne(new LambdaQueryWrapper<WorldRound>()
                .eq(WorldRound::getWorldId, worldId)
                .in(WorldRound::getStatus, WorldRoundStatus.PENDING.name(), WorldRoundStatus.RUNNING.name())
                .orderByDesc(WorldRound::getId)
                .last("LIMIT 1"));
        if (active != null) {
            throw new BusinessException(409, "WORLD_ROUND_ACTIVE");
        }

        LocalDateTime now = LocalDateTime.now(clock);
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
    public WorldRoundResponse getActive(Long userId, Long worldId) {
        requireOwnedWorld(userId, worldId);
        WorldRound active = roundMapper.selectOne(new LambdaQueryWrapper<WorldRound>()
                .eq(WorldRound::getWorldId, worldId)
                .in(WorldRound::getStatus, WorldRoundStatus.PENDING.name(), WorldRoundStatus.RUNNING.name())
                .orderByDesc(WorldRound::getId)
                .last("LIMIT 1"));
        return active == null ? null : toRoundResponse(active);
    }

    @Override
    public WorldTimelinePageResponse getTimeline(Long userId, Long worldId, Long beforeRoundId, Integer limit) {
        requireOwnedWorld(userId, worldId);
        int pageSize = validateTimelineLimit(limit);
        if (beforeRoundId != null && beforeRoundId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "beforeRoundId必须为正整数");
        }

        LambdaQueryWrapper<WorldRound> query = new LambdaQueryWrapper<WorldRound>()
                .eq(WorldRound::getWorldId, worldId)
                .orderByDesc(WorldRound::getId)
                .last("LIMIT " + (pageSize + 1));
        if (beforeRoundId != null) {
            query.lt(WorldRound::getId, beforeRoundId);
        }
        List<WorldRound> fetched = roundMapper.selectList(query);
        boolean hasMore = fetched.size() > pageSize;
        List<WorldRound> rounds = hasMore ? fetched.subList(0, pageSize) : fetched;
        if (rounds.isEmpty()) {
            return new WorldTimelinePageResponse(Collections.emptyList(), null, false);
        }

        List<Long> roundIds = rounds.stream().map(WorldRound::getId).toList();
        Map<Long, List<WorldEventResponse>> eventsByRound = new LinkedHashMap<>();
        for (Long roundId : roundIds) {
            eventsByRound.put(roundId, new ArrayList<>());
        }
        eventMapper.selectList(new LambdaQueryWrapper<WorldEvent>()
                        .in(WorldEvent::getRoundId, roundIds)
                        .orderByAsc(WorldEvent::getRoundId)
                        .orderByAsc(WorldEvent::getSequenceNo)
                        .orderByAsc(WorldEvent::getId))
                .forEach(event -> eventsByRound.get(event.getRoundId()).add(toEventResponse(event)));
        List<WorldTimelineItemResponse> items = rounds.stream()
                .map(round -> new WorldTimelineItemResponse(toRoundResponse(round), eventsByRound.get(round.getId())))
                .toList();
        Long nextBeforeRoundId = hasMore ? rounds.get(rounds.size() - 1).getId() : null;
        return new WorldTimelinePageResponse(items, nextBeforeRoundId, hasMore);
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
    public WorldRoundResponse execute(Long userId, Long worldId, Long roundId) {
        CharacterWorld world = requireOwnedWorld(userId, worldId);
        WorldRound round = requireRound(worldId, roundId);
        roundOrchestrator.execute(userId, world, round);
        return toRoundResponse(requireRound(worldId, roundId));
    }

    @Override
    public boolean tryStart(Long userId, Long worldId, Long roundId) {
        requireOwnedWorld(userId, worldId);
        requireRound(worldId, roundId);
        LocalDateTime now = LocalDateTime.now(clock);
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

    private int validateTimelineLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TIMELINE_LIMIT;
        }
        if (limit < 1 || limit > MAX_TIMELINE_LIMIT) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "limit必须在1到50之间");
        }
        return limit;
    }

    private WorldRoundResponse toRoundResponse(WorldRound round) {
        WorldRoundResponse response = new WorldRoundResponse();
        response.setId(round.getId());
        response.setWorldId(round.getWorldId());
        response.setRequestId(round.getRequestId());
        response.setUserInput(round.getUserInput());
        response.setStatus(round.getStatus());
        response.setErrorCode(round.getErrorCode());
        response.setExecutionRecoverable(isExecutionRecoverable(round));
        response.setStartedTime(round.getStartedTime());
        response.setCompletionTime(round.getCompletionTime());
        response.setCreateTime(round.getCreateTime());
        response.setUpdateTime(round.getUpdateTime());
        return response;
    }

    private boolean isExecutionRecoverable(WorldRound round) {
        if (WorldRoundStatus.PENDING.name().equals(round.getStatus())) {
            return true;
        }
        if (!WorldRoundStatus.RUNNING.name().equals(round.getStatus())) {
            return false;
        }
        return round.getLeaseUntil() == null || !round.getLeaseUntil().isAfter(LocalDateTime.now(clock));
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
