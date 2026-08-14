package com.companion.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.AiService;
import com.companion.entity.Avatar;
import com.companion.entity.ChatSession;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.AuthSessionMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.mapper.PersonalityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "ai.mock.enabled=true",
        "jwt.secret=resource-ownership-test-jwt-secret-at-least-32-bytes"
})
@AutoConfigureMockMvc
class ResourceOwnershipIntegrationTest {

    private static final long USER_A_ID = 1001L;
    private static final long USER_B_ID = 2002L;
    private static final long USER_B_AVATAR_ID = 3003L;
    private static final long USER_B_SESSION_ID = 4004L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean
    private AvatarMapper avatarMapper;

    @MockBean
    private ChatSessionMapper chatSessionMapper;

    @MockBean
    private ChatMessageMapper chatMessageMapper;

    @MockBean
    private PersonalityMapper personalityMapper;

    @MockBean
    private AiService aiService;

    @MockBean
    private AuthSessionMapper authSessionMapper;

    private String userAToken;
    private Avatar userBAvatar;
    private ChatSession userBSession;

    @BeforeEach
    void setUp() {
        when(authSessionMapper.countActiveForEnabledUser(any(), any())).thenReturn(1L);
        userAToken = jwtTokenService.generateAccessToken(USER_A_ID, "UserA");

        userBAvatar = new Avatar();
        userBAvatar.setId(USER_B_AVATAR_ID);
        userBAvatar.setUserId(USER_B_ID);
        userBAvatar.setName("UserB-Avatar");
        userBAvatar.setStatus(1);

        userBSession = new ChatSession();
        userBSession.setId(USER_B_SESSION_ID);
        userBSession.setUserId(USER_B_ID);
        userBSession.setAvatarId(USER_B_AVATAR_ID);
        userBSession.setStatus(1);

        when(avatarMapper.selectOne(any())).thenAnswer(invocation -> {
            QueryWrapper<Avatar> wrapper = invocation.getArgument(0);
            return containsParameter(wrapper, USER_B_ID)
                    && containsParameter(wrapper, USER_B_AVATAR_ID)
                    ? userBAvatar
                    : null;
        });

        when(chatSessionMapper.selectOne(any())).thenAnswer(invocation -> {
            QueryWrapper<ChatSession> wrapper = invocation.getArgument(0);
            return containsParameter(wrapper, USER_B_ID)
                    && containsParameter(wrapper, USER_B_SESSION_ID)
                    ? userBSession
                    : null;
        });
    }

    @Test
    @DisplayName("UserA不能查询UserB的Avatar")
    void userACannotGetUserBAvatar() throws Exception {
        mockMvc.perform(get("/api/avatar/{id}", USER_B_AVATAR_ID)
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("资源不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("UserA不能使用UserB的Avatar创建聊天会话")
    void userACannotCreateSessionWithUserBAvatar() throws Exception {
        mockMvc.perform(post("/api/chat/session/create")
                        .header("Authorization", bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatarId": 3003,
                                  "title": "unauthorized session"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("资源不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(chatSessionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("UserA不能查询UserB的聊天记录")
    void userACannotGetUserBChatMessages() throws Exception {
        mockMvc.perform(get("/api/chat/session/{id}/messages", USER_B_SESSION_ID)
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("资源不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(chatMessageMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("UserA不能修改UserB的Personality")
    void userACannotUpdateUserBPersonality() throws Exception {
        mockMvc.perform(put("/api/personality/update")
                        .header("Authorization", bearer(userAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "avatarId": 3003,
                                  "name": "UserB Personality",
                                  "templateType": 1,
                                  "corePersonality": "gentle",
                                  "identity": "companion",
                                  "languageStyle": "friendly"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("资源不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(personalityMapper, never()).updateById(any());
    }

    private boolean containsParameter(QueryWrapper<?> wrapper, Object expected) {
        if (wrapper == null) {
            return false;
        }
        Map<String, Object> parameters = wrapper.getParamNameValuePairs();
        return parameters.values().stream().anyMatch(expected::equals);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
