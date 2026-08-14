package com.companion.security;

import com.companion.entity.User;
import com.companion.entity.enums.UserStatus;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.mapper.UserMapper;
import com.companion.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("soft-delete-test")
@Transactional
class SseAsyncSecurityIntegrationTest {

    private static final String STREAM_REQUEST = "{\"sessionId\":1,\"content\":\"hello\"}";

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

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private ChatService chatService;

    private User user;
    private IssuedAccessToken accessToken;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("sse-security-user");
        user.setPassword(passwordEncoder.encode("Valid1Password"));
        user.setNickname("SSE security user");
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        accessToken = jwtTokenService.issueAccessToken(user.getId(), user.getUsername());
        tokenSessionService.register(user.getId(), accessToken);
        when(chatService.sendMessage(anyLong(), any())).thenReturn(Flux.just("reply"));
    }

    @Test
    void authenticatedUserCanEstablishSseRequest() throws Exception {
        mockMvc.perform(authenticatedStreamRequest())
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(chatService).sendMessage(anyLong(), any());
    }

    @Test
    void sseAsyncCompletionDoesNotReturnUnauthorizedOrForbidden() throws Exception {
        MvcResult initialResult = mockMvc.perform(authenticatedStreamRequest())
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(initialResult))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[DONE]")));
    }

    @Test
    void unauthenticatedUserCannotAccessChatStream() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(STREAM_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(request().asyncNotStarted());

        verify(chatService, never()).sendMessage(anyLong(), any());
    }

    @Test
    void sseErrorUsesStableSafePayload() throws Exception {
        assertProviderErrorIsSafelyCompleted(LlmErrorType.UPSTREAM_ERROR, 500);
    }

    @Test
    void rateLimitIsReturnedAsSseErrorWithoutSecondaryAccessDenied() throws Exception {
        assertProviderErrorIsSafelyCompleted(LlmErrorType.RATE_LIMIT, 429);
    }

    @Test
    void providerClientErrorIsReturnedAsSseErrorWithoutSecondaryAccessDenied() throws Exception {
        assertProviderErrorIsSafelyCompleted(LlmErrorType.INVALID_REQUEST, 400);
    }

    private void assertProviderErrorIsSafelyCompleted(LlmErrorType errorType, int statusCode) throws Exception {
        when(chatService.sendMessage(anyLong(), any())).thenReturn(Flux.error(new LlmProviderException(
                "test", errorType, statusCode, false, "sensitive provider response"
        )));

        MvcResult initialResult = mockMvc.perform(authenticatedStreamRequest())
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        initialResult.getAsyncResult();
        MvcResult completedResult = mockMvc.perform(asyncDispatch(initialResult))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = completedResult.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(responseBody)
                .contains("LLM_" + errorType.name())
                .contains("AI 服务暂时不可用，请稍后重试")
                .doesNotContain("sensitive provider response");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedStreamRequest() {
        return post("/api/chat/stream")
                .header(jwtProperties.getHeader(), jwtProperties.getPrefix() + accessToken.value())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(STREAM_REQUEST);
    }
}
