package com.companion.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.entity.AuthSession;
import com.companion.entity.User;
import com.companion.entity.enums.UserStatus;
import com.companion.mapper.AuthSessionMapper;
import com.companion.mapper.UserMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("soft-delete-test")
@Transactional
class JwtAuthenticationIntegrationTest {

    private static final String PASSWORD = "Valid1Password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthSessionMapper authSessionMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private TokenSessionService tokenSessionService;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void validJwtCanAccessProtectedEndpoint() throws Exception {
        User user = createActiveUser("valid-jwt-user");
        IssuedAccessToken token = jwtTokenService.issueAccessToken(user.getId(), user.getUsername());
        tokenSessionService.register(user.getId(), token);

        mockMvc.perform(get("/api/user/info")
                        .header(jwtProperties.getHeader(), jwtProperties.getPrefix() + token.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.username").value(user.getUsername()));
    }

    @Test
    void missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void forgedTokenReturnsUnauthorized() throws Exception {
        User user = createActiveUser("forged-jwt-user");
        String forgedToken = createToken(
                user,
                new Date(System.currentTimeMillis() + 60_000),
                "different-test-signing-secret-at-least-32-bytes"
        );

        mockMvc.perform(get("/api/user/info")
                        .header(jwtProperties.getHeader(), jwtProperties.getPrefix() + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        User user = createActiveUser("expired-jwt-user");
        String expiredToken = createToken(
                user,
                new Date(System.currentTimeMillis() - 60_000),
                jwtProperties.getSecret()
        );

        mockMvc.perform(get("/api/user/info")
                        .header(jwtProperties.getHeader(), jwtProperties.getPrefix() + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void activeUserCanLoginAndCreatesServerSideSession() throws Exception {
        User user = createActiveUser("normal-login-user");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"normal-login-user\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.id").value(user.getId()))
                .andExpect(jsonPath("$.data.user.username").value(user.getUsername()));

        Long sessionCount = authSessionMapper.selectCount(
                new LambdaQueryWrapper<AuthSession>().eq(AuthSession::getUserId, user.getId())
        );
        assertThat(sessionCount).isEqualTo(1L);
    }

    private User createActiveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setNickname(username);
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private String createToken(User user, Date expiration, String secret) {
        Date issuedAt = new Date(expiration.getTime() - 60_000);
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(jwtProperties.getIssuer())
                .setAudience(jwtProperties.getAudience())
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", "USER")
                .claim("tokenType", "access")
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
