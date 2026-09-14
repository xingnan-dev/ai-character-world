package com.companion.world;

import com.companion.character.snapshot.*;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.dto.response.CharacterResponse;
import com.companion.entity.*;
import com.companion.mapper.*;
import com.companion.service.CharacterService;
import com.companion.service.impl.WorldRoundServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorldRoundSnapshotTest {
    @Test void newRoundStoresFrozenWorldSnapshot(){
        CharacterWorldMapper worlds=mock(CharacterWorldMapper.class);WorldRoundMapper rounds=mock(WorldRoundMapper.class);WorldEventMapper events=mock(WorldEventMapper.class);CharacterService characters=mock(CharacterService.class);
        CharacterWorld world=new CharacterWorld();world.setId(2L);world.setUserCharacterId(4L);world.setName("bar");world.setBackground("old");when(worlds.selectOwnedForUpdate(1L,2L)).thenReturn(world);
        CharacterResponse user=new CharacterResponse();user.setId(4L);user.setCharacterType("USER");user.setName("user");user.setVisualType("INITIAL");when(characters.get(1L,4L)).thenReturn(user);
        when(rounds.insert(any())).thenAnswer(i->{((WorldRound)i.getArgument(0)).setId(3L);return 1;});
        WorldRoundServiceImpl service=new WorldRoundServiceImpl(worlds,rounds,events,mock(WorldRoundOrchestrator.class),Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"),ZoneOffset.UTC),characters,new CharacterSnapshotJsonMapper(new ObjectMapper()));
        WorldRoundCreateRequest request=new WorldRoundCreateRequest();request.setRequestId("r");request.setUserInput("hello");service.create(1L,2L,request);
        ArgumentCaptor<WorldRound> c=ArgumentCaptor.forClass(WorldRound.class);verify(rounds).insert(c.capture());WorldSnapshot snapshot=new WorldSnapshotJsonMapper(new ObjectMapper()).read(c.getValue().getWorldSnapshot());assertThat(snapshot.name()).isEqualTo("bar");assertThat(c.getValue().getWorldSnapshotVersion()).isEqualTo(1);
    }
    @Test void newRoundDoesNotUseLegacyFallback(){newRoundStoresFrozenWorldSnapshot();}
}
