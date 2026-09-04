package com.companion.world;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.dto.request.CharacterCreateRequest;
import com.companion.dto.response.CharacterResponse;
import com.companion.entity.CharacterWorld;
import com.companion.entity.User;
import com.companion.entity.enums.UserStatus;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldRound;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.UserMapper;
import com.companion.mapper.WorldEventMapper;
import com.companion.mapper.WorldRoundMapper;
import com.companion.service.WorldRoundService;
import com.companion.service.CharacterService;
import com.companion.service.CharacterWorldService;
import com.companion.service.UserService;
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
    @Autowired private UserMapper userMapper;
    @Autowired private WorldRoundMapper roundMapper;
    @Autowired private WorldRoundService roundService;
    @Autowired private CharacterService characterService;
    @Autowired private CharacterWorldService characterWorldService;
    @Autowired private UserService userService;
    @SpyBean private WorldEventMapper eventMapper;

    @Test
    void eventInsertFailureRollsBackRound() {
        User user = createUser();
        CharacterCreateRequest characterRequest = new CharacterCreateRequest();
        characterRequest.setCharacterType("USER");
        characterRequest.setName("transaction-user-character");
        characterRequest.setIdentity("测试用户");
        CharacterResponse userCharacter = characterService.create(user.getId(), characterRequest);
        userService.setCurrentUserCharacter(user.getId(), userCharacter.getId());

        CharacterWorld world = new CharacterWorld();
        world.setOwnerUserId(user.getId());
        world.setName("transaction-world");
        world.setStatus(1);
        world.setCreateTime(LocalDateTime.now());
        world.setUpdateTime(LocalDateTime.now());
        worldMapper.insert(world);
        characterWorldService.setUserCharacter(user.getId(), world.getId(), userCharacter.getId());

        WorldRoundCreateRequest request = new WorldRoundCreateRequest();
        request.setRequestId("rollback-request");
        request.setUserInput("must rollback");
        doThrow(new DataIntegrityViolationException("forced event failure"))
                .when(eventMapper).insert(any(WorldEvent.class));

        try {
            assertThatThrownBy(() -> roundService.create(user.getId(), world.getId(), request))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            reset(eventMapper);
        }

        assertThat(roundMapper.selectCount(new LambdaQueryWrapper<WorldRound>()
                .eq(WorldRound::getWorldId, world.getId())
                .eq(WorldRound::getRequestId, "rollback-request"))).isZero();
    }

    private User createUser() {
        User user = new User();
        user.setUsername("transaction-user-" + System.nanoTime());
        user.setPassword("not-used-in-this-test");
        user.setNickname("transaction-user");
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }
}
