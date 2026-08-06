package com.companion.security;

import com.companion.entity.User;
import com.companion.entity.enums.UserStatus;
import com.companion.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("soft-delete-test")
@Transactional
class AccountSecurityIntegrationTest {

    private static final String CURRENT_PASSWORD = "Current1Password";
    private static final String NEW_PASSWORD = "Changed2Password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private TokenSessionService tokenSessionService;

    @Test
    void disabledAccountCannotLogin() throws Exception {
        createUser("disabled-user", UserStatus.DISABLED);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("disabled-user", CURRENT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void lockedAccountCannotLogin() throws Exception {
        createUser("locked-user", UserStatus.LOCKED);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("locked-user", CURRENT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void disablingAccountImmediatelyInvalidatesExistingToken() throws Exception {
        User user = createUser("runtime-disabled-user", UserStatus.ACTIVE);
        IssuedAccessToken token = issueAndRegister(user);

        user.setStatus(UserStatus.DISABLED.getCode());
        userMapper.updateById(user);

        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", bearer(token.value())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void changingPasswordRevokesAllOldTokensAndAllowsNewPasswordLogin() throws Exception {
        User user = createUser("password-change-user", UserStatus.ACTIVE);
        IssuedAccessToken firstToken = issueAndRegister(user);
        IssuedAccessToken secondToken = issueAndRegister(user);

        mockMvc.perform(put("/api/user/password")
                        .header("Authorization", bearer(firstToken.value()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson(CURRENT_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertUnauthorized(firstToken.value());
        assertUnauthorized(secondToken.value());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(user.getUsername(), CURRENT_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(user.getUsername(), NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void weakNewPasswordIsRejectedWithoutRevokingCurrentToken() throws Exception {
        User user = createUser("weak-password-user", UserStatus.ACTIVE);
        IssuedAccessToken token = issueAndRegister(user);

        mockMvc.perform(put("/api/user/password")
                        .header("Authorization", bearer(token.value()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson(CURRENT_PASSWORD, "weakpass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", bearer(token.value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void incorrectCurrentPasswordDoesNotChangePasswordOrRevokeToken() throws Exception {
        User user = createUser("incorrect-current-password-user", UserStatus.ACTIVE);
        IssuedAccessToken token = issueAndRegister(user);

        mockMvc.perform(put("/api/user/password")
                        .header("Authorization", bearer(token.value()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("Wrong1Password", NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", bearer(token.value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private User createUser(String username, UserStatus status) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(CURRENT_PASSWORD));
        user.setNickname(username);
        user.setStatus(status.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    private IssuedAccessToken issueAndRegister(User user) {
        IssuedAccessToken token = jwtTokenService.issueAccessToken(user.getId(), user.getUsername());
        tokenSessionService.register(user.getId(), token);
        return token;
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String changePasswordJson(String currentPassword, String newPassword) {
        return "{\"currentPassword\":\"" + currentPassword
                + "\",\"newPassword\":\"" + newPassword + "\"}";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
