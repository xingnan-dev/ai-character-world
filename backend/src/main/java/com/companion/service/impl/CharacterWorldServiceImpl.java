package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.character.snapshot.CharacterSnapshotJsonMapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.WorldCreateRequest;
import com.companion.dto.request.WorldParticipantCreateRequest;
import com.companion.dto.request.WorldParticipantReplaceRequest;
import com.companion.dto.request.WorldUpdateRequest;
import com.companion.dto.response.CharacterResponse;
import com.companion.dto.response.WorldParticipantResponse;
import com.companion.dto.response.WorldResponse;
import com.companion.entity.CharacterWorld;
import com.companion.entity.WorldParticipant;
import com.companion.entity.enums.WorldParticipantType;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.WorldParticipantMapper;
import com.companion.mapper.WorldRoundMapper;
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
    private final WorldRoundMapper roundMapper;
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
        world.setAtmosphere(normalize(request.getAtmosphere()));
        world.setScene(normalize(request.getScene()));
        world.setSourceDescription(normalize(request.getSourceDescription()));
        world.setStatus(1);
        world.setCreateTime(now);
        world.setUpdateTime(now);
        worldMapper.insert(world);

        try {
            insertParticipants(userId, world.getId(), request.getParticipants(), now);
        } catch (DataIntegrityViolationException error) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "参与者存在重复或顺序冲突");
        }
        return get(world.getOwnerUserId(), world.getId());
    }

    @Override
    public List<WorldResponse> list(Long userId) {
        return worldMapper.selectList(new QueryWrapper<CharacterWorld>()
                        .eq("owner_user_id", userId)
                        .eq("status", 1)
                        .orderByDesc("update_time")
                        .orderByDesc("id"))
                .stream().map(world -> toResponse(world, participants(world.getId()))).toList();
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

        return toResponse(world, participants(worldId));
    }

    @Override
    @Transactional
    public WorldResponse update(Long userId, Long worldId, WorldUpdateRequest request) {
        CharacterWorld world = requireOwnedWorldForUpdate(userId, worldId);
        world.setName(request.getName().trim());
        world.setBackground(normalize(request.getBackground()));
        world.setRules(normalize(request.getRules()));
        world.setAtmosphere(normalize(request.getAtmosphere()));
        world.setScene(normalize(request.getScene()));
        world.setSourceDescription(normalize(request.getSourceDescription()));
        world.setUpdateTime(LocalDateTime.now());
        worldMapper.updateById(world);
        return get(userId, worldId);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long worldId) {
        CharacterWorld world = requireOwnedWorld(userId, worldId);
        worldMapper.deleteById(world.getId());
    }

    @Override
    @Transactional
    public WorldResponse replaceParticipants(Long userId, Long worldId, WorldParticipantReplaceRequest request) {
        CharacterWorld world = requireOwnedWorldForUpdate(userId, worldId);
        if (participantsLocked(worldId)) {
            throw new BusinessException(409, "WORLD_PARTICIPANTS_LOCKED");
        }
        validateParticipants(request.getParticipants());
        List<PreparedParticipant> prepared = prepareParticipants(userId, request.getParticipants());
        participantMapper.deletePhysicallyByWorldId(worldId);
        LocalDateTime now = LocalDateTime.now();
        try {
            for (int index = 0; index < prepared.size(); index++) {
                insertParticipant(worldId, prepared.get(index), index, now);
            }
        } catch (DataIntegrityViolationException error) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "参与者存在重复或顺序冲突");
        }
        world.setUpdateTime(now);
        worldMapper.updateById(world);
        return get(userId, worldId);
    }

    private WorldResponse toResponse(CharacterWorld world, List<WorldParticipantResponse> participants) {
        WorldResponse response = new WorldResponse();
        response.setId(world.getId());
        response.setName(world.getName());
        response.setBackground(world.getBackground());
        response.setRules(world.getRules());
        response.setAtmosphere(world.getAtmosphere());
        response.setScene(world.getScene());
        response.setSourceDescription(world.getSourceDescription());
        response.setStatus(world.getStatus());
        response.setParticipants(participants);
        response.setParticipantsLocked(participantsLocked(world.getId()));
        response.setCreateTime(world.getCreateTime());
        response.setUpdateTime(world.getUpdateTime());
        return response;
    }

    private CharacterWorld requireOwnedWorld(Long userId, Long worldId) {
        CharacterWorld world = worldMapper.selectOne(new QueryWrapper<CharacterWorld>()
                .eq("id", worldId).eq("owner_user_id", userId).eq("status", 1));
        if (world == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return world;
    }

    private CharacterWorld requireOwnedWorldForUpdate(Long userId, Long worldId) {
        CharacterWorld world = worldMapper.selectOwnedForUpdate(userId, worldId);
        if (world == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return world;
    }

    private List<WorldParticipantResponse> participants(Long worldId) {
        return participantMapper.selectList(new QueryWrapper<WorldParticipant>()
                        .eq("world_id", worldId)
                        .orderByAsc("display_order").orderByAsc("id"))
                .stream().map(this::toParticipantResponse).toList();
    }

    private boolean participantsLocked(Long worldId) {
        return roundMapper.selectCount(new QueryWrapper<com.companion.entity.WorldRound>()
                .eq("world_id", worldId)) > 0;
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
        if (participants == null || participants.size() < 2 || participants.size() > 4) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "一个世界必须选择2到4个AI角色");
        }
        Set<Long> characterIds = new HashSet<>();
        Set<Integer> displayOrders = new HashSet<>();
        for (WorldParticipantCreateRequest participant : participants) {
            if (WorldParticipantType.fromName(participant.getParticipantType()) != WorldParticipantType.AI) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "World Participant必须是AI角色");
            }
            if (!characterIds.add(participant.getCharacterId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "同一角色不能重复加入世界");
            }
            if (!displayOrders.add(participant.getDisplayOrder())) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "参与者顺序不能重复");
            }
        }
    }

    private void insertParticipants(Long userId, Long worldId,
                                    List<WorldParticipantCreateRequest> requests, LocalDateTime now) {
        List<PreparedParticipant> prepared = prepareParticipants(userId, requests);
        for (int index = 0; index < prepared.size(); index++) {
            insertParticipant(worldId, prepared.get(index), index, now);
        }
    }

    private List<PreparedParticipant> prepareParticipants(
            Long userId, List<WorldParticipantCreateRequest> requests) {
        return requests.stream()
                .sorted(java.util.Comparator.comparing(WorldParticipantCreateRequest::getDisplayOrder))
                .map(request -> prepareParticipant(userId, request))
                .toList();
    }

    private PreparedParticipant prepareParticipant(Long userId, WorldParticipantCreateRequest request) {
        CharacterResponse character = characterService.get(userId, request.getCharacterId());
        if (!"AI".equals(character.getCharacterType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "World Participant必须绑定AI Character");
        }
        return new PreparedParticipant(CharacterSnapshot.from(character));
    }

    private void insertParticipant(Long worldId, PreparedParticipant prepared,
                                   int displayOrder, LocalDateTime now) {
        CharacterSnapshot snapshot = prepared.snapshot();
        WorldParticipant participant = new WorldParticipant();
        participant.setWorldId(worldId);
        participant.setParticipantType(WorldParticipantType.AI.getCode());
        participant.setSourceCharacterId(snapshot.sourceCharacterId());
        participant.setCharacterSnapshot(snapshotJsonMapper.write(snapshot));
        participant.setDisplayOrder(displayOrder);
        participant.setCreateTime(now);
        participantMapper.insert(participant);
    }

    private record PreparedParticipant(CharacterSnapshot snapshot) {}

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
