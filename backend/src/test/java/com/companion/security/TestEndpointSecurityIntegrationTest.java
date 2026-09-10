package com.companion.security;

import com.companion.entity.User;
import com.companion.entity.enums.UserStatus;
import com.companion.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("soft-delete-test")
@Transactional
class TestEndpointSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private TokenSessionService tokenSessionService;

    @Test
    void databaseInspectionEndpointsAreNotRegistered() {
        boolean registered = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .anyMatch(pattern -> pattern.startsWith("/api/test"));

        assertThat(registered).isFalse();
    }

    @Test
    void anonymousRequestsCannotReadDatabaseInspectionData() throws Exception {
        for (String path : List.of("/api/test/db", "/api/test/user", "/api/test/raw")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.anyOf(
                                    org.hamcrest.Matchers.containsString("users"),
                                    org.hamcrest.Matchers.containsString("username"),
                                    org.hamcrest.Matchers.containsString("userCount"),
                                    org.hamcrest.Matchers.containsString("mybatisPlusTest"),
                                    org.hamcrest.Matchers.containsString("jdbcTest")
                            ))));
        }
    }

    @Test
    void authenticatedRequestsReceiveNotFoundWithoutInternalDetails() throws Exception {
        User user = new User();
        user.setUsername("removed-test-endpoint-user");
        user.setPassword("not-used");
        user.setNickname("Removed endpoint verifier");
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        IssuedAccessToken token = jwtTokenService.issueAccessToken(user.getId(), user.getUsername());
        tokenSessionService.register(user.getId(), token);

        for (String path : List.of("/api/test/raw", "/api/missing-resource-verification")) {
            mockMvc.perform(get(path)
                            .header(jwtTokenService.getHeaderName(),
                                    jwtTokenService.getTokenPrefix() + token.value()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.msg").value("资源不存在"))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.anyOf(
                                    org.hamcrest.Matchers.containsString(user.getUsername()),
                                    org.hamcrest.Matchers.containsString("database"),
                                    org.hamcrest.Matchers.containsString("jdbc"),
                                    org.hamcrest.Matchers.containsString("NoResourceFoundException"),
                                    org.hamcrest.Matchers.containsString("java."),
                                    org.hamcrest.Matchers.containsString(path),
                                    org.hamcrest.Matchers.containsString("at com.companion")
                            ))));
        }

        mockMvc.perform(get("/api/user/info")
                        .header(jwtTokenService.getHeaderName(),
                                jwtTokenService.getTokenPrefix() + token.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(user.getUsername()));
    }
}
