package com.companion.world;

import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.character.snapshot.CharacterSnapshotJsonMapper;
import com.companion.entity.WorldParticipant;
import com.companion.entity.enums.WorldParticipantType;
import com.companion.mapper.WorldParticipantMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldParticipantResolverTest {

    @Test
    void resolvesAndSortsOnlyFromStoredSnapshotsAndMarksCorruption() {
        WorldParticipantMapper mapper = mock(WorldParticipantMapper.class);
        CharacterSnapshotJsonMapper json = new CharacterSnapshotJsonMapper(new ObjectMapper());
        WorldParticipant later = participant(2L, 20, json.write(snapshot(22L, "后")), 22L);
        WorldParticipant earlier = participant(1L, 10, json.write(snapshot(11L, "前")), 11L);
        WorldParticipant corrupt = participant(3L, 30, "{broken", 33L);
        when(mapper.selectList(any())).thenReturn(List.of(later, corrupt, earlier));

        DefaultWorldParticipantResolver resolver = new DefaultWorldParticipantResolver(mapper, json);
        List<WorldActorContext> result = resolver.resolveAiParticipants(9L);

        assertThat(result).extracting(WorldActorContext::participantId).containsExactly(1L, 2L, 3L);
        assertThat(result.get(0).snapshot().name()).isEqualTo("前");
        assertThat(result.get(2).resolutionErrorCode()).isEqualTo("WORLD_SNAPSHOT_INVALID");
        assertThat(java.util.Arrays.stream(DefaultWorldParticipantResolver.class.getDeclaredFields())
                .filter(field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .map(java.lang.reflect.Field::getType).toList())
                .containsExactlyInAnyOrder(WorldParticipantMapper.class, CharacterSnapshotJsonMapper.class);
    }

    private WorldParticipant participant(long id, int order, String json, long sourceId) {
        WorldParticipant value = new WorldParticipant();
        value.setId(id); value.setWorldId(9L); value.setDisplayOrder(order);
        value.setParticipantType(WorldParticipantType.AI.getCode());
        value.setSourceCharacterId(sourceId); value.setCharacterSnapshot(json);
        return value;
    }

    private CharacterSnapshot snapshot(long id, String name) {
        return new CharacterSnapshot(1, id, "AI", name, null, null, null, null,
                null, null, null, CharacterSnapshot.Profile.empty(), "INITIAL", null, null, "#000");
    }
}
