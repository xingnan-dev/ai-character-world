package com.companion.world;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.entity.CharacterWorld;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldRound;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.WorldEventMapper;
import com.companion.mapper.WorldRoundMapper;
import com.companion.service.WorldRoundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
class WorldRoundTransactionIntegrationTest {

    @Autowired private CharacterWorldMapper worldMapper;
    @Autowired private WorldRoundMapper roundMapper;
    @Autowired private WorldRoundService roundService;
    @SpyBean private WorldEventMapper eventMapper;

    @Test
    void eventInsertFailureRollsBackRound() {
        CharacterWorld world = new CharacterWorld();
        world.setOwnerUserId(991L);
        world.setName("transaction-world");
        world.setStatus(1);
        world.setCreateTime(LocalDateTime.now());
        world.setUpdateTime(LocalDateTime.now());
        worldMapper.insert(world);

        WorldRoundCreateRequest request = new WorldRoundCreateRequest();
        request.setRequestId("rollback-request");
        request.setUserInput("must rollback");
        doThrow(new DataIntegrityViolationException("forced event failure"))
                .when(eventMapper).insert(any(WorldEvent.class));

        try {
            assertThatThrownBy(() -> roundService.create(991L, world.getId(), request))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            reset(eventMapper);
        }

        assertThat(roundMapper.selectCount(new LambdaQueryWrapper<WorldRound>()
                .eq(WorldRound::getWorldId, world.getId())
                .eq(WorldRound::getRequestId, "rollback-request"))).isZero();
    }
}
