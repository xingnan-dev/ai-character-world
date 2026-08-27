package com.companion.world;

import com.companion.character.model.CharacterProfile;
import com.companion.dto.request.CharacterCreateRequest;
import com.companion.dto.request.CharacterUpdateRequest;
import com.companion.dto.response.CharacterResponse;
import com.companion.entity.CharacterWorld;
import com.companion.entity.User;
import com.companion.entity.WorldParticipant;
import com.companion.entity.enums.UserStatus;
import com.companion.mapper.CharacterWorldMapper;
import com.companion.mapper.UserMapper;
import com.companion.mapper.WorldParticipantMapper;
import com.companion.security.IssuedAccessToken;
import com.companion.security.JwtProperties;
import com.companion.security.JwtTokenService;
import com.companion.security.TokenSessionService;
import com.companion.service.CharacterService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("soft-delete-test")
class CharacterWorldIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private CharacterService characterService;
    @Autowired private CharacterWorldMapper worldMapper;
    @Autowired private WorldParticipantMapper participantMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private TokenSessionService tokenSessionService;
    @Autowired private JwtProperties jwtProperties;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        userA = createUser("world-user-a");
        userB = createUser("world-user-b");
        tokenA = tokenFor(userA);
        tokenB = tokenFor(userB);
    }

    @Test
    void createsWorldPersistsSnapshotsAndReturnsParticipantsInStableOrder() throws Exception {
        CharacterResponse ai = createCharacter(userA.getId(), "AI", "分析师");
        CharacterResponse observer = createCharacter(userA.getId(), "AI", "观察者");

        long worldId = createWorld(tokenA, List.of(
                participant(observer.getId(), "AI", 20),
                participant(ai.getId(), "AI", 10)
        ));

        mockMvc.perform(get("/api/worlds/{id}", worldId).header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.participants[0].displayOrder").value(0))
                .andExpect(jsonPath("$.data.participants[0].character.name").value("分析师"))
                .andExpect(jsonPath("$.data.participants[1].displayOrder").value(1))
                .andExpect(jsonPath("$.data.participants[1].participantType").value("AI"));

        List<WorldParticipant> persisted = participantMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WorldParticipant>()
                        .eq("world_id", worldId)
        );
        assertThat(persisted).hasSize(2);
        assertThat(persisted).allMatch(item -> item.getCharacterSnapshot().contains("snapshotVersion"));
    }

    @Test
    void characterUpdateDoesNotChangeExistingParticipantSnapshot() throws Exception {
        CharacterResponse character = createCharacter(userA.getId(), "AI", "快照原名");
        long worldId = createWorld(tokenA, List.of(participant(character.getId(), "AI", 0)));

        CharacterUpdateRequest update = new CharacterUpdateRequest();
        update.setName("实时角色新名");
        update.setCorePersonality("实时角色新性格");
        characterService.update(userA.getId(), character.getId(), update);

        mockMvc.perform(get("/api/worlds/{id}", worldId).header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[0].character.name").value("快照原名"))
                .andExpect(jsonPath("$.data.participants[0].character.corePersonality").value("冷静可靠"));
    }

    @Test
    void characterSoftDeleteDoesNotRemoveExistingParticipantSnapshot() throws Exception {
        CharacterResponse character = createCharacter(userA.getId(), "AI", "待删除角色");
        long worldId = createWorld(tokenA, List.of(participant(character.getId(), "AI", 0)));

        characterService.delete(userA.getId(), character.getId());

        mockMvc.perform(get("/api/worlds/{id}", worldId).header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[0].character.name").value("待删除角色"))
                .andExpect(jsonPath("$.data.participants[0].character.corePersonality").value("冷静可靠"))
                .andExpect(jsonPath("$.data.participants[0].character.profile").isMap());
    }

    @Test
    void rejectsDeletedAndForeignCharactersAndRollsBackWorldCreation() throws Exception {
        CharacterResponse deleted = createCharacter(userA.getId(), "AI", "已删除");
        characterService.delete(userA.getId(), deleted.getId());
        CharacterResponse foreign = createCharacter(userB.getId(), "AI", "其他用户角色");
        long before = worldMapper.selectCount(null);
        long participantsBefore = participantMapper.selectCount(null);

        CharacterResponse owned = createCharacter(userA.getId(), "AI", "合法角色");
        expectCode(createWorldRequest(tokenA, List.of(
                participant(deleted.getId(), "AI", 0), participant(owned.getId(), "AI", 1))), 404);
        expectCode(createWorldRequest(tokenA, List.of(
                participant(foreign.getId(), "AI", 0), participant(owned.getId(), "AI", 1))), 404);
        expectCode(createWorldRequest(tokenA, List.of(
                participant(owned.getId(), "AI", 0), participant(foreign.getId(), "AI", 1)
        )), 404);
        assertThat(worldMapper.selectCount(null)).isEqualTo(before);
        assertThat(participantMapper.selectCount(null)).isEqualTo(participantsBefore);
    }

    @Test
    void worldOwnershipComesFromJwtAndOtherUsersCannotAccessIt() throws Exception {
        CharacterResponse character = createCharacter(userA.getId(), "AI", "所有权角色");
        CharacterResponse companion = createCharacter(userA.getId(), "AI", "所有权辅助角色");
        Map<String, Object> payload = worldPayload(List.of(
                participant(character.getId(), "AI", 0), participant(companion.getId(), "AI", 1)));
        payload.put("ownerUserId", userB.getId());
        MvcResult result = mockMvc.perform(post("/api/worlds")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long worldId = responseId(result);

        CharacterWorld stored = worldMapper.selectById(worldId);
        assertThat(stored.getOwnerUserId()).isEqualTo(userA.getId());
        mockMvc.perform(get("/api/worlds/{id}", worldId).header(authHeader(), bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void rejectsDuplicateCharacterOrderInvalidTypeAndOversizedInput() throws Exception {
        CharacterResponse first = createCharacter(userA.getId(), "AI", "角色一");
        CharacterResponse second = createCharacter(userA.getId(), "AI", "角色二");
        CharacterResponse userCharacter = createCharacter(userA.getId(), "USER", "用户角色");

        expectCode(createWorldRequest(tokenA, List.of(
                participant(first.getId(), "AI", 0), participant(first.getId(), "AI", 1)
        )), 400);
        expectCode(createWorldRequest(tokenA, List.of(
                participant(first.getId(), "AI", 0), participant(second.getId(), "AI", 0)
        )), 400);
        expectCode(createWorldRequest(tokenA, List.of(
                participant(first.getId(), "ROBOT", 0), participant(second.getId(), "AI", 1))), 400);
        expectCode(createWorldRequest(tokenA, List.of(
                participant(userCharacter.getId(), "AI", 0), participant(second.getId(), "AI", 1))), 400);

        Map<String, Object> nullParticipant = worldPayload(new ArrayList<>());
        ((List<Object>) nullParticipant.get("participants")).add(null);
        mockMvc.perform(post("/api/worlds")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullParticipant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        Map<String, Object> oversized = worldPayload(List.of(participant(first.getId(), "AI", 0)));
        oversized.put("name", "x".repeat(101));
        mockMvc.perform(post("/api/worlds")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oversized)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void requiresAuthenticationForWorldEndpoints() throws Exception {
        mockMvc.perform(post("/api/worlds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/worlds/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsUpdatesAndSoftDeletesOnlyOwnedWorldsWithoutChangingSnapshots() throws Exception {
        CharacterResponse first = createCharacter(userA.getId(), "AI", "语义角色一");
        CharacterResponse second = createCharacter(userA.getId(), "AI", "语义角色二");
        long worldId = createWorld(tokenA, List.of(
                participant(first.getId(), "AI", 8), participant(second.getId(), "AI", 3)));
        CharacterResponse foreignFirst = createCharacter(userB.getId(), "AI", "外部一");
        CharacterResponse foreignSecond = createCharacter(userB.getId(), "AI", "外部二");
        createWorld(tokenB, List.of(
                participant(foreignFirst.getId(), "AI", 0), participant(foreignSecond.getId(), "AI", 1)));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("name", "更新后的世界"); update.put("background", "新背景");
        update.put("rules", "新规则"); update.put("atmosphere", "宁静");
        update.put("scene", "湖边木屋"); update.put("sourceDescription", "原始世界描述");
        mockMvc.perform(put("/api/worlds/{id}", worldId).header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("更新后的世界"))
                .andExpect(jsonPath("$.data.atmosphere").value("宁静"))
                .andExpect(jsonPath("$.data.scene").value("湖边木屋"))
                .andExpect(jsonPath("$.data.participants[0].character.name").value("语义角色二"));

        mockMvc.perform(get("/api/worlds").header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(worldId));
        mockMvc.perform(delete("/api/worlds/{id}", worldId).header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/worlds/{id}", worldId).header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(get("/api/worlds").header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void replacesUnlockedRosterWithFreshSnapshotsAndNormalizesOrder() throws Exception {
        CharacterResponse first = createCharacter(userA.getId(), "AI", "替换前一");
        CharacterResponse second = createCharacter(userA.getId(), "AI", "替换前二");
        long worldId = createWorld(tokenA, List.of(
                participant(first.getId(), "AI", 0), participant(second.getId(), "AI", 1)));
        CharacterUpdateRequest update = new CharacterUpdateRequest(); update.setName("替换后新名");
        characterService.update(userA.getId(), first.getId(), update);

        Map<String, Object> body = Map.of("participants", List.of(
                participant(first.getId(), "AI", 40), participant(second.getId(), "AI", 10)));
        mockMvc.perform(put("/api/worlds/{id}/participants", worldId)
                        .header(authHeader(), bearer(tokenA)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants[0].displayOrder").value(0))
                .andExpect(jsonPath("$.data.participants[0].character.name").value("替换前二"))
                .andExpect(jsonPath("$.data.participants[1].displayOrder").value(1))
                .andExpect(jsonPath("$.data.participants[1].character.name").value("替换后新名"))
                .andExpect(jsonPath("$.data.participantsLocked").value(false));
    }

    @Test
    void locksRosterAfterAnyRoundAndHidesForeignWorld() throws Exception {
        CharacterResponse first = createCharacter(userA.getId(), "AI", "锁定一");
        CharacterResponse second = createCharacter(userA.getId(), "AI", "锁定二");
        long worldId = createWorld(tokenA, List.of(
                participant(first.getId(), "AI", 0), participant(second.getId(), "AI", 1)));
        mockMvc.perform(post("/api/worlds/{worldId}/rounds", worldId)
                        .header(authHeader(), bearer(tokenA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"lock-round\",\"userInput\":\"hello\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/worlds/{id}", worldId).header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.participantsLocked").value(true));

        Map<String, Object> body = Map.of("participants", List.of(
                participant(first.getId(), "AI", 0), participant(second.getId(), "AI", 1)));
        mockMvc.perform(put("/api/worlds/{id}/participants", worldId)
                        .header(authHeader(), bearer(tokenA)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("WORLD_PARTICIPANTS_LOCKED"));
        mockMvc.perform(put("/api/worlds/{id}/participants", worldId)
                        .header(authHeader(), bearer(tokenB)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(404));
    }

    private long createWorld(String token, List<Map<String, Object>> participants) throws Exception {
        if (participants.size() == 1) {
            CharacterResponse companion = createCharacter(userA.getId(), "AI", "辅助角色-" + System.nanoTime());
            List<Map<String, Object>> expanded = new ArrayList<>(participants);
            expanded.add(participant(companion.getId(), "AI", 9999));
            participants = expanded;
        }
        MvcResult result = createWorldRequest(token, participants)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return responseId(result);
    }

    private org.springframework.test.web.servlet.ResultActions createWorldRequest(
            String token, List<Map<String, Object>> participants) throws Exception {
        return mockMvc.perform(post("/api/worlds")
                .header(authHeader(), bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(worldPayload(participants))));
    }

    private void expectCode(org.springframework.test.web.servlet.ResultActions result, int code) throws Exception {
        result.andExpect(status().isOk()).andExpect(jsonPath("$.code").value(code));
    }

    private Map<String, Object> worldPayload(List<Map<String, Object>> participants) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "最小测试世界");
        payload.put("background", "一间用于协作讨论的工作室");
        payload.put("rules", "参与者轮流表达观点，不进行人身攻击");
        payload.put("participants", participants);
        return payload;
    }

    private Map<String, Object> participant(long characterId, String type, int order) {
        return Map.of("characterId", characterId, "participantType", type, "displayOrder", order);
    }

    private long responseId(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asLong();
    }

    private CharacterResponse createCharacter(Long userId, String type, String name) {
        CharacterProfile profile = new CharacterProfile();
        profile.setValues(new ArrayList<>(List.of("诚实")));
        profile.setInterests(new ArrayList<>(List.of("软件工程")));

        CharacterCreateRequest request = new CharacterCreateRequest();
        request.setCharacterType(type);
        request.setName(name);
        request.setIdentity("研究员");
        request.setCorePersonality("冷静可靠");
        request.setCurrentGoal("完成一次协作任务");
        request.setBiography("用于 World 基础测试的角色");
        request.setRelationshipToUser("伙伴");
        request.setSpeakingStyle("简洁自然");
        request.setProfile(profile);
        request.setVisualType("INITIAL");
        request.setAvatarColor("#667eea");
        return characterService.create(userId, request);
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

    private String authHeader() {
        return jwtProperties.getHeader();
    }

    private String bearer(String token) {
        return jwtProperties.getPrefix() + token;
    }
}
