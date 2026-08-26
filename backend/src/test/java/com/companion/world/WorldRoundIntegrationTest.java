package com.companion.world;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.entity.CharacterWorld;
import com.companion.entity.User;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldRound;
import com.companion.entity.enums.UserStatus;
import com.companion.entity.enums.WorldRoundStatus;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.UserMapper;
import com.companion.mapper.WorldEventMapper;
import com.companion.mapper.WorldRoundMapper;
import com.companion.security.IssuedAccessToken;
import com.companion.security.JwtProperties;
import com.companion.security.JwtTokenService;
import com.companion.security.TokenSessionService;
import com.companion.service.WorldRoundService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("soft-delete-test")
class WorldRoundIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private CharacterWorldMapper worldMapper;
    @Autowired private WorldRoundMapper roundMapper;
    @Autowired private WorldEventMapper eventMapper;
    @Autowired private WorldRoundService roundService;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private TokenSessionService tokenSessionService;
    @Autowired private JwtProperties jwtProperties;

    private User owner;
    private User other;
    private String ownerToken;
    private String otherToken;
    private CharacterWorld world;

    @BeforeEach
    void setUp() {
        owner = createUser("round-owner");
        other = createUser("round-other");
        ownerToken = tokenFor(owner);
        otherToken = tokenFor(other);
        world = createWorld(owner.getId(), "Round World");
    }

    @Test
    void createsPendingRoundAndSingleCompletedUserEvent() throws Exception {
        MvcResult result = createRound(ownerToken, world.getId(), "request-1", "  讨论新的计划  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.userInput").value("讨论新的计划"))
                .andExpect(jsonPath("$.data.ownerUserId").doesNotExist())
                .andReturn();

        long roundId = responseId(result);
        WorldRound stored = roundMapper.selectById(roundId);
        List<WorldEvent> events = events(roundId);
        assertThat(stored.getRequestId()).isEqualTo("request-1");
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getSequenceNo()).isEqualTo(1);
            assertThat(event.getParticipantId()).isNull();
            assertThat(event.getEventType()).isEqualTo("USER_MESSAGE");
            assertThat(event.getStatus()).isEqualTo("COMPLETED");
            assertThat(event.getContent()).isEqualTo(stored.getUserInput());
        });
    }

    @Test
    void returnsExistingRoundForSameNormalizedInputAndRejectsConflict() throws Exception {
        long firstId = responseId(createRound(ownerToken, world.getId(), " same-id ", " same input ")
                .andExpect(jsonPath("$.code").value(200)).andReturn());
        long secondId = responseId(createRound(ownerToken, world.getId(), "same-id", "same input")
                .andExpect(jsonPath("$.code").value(200)).andReturn());

        assertThat(secondId).isEqualTo(firstId);
        assertThat(roundCount(world.getId(), "same-id")).isEqualTo(1);
        assertThat(events(firstId)).hasSize(1);

        createRound(ownerToken, world.getId(), "same-id", "different")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
        assertThat(roundCount(world.getId(), "same-id")).isEqualTo(1);
        assertThat(events(firstId)).hasSize(1);
    }

    @Test
    void concurrentDuplicateCreatesAtMostOneRoundAndEvent() throws Exception {
        int workers = 6;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Long> ids = java.util.Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < workers; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                    WorldRoundCreateRequest request = request("concurrent-id", "same input");
                    ids.add(roundService.create(owner.getId(), world.getId(), request).getId());
                } catch (Throwable error) {
                    errors.add(error);
                }
            });
        }
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();

        assertThat(errors).isEmpty();
        assertThat(new HashSet<>(ids)).hasSize(1);
        assertThat(roundCount(world.getId(), "concurrent-id")).isEqualTo(1);
        assertThat(events(ids.get(0))).hasSize(1);
    }

    @Test
    void protectsWorldOwnershipPathBindingAndSoftDeletedWorld() throws Exception {
        long roundId = responseId(createRound(ownerToken, world.getId(), "secure-id", "hello")
                .andExpect(jsonPath("$.code").value(200)).andReturn());
        CharacterWorld otherWorld = createWorld(owner.getId(), "Other world");

        createRound(otherToken, world.getId(), "foreign", "hello")
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(get("/api/worlds/{worldId}/rounds/{roundId}", world.getId(), roundId)
                        .header(authHeader(), bearer(otherToken)))
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(get("/api/worlds/{worldId}/rounds/{roundId}", otherWorld.getId(), roundId)
                        .header(authHeader(), bearer(ownerToken)))
                .andExpect(jsonPath("$.code").value(404));
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> roundService.tryStart(other.getId(), world.getId(), roundId)))
                .isInstanceOf(com.companion.common.exception.BusinessException.class);

        worldMapper.deleteById(world.getId());
        mockMvc.perform(get("/api/worlds/{worldId}/rounds/{roundId}", world.getId(), roundId)
                        .header(authHeader(), bearer(ownerToken)))
                .andExpect(jsonPath("$.code").value(404));
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> roundService.tryStart(owner.getId(), world.getId(), roundId)))
                .isInstanceOf(com.companion.common.exception.BusinessException.class);
    }

    @Test
    void ignoresClientOwnerFieldAndValidatesRequestContent() throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("requestId", "owner-spoof");
        payload.put("userInput", "hello");
        payload.put("ownerUserId", other.getId());
        mockMvc.perform(post("/api/worlds/{worldId}/rounds", world.getId())
                        .header(authHeader(), bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(jsonPath("$.code").value(200));

        createRound(ownerToken, world.getId(), " ", "hello")
                .andExpect(jsonPath("$.code").value(400));
        createRound(ownerToken, world.getId(), "x", " ")
                .andExpect(jsonPath("$.code").value(400));
        createRound(ownerToken, world.getId(), "x".repeat(65), "hello")
                .andExpect(jsonPath("$.code").value(400));
        createRound(ownerToken, world.getId(), "x", "y".repeat(4001))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void atomicallyStartsPendingRoundOnlyOnceAndRejectsTerminalStates() {
        long roundId = roundService.create(owner.getId(), world.getId(), request("start-id", "hello")).getId();
        assertThat(roundService.tryStart(owner.getId(), world.getId(), roundId)).isTrue();
        assertThat(roundService.tryStart(owner.getId(), world.getId(), roundId)).isFalse();
        WorldRound running = roundMapper.selectById(roundId);
        assertThat(running.getStatus()).isEqualTo(WorldRoundStatus.RUNNING.name());
        assertThat(running.getStartedTime()).isNotNull();

        long completedId = roundService.create(owner.getId(), world.getId(), request("completed-id", "hello")).getId();
        WorldRound completed = roundMapper.selectById(completedId);
        completed.setStatus(WorldRoundStatus.COMPLETED.name());
        roundMapper.updateById(completed);
        assertThat(roundService.tryStart(owner.getId(), world.getId(), completedId)).isFalse();
    }

    @Test
    void queriesRoundAndEventsInSequenceWithoutInternalFields() throws Exception {
        long roundId = roundService.create(owner.getId(), world.getId(), request("query-id", "first")).getId();
        insertEvent(roundId, 3, "third");
        insertEvent(roundId, 2, "second");

        mockMvc.perform(get("/api/worlds/{worldId}/rounds/{roundId}", world.getId(), roundId)
                        .header(authHeader(), bearer(ownerToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.worldId").value(world.getId()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        mockMvc.perform(get("/api/worlds/{worldId}/rounds/{roundId}/events", world.getId(), roundId)
                        .header(authHeader(), bearer(ownerToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].sequenceNo").value(1))
                .andExpect(jsonPath("$.data[1].sequenceNo").value(2))
                .andExpect(jsonPath("$.data[2].sequenceNo").value(3))
                .andExpect(jsonPath("$.data[0].worldId").doesNotExist());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/worlds/{worldId}/rounds", world.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/worlds/{worldId}/rounds/1", world.getId()))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions createRound(
            String token, long worldId, String requestId, String input) throws Exception {
        return mockMvc.perform(post("/api/worlds/{worldId}/rounds", worldId)
                .header(authHeader(), bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("requestId", requestId, "userInput", input))));
    }

    private WorldRoundCreateRequest request(String requestId, String input) {
        WorldRoundCreateRequest request = new WorldRoundCreateRequest();
        request.setRequestId(requestId);
        request.setUserInput(input);
        return request;
    }

    private long responseId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asLong();
    }

    private long roundCount(long worldId, String requestId) {
        return roundMapper.selectCount(new LambdaQueryWrapper<WorldRound>()
                .eq(WorldRound::getWorldId, worldId).eq(WorldRound::getRequestId, requestId));
    }

    private List<WorldEvent> events(long roundId) {
        return eventMapper.selectList(new LambdaQueryWrapper<WorldEvent>()
                .eq(WorldEvent::getRoundId, roundId).orderByAsc(WorldEvent::getSequenceNo));
    }

    private void insertEvent(long roundId, int sequence, String content) {
        WorldEvent event = new WorldEvent();
        event.setRoundId(roundId);
        event.setSequenceNo(sequence);
        event.setEventType("AI_MESSAGE");
        event.setContent(content);
        event.setStatus("COMPLETED");
        event.setCreateTime(LocalDateTime.now());
        eventMapper.insert(event);
    }

    private CharacterWorld createWorld(Long ownerId, String name) {
        CharacterWorld value = new CharacterWorld();
        value.setOwnerUserId(ownerId);
        value.setName(name + System.nanoTime());
        value.setStatus(1);
        value.setCreateTime(LocalDateTime.now());
        value.setUpdateTime(LocalDateTime.now());
        worldMapper.insert(value);
        return value;
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + "-" + System.nanoTime());
        user.setPassword("not-used-in-this-test");
        user.setNickname(prefix);
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private String tokenFor(User user) {
        IssuedAccessToken issued = jwtTokenService.issueAccessToken(user.getId(), user.getUsername());
        tokenSessionService.register(user.getId(), issued);
        return issued.value();
    }

    private String authHeader() { return jwtProperties.getHeader(); }
    private String bearer(String token) { return jwtProperties.getPrefix() + token; }
}
