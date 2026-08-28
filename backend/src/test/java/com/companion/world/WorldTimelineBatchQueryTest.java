package com.companion.world;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.dto.response.WorldTimelinePageResponse;
import com.companion.entity.CharacterWorld;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldRound;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.WorldEventMapper;
import com.companion.mapper.WorldRoundMapper;
import com.companion.service.impl.WorldRoundServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorldTimelineBatchQueryTest {

    @Test
    @SuppressWarnings("unchecked")
    void loadsAllPageEventsWithOneBatchQuery() {
        CharacterWorldMapper worlds = mock(CharacterWorldMapper.class);
        WorldRoundMapper rounds = mock(WorldRoundMapper.class);
        WorldEventMapper events = mock(WorldEventMapper.class);
        CharacterWorld owned = new CharacterWorld();
        owned.setId(9L);
        when(worlds.selectOne(any(LambdaQueryWrapper.class))).thenReturn(owned);
        when(rounds.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(round(30L), round(20L), round(10L)));
        when(events.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(event(20L, 1), event(30L, 1), event(30L, 2)));
        WorldRoundServiceImpl service = new WorldRoundServiceImpl(
                worlds, rounds, events, mock(WorldRoundOrchestrator.class),
                Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC));

        WorldTimelinePageResponse page = service.getTimeline(1L, 9L, null, 2);

        assertThat(page.getItems()).hasSize(2);
        assertThat(page.isHasMore()).isTrue();
        assertThat(page.getNextBeforeRoundId()).isEqualTo(20L);
        assertThat(page.getItems().get(0).getEvents()).hasSize(2);
        assertThat(page.getItems().get(1).getEvents()).hasSize(1);
        verify(events).selectList(any(LambdaQueryWrapper.class));
    }

    private WorldRound round(long id) {
        WorldRound round = new WorldRound();
        round.setId(id);
        round.setWorldId(9L);
        round.setStatus("COMPLETED");
        return round;
    }

    private WorldEvent event(long roundId, int sequence) {
        WorldEvent event = new WorldEvent();
        event.setId(roundId + sequence);
        event.setRoundId(roundId);
        event.setSequenceNo(sequence);
        event.setStatus("COMPLETED");
        return event;
    }
}
