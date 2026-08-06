package com.companion.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.entity.AuthSession;
import com.companion.entity.User;
import com.companion.mapper.AuthSessionMapper;
import com.companion.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("soft-delete-test")
@Transactional
class JwtRevocationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private TokenSessionService tokenSessionService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthSessionMapper authSessionMapper;

    @Test
    void logoutRevokesCurrentTokenAndRejectsSubsequentRequests() throws Exception {
        User user = new User();
        user.setUsername("jwt-revocation-user");
        user.setPassword("encoded-password");
        user.setNickname("JWT User");
        user.setStatus(1);
        userMapper.insert(user);

        IssuedAccessToken token = jwtTokenService.issueAccessToken(user.getId(), user.getUsername());
        tokenSessionService.register(user.getId(), token);

        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", bearer(token.value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(user.getId()));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(token.value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        AuthSession session = authSessionMapper.selectOne(new LambdaQueryWrapper<AuthSession>()
                .eq(AuthSession::getJti, token.tokenId()));
        assertThat(session).isNotNull();
        assertThat(session.getRevokedAt()).isNotNull();

        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", bearer(token.value())))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
