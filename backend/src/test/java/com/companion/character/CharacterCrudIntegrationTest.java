package com.companion.character;

import com.companion.entity.Avatar;
import com.companion.entity.User;
import com.companion.entity.enums.UserStatus;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.CharacterMapper;
import com.companion.mapper.UserMapper;
import com.companion.security.IssuedAccessToken;
import com.companion.security.JwtProperties;
import com.companion.security.JwtTokenService;
import com.companion.security.TokenSessionService;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("soft-delete-test")
@Transactional
class CharacterCrudIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private AvatarMapper avatarMapper;
    @Autowired private CharacterMapper characterMapper;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private TokenSessionService tokenSessionService;
    @Autowired private JwtProperties jwtProperties;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        userA = createUser("character-user-a");
        userB = createUser("character-user-b");
        tokenA = tokenFor(userA);
        tokenB = tokenFor(userB);
    }

    @Test
    void createsAiCharacterWithoutAvatar() throws Exception {
        mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(characterJson("AI", "林墨", "INITIAL", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.characterType").value("AI"))
                .andExpect(jsonPath("$.data.avatarId").doesNotExist())
                .andExpect(jsonPath("$.data.visualType").value("INITIAL"));
    }

    @Test
    void createsUserCharacterWithoutAvatar() throws Exception {
        mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(characterJson("USER", "用户角色", "INITIAL", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.characterType").value("USER"));
    }

    @Test
    void listsOwnedCharactersAndSupportsTypeFilter() throws Exception {
        createCharacter(tokenA, "AI", "AI角色");
        createCharacter(tokenA, "USER", "用户角色");
        createCharacter(tokenB, "AI", "其他用户角色");

        mockMvc.perform(get("/api/characters").param("type", "AI")
                        .header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("AI角色"));
    }

    @Test
    void getsCharacterDetails() throws Exception {
        long id = createCharacter(tokenA, "AI", "详情角色");
        mockMvc.perform(get("/api/characters/{id}", id).header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.profile.values[0]").value("自由"));
    }

    @Test
    void updatesCharacter() throws Exception {
        long id = createCharacter(tokenA, "AI", "旧名字");
        mockMvc.perform(put("/api/characters/{id}", id)
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名字\",\"currentGoal\":\"完成新的目标\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("新名字"))
                .andExpect(jsonPath("$.data.currentGoal").value("完成新的目标"));
    }

    @Test
    void deletesCharacterAndMakesItUnavailable() throws Exception {
        long id = createCharacter(tokenA, "AI", "待删除角色");
        mockMvc.perform(delete("/api/characters/{id}", id).header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/characters/{id}", id).header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("资源不存在"));
        assertThat(characterMapper.selectById(id)).isNull();
    }

    @Test
    void userACannotReadUserBCharacter() throws Exception {
        long id = createCharacter(tokenB, "AI", "UserB角色");
        expectNotFound(get("/api/characters/{id}", id).header(authHeader(), bearer(tokenA)));
    }

    @Test
    void userACannotUpdateUserBCharacter() throws Exception {
        long id = createCharacter(tokenB, "AI", "UserB角色");
        expectNotFound(put("/api/characters/{id}", id)
                .header(authHeader(), bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"越权修改\"}"));
    }

    @Test
    void userACannotDeleteUserBCharacter() throws Exception {
        long id = createCharacter(tokenB, "AI", "UserB角色");
        expectNotFound(delete("/api/characters/{id}", id).header(authHeader(), bearer(tokenA)));
    }

    @Test
    void userACannotBindUserBAvatar() throws Exception {
        Avatar avatar = createAvatar(userB.getId());
        mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(characterJson("AI", "越权绑定", "VRM", avatar.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("资源不存在"));
    }

    @Test
    void bindsOwnedAvatarForVrmVisual() throws Exception {
        Avatar avatar = createAvatar(userA.getId());
        mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(characterJson("AI", "VRM角色", "VRM", avatar.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarId").value(avatar.getId()))
                .andExpect(jsonPath("$.data.visualType").value("VRM"));
    }

    @Test
    void rejectsInvalidListType() throws Exception {
        mockMvc.perform(get("/api/characters").param("type", "ROBOT")
                        .header(authHeader(), bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsInvalidCreateType() throws Exception {
        mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(characterJson("ROBOT", "非法角色", "INITIAL", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(characterJson("AI", " ", "INITIAL", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsBiographyOverLimit() throws Exception {
        Map<String, Object> payload = baseCharacter("AI", "超长角色", "INITIAL", null);
        payload.put("biography", "x".repeat(5001));
        expectParamError(payload);
    }

    @Test
    void rejectsProfileOverTenItems() throws Exception {
        Map<String, Object> payload = baseCharacter("AI", "超量档案", "INITIAL", null);
        payload.put("profile", Map.of("values", List.of(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"
        )));
        expectParamError(payload);
    }

    @Test
    void rejectsVrmVisualWithoutAvatar() throws Exception {
        mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(characterJson("AI", "无模型VRM", "VRM", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void acceptsInitialVisualWithoutAvatar() throws Exception {
        mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(characterJson("AI", "颜色头像角色", "INITIAL", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visualType").value("INITIAL"))
                .andExpect(jsonPath("$.data.avatarColor").value("#667eea"));
    }

    @Test
    void rejectsUnsafeAvatarColor() throws Exception {
        Map<String, Object> payload = baseCharacter("AI", "危险颜色", "INITIAL", null);
        payload.put("avatarColor", "url(javascript:alert(1))");
        expectParamError(payload);
    }

    @Test
    void missingTokenCannotAccessCharacters() throws Exception {
        mockMvc.perform(get("/api/characters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private void expectParamError(Map<String, Object> payload) throws Exception {
        mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    private void expectNotFound(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("资源不存在"));
    }

    @Test
    void parsesCharacterDraftWithMockProviderWithoutPersistence() throws Exception {
        Long before = characterMapper.selectCount(null);

        mockMvc.perform(post("/api/characters/parse")
                        .header(authHeader(), bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "characterType", "AI",
                                "description", "一个冷静理性的人工智能研究员"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.characterType").value("AI"))
                .andExpect(jsonPath("$.data.name").value("林澈"))
                .andExpect(jsonPath("$.data.sourceDescription").value("一个冷静理性的人工智能研究员"));

        assertThat(characterMapper.selectCount(null)).isEqualTo(before);
    }

    @Test
    void parseRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/characters/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characterType\":\"AI\",\"description\":\"角色描述\"}"))
                .andExpect(status().isUnauthorized());
    }

    private long createCharacter(String token, String type, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/characters")
                        .header(authHeader(), bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(characterJson(type, name, "INITIAL", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("id").asLong();
    }

    private String characterJson(String type, String name, String visualType, Long avatarId) throws Exception {
        return objectMapper.writeValueAsString(baseCharacter(type, name, visualType, avatarId));
    }

    private Map<String, Object> baseCharacter(String type, String name, String visualType, Long avatarId) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("characterType", type);
        payload.put("name", name);
        payload.put("age", 22);
        payload.put("identity", "摄影师");
        payload.put("corePersonality", "开朗但敏感");
        payload.put("currentGoal", "完成一次旅行摄影展");
        payload.put("biography", "喜欢旅行与摄影的年轻角色");
        payload.put("relationshipToUser", "朋友");
        payload.put("speakingStyle", "自然、真诚");
        payload.put("profile", Map.of("values", List.of("自由"), "interests", List.of("摄影")));
        payload.put("visualType", visualType);
        payload.put("avatarColor", "#667eea");
        if (avatarId != null) payload.put("avatarId", avatarId);
        return payload;
    }

    private Avatar createAvatar(Long userId) {
        Avatar avatar = new Avatar();
        avatar.setUserId(userId);
        avatar.setName("owned-avatar-" + userId);
        avatar.setStatus(1);
        avatar.setCreateTime(LocalDateTime.now());
        avatar.setUpdateTime(LocalDateTime.now());
        avatarMapper.insert(avatar);
        return avatar;
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username + "-" + System.nanoTime());
        user.setPassword("not-used-in-this-test");
        user.setNickname(username);
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private String tokenFor(User user) {
        IssuedAccessToken token = jwtTokenService.issueAccessToken(user.getId(), user.getUsername());
        tokenSessionService.register(user.getId(), token);
        return token.value();
    }

    private String authHeader() {
        return jwtProperties.getHeader();
    }

    private String bearer(String token) {
        return jwtProperties.getPrefix() + token;
    }
}
