package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.character.snapshot.CharacterSnapshotJsonMapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.WorldCreateRequest;
import com.companion.dto.request.WorldParticipantCreateRequest;
import com.companion.dto.response.CharacterResponse;
import com.companion.dto.response.WorldParticipantResponse;
import com.companion.dto.response.WorldResponse;
import com.companion.entity.CharacterWorld;
import com.companion.entity.WorldParticipant;
import com.companion.entity.enums.WorldParticipantType;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.WorldParticipantMapper;
import com.companion.service.CharacterService;
import com.companion.service.CharacterWorldService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CharacterWorldServiceImpl implements CharacterWorldService {

    private final CharacterWorldMapper worldMapper;
    private final WorldParticipantMapper participantMapper;
    private final CharacterService characterService;
    private final CharacterSnapshotJsonMapper snapshotJsonMapper;

    @Override
    @Transactional
    public WorldResponse create(Long userId, WorldCreateRequest request) {
        validateParticipants(request.getParticipants());
        LocalDateTime now = LocalDateTime.now();

        CharacterWorld world = new CharacterWorld();
        world.setOwnerUserId(userId);
        world.setName(request.getName().trim());
        world.setBackground(normalize(request.getBackground()));
        world.setRules(normalize(request.getRules()));
        world.setStatus(1);
        world.setCreateTime(now);
        world.setUpdateTime(now);
        worldMapper.insert(world);

        try {
            for (WorldParticipantCreateRequest participantRequest : request.getParticipants()) {
                CharacterResponse character = characterService.get(userId, participantRequest.getCharacterId());
                CharacterSnapshot snapshot = CharacterSnapshot.from(character);

                WorldParticipant participant = new WorldParticipant();
                participant.setWorldId(world.getId());
                participant.setParticipantType(
                        WorldParticipantType.fromName(participantRequest.getParticipantType()).getCode()
                );
                participant.setSourceCharacterId(character.getId());
                participant.setCharacterSnapshot(snapshotJsonMapper.write(snapshot));
                participant.setDisplayOrder(participantRequest.getDisplayOrder());
                participant.setCreateTime(now);
                participantMapper.insert(participant);
            }
        } catch (DataIntegrityViolationException error) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "参与者存在重复或顺序冲突");
        }
        return get(world.getOwnerUserId(), world.getId());
    }

    @Override
    public WorldResponse get(Long userId, Long worldId) {
        CharacterWorld world = worldMapper.selectOne(
                new QueryWrapper<CharacterWorld>()
                        .eq("id", worldId)
                        .eq("owner_user_id", userId)
                        .eq("status", 1)
        );
        if (world == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        List<WorldParticipantResponse> participants = participantMapper.selectList(
                        new QueryWrapper<WorldParticipant>()
                                .eq("world_id", worldId)
                                .orderByAsc("display_order")
                                .orderByAsc("id")
                ).stream()
                .map(this::toParticipantResponse)
                .toList();

        WorldResponse response = new WorldResponse();
        response.setId(world.getId());
        response.setName(world.getName());
        response.setBackground(world.getBackground());
        response.setRules(world.getRules());
        response.setStatus(world.getStatus());
        response.setParticipants(participants);
        response.setCreateTime(world.getCreateTime());
        response.setUpdateTime(world.getUpdateTime());
        return response;
    }

    private WorldParticipantResponse toParticipantResponse(WorldParticipant participant) {
        try {
            WorldParticipantResponse response = new WorldParticipantResponse();
            response.setId(participant.getId());
            response.setParticipantType(
                    WorldParticipantType.fromCode(participant.getParticipantType()).name()
            );
            response.setSourceCharacterId(participant.getSourceCharacterId());
            response.setDisplayOrder(participant.getDisplayOrder());
            response.setCharacter(snapshotJsonMapper.read(participant.getCharacterSnapshot()));
            response.setCreateTime(participant.getCreateTime());
            return response;
        } catch (RuntimeException error) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "角色快照数据无效");
        }
    }

    private void validateParticipants(List<WorldParticipantCreateRequest> participants) {
        Set<Long> characterIds = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();
        for (WorldParticipantCreateRequest participant : participants) {
            WorldParticipantType.fromName(participant.getParticipantType());
            if (!characterIds.add(participant.getCharacterId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "同一角色不能重复加入世界");
            }
            if (!displayOrders.add(participant.getDisplayOrder())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "参与者顺序不能重复");
            }
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
