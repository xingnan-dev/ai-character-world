package com.companion.world;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.character.snapshot.CharacterSnapshotJsonMapper;
import com.companion.entity.WorldParticipant;
import com.companion.entity.enums.WorldParticipantType;
import com.companion.mapper.WorldParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class DefaultWorldParticipantResolver implements WorldParticipantResolver {

    public static final String SNAPSHOT_INVALID = "WORLD_SNAPSHOT_INVALID";

    private final WorldParticipantMapper participantMapper;
    private final CharacterSnapshotJsonMapper snapshotJsonMapper;

    @Override
    public List<WorldActorContext> resolveAiParticipants(Long worldId) {
        return participantMapper.selectList(new LambdaQueryWrapper<WorldParticipant>()
                        .eq(WorldParticipant::getWorldId, worldId)
                        .eq(WorldParticipant::getParticipantType, WorldParticipantType.AI.getCode())
                        .orderByAsc(WorldParticipant::getDisplayOrder)
                        .orderByAsc(WorldParticipant::getId))
                .stream()
                .sorted(Comparator.comparing(WorldParticipant::getDisplayOrder)
                        .thenComparing(WorldParticipant::getId))
                .map(this::resolve).toList();
    }

    private WorldActorContext resolve(WorldParticipant participant) {
        try {
            CharacterSnapshot snapshot = snapshotJsonMapper.read(participant.getCharacterSnapshot());
            if (!"AI".equals(snapshot.characterType())
                    || !participant.getSourceCharacterId().equals(snapshot.sourceCharacterId())) {
                return invalid(participant);
            }
            return new WorldActorContext(participant.getId(), participant.getWorldId(),
                    participant.getDisplayOrder(), snapshot, null);
        } catch (RuntimeException error) {
            return invalid(participant);
        }
    }

    private WorldActorContext invalid(WorldParticipant participant) {
        return new WorldActorContext(participant.getId(), participant.getWorldId(),
                participant.getDisplayOrder(), null, SNAPSHOT_INVALID);
    }
}
