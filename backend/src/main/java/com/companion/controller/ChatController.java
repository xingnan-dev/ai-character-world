package com.companion.controller;

import com.companion.chat.SseSubscriptionLifecycle;
import com.companion.ai.context.ConversationContextManager;
import com.companion.ai.exception.LlmProviderException;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.Result;
import com.companion.dto.request.ChatSendRequest;
import com.companion.dto.request.ChatSessionCreateRequest;
import com.companion.dto.response.ChatMessageVO;
import com.companion.dto.response.ChatSessionVO;
import com.companion.security.AuthenticatedUser;
import com.companion.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/session/create")
    public Result<ChatSessionVO> createSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChatSessionCreateRequest request) {
        Long userId = user.userId();
        log.info("创建会话: userId={}, avatarId={}", userId, request.getAvatarId());
        ChatSessionVO sessionVO = chatService.createSession(userId, request);
        return Result.success(sessionVO);
    }

    @GetMapping("/session/list")
    public Result<List<ChatSessionVO>> getSessionList(@AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user.userId();
        log.info("获取会话列表: userId={}", userId);
        List<ChatSessionVO> list = chatService.getSessionList(userId);
        return Result.success(list);
    }

    @DeleteMapping("/session/{id}")
    public Result<Void> deleteSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        Long userId = user.userId();
        log.info("删除会话: userId={}, sessionId={}", userId, id);
        chatService.deleteSession(userId, id);
        return Result.success();
    }

    @GetMapping("/session/{id}/messages")
    public Result<List<ChatMessageVO>> getMessageList(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        Long userId = user.userId();
        log.info("获取消息列表: userId={}, sessionId={}", userId, id);
        List<ChatMessageVO> list = chatService.getMessageList(userId, id);
        return Result.success(list);
    }

    @PostMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter streamChat(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChatSendRequest request) {
        Long userId = user.userId();
        log.info("流式聊天: userId={}, sessionId={}", userId, request.getSessionId());

        SseEmitter emitter = new SseEmitter(300000L);
        SseSubscriptionLifecycle subscriptionLifecycle = new SseSubscriptionLifecycle();

        Flux<String> flux = chatService.sendMessage(userId, request);

        BaseSubscriber<String> subscriber = new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                subscriptionLifecycle.attach(this);
                requestUnbounded();
            }

            @Override
            protected void hookOnNext(String token) {
                try {
                    emitter.send(SseEmitter.event().data(token));
                } catch (IOException e) {
                    log.error("发送token失败", e);
                    subscriptionLifecycle.cancelUpstream();
                    emitter.complete();
                }
            }

            @Override
            protected void hookOnError(Throwable error) {
                subscriptionLifecycle.markUpstreamTerminated();
                log.error("流式响应错误", error);
                try {
                    emitter.send(SseEmitter.event().name("error").data(safeError(error)));
                } catch (IOException e) {
                    log.error("发送错误事件失败", e);
                }
                // The provider error has already been translated into a safe SSE event.
                // Complete normally to avoid a second ERROR dispatch after commit.
                emitter.complete();
            }

            @Override
            protected void hookOnComplete() {
                subscriptionLifecycle.markUpstreamTerminated();
                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                } catch (IOException e) {
                    log.error("发送完成事件失败", e);
                }
                emitter.complete();
            }
        };
        flux.subscribe(subscriber);

        emitter.onCompletion(() -> {
            subscriptionLifecycle.cancelUpstream();
            log.info("SSE连接完成: sessionId={}", request.getSessionId());
        });
        emitter.onTimeout(() -> {
            subscriptionLifecycle.cancelUpstream();
            log.warn("SSE连接超时: sessionId={}", request.getSessionId());
        });
        emitter.onError(t -> {
            subscriptionLifecycle.cancelUpstream();
            log.error("SSE连接错误: sessionId={}", request.getSessionId(), t);
        });

        return emitter;
    }

    private SseErrorPayload safeError(Throwable error) {
        if (error instanceof ConversationContextManager.ContextWindowExceededException) {
            return new SseErrorPayload("INPUT_TOO_LONG", "输入内容过长，请缩短消息后重试");
        }
        if (error instanceof LlmProviderException providerException) {
            return new SseErrorPayload(
                    "LLM_" + providerException.getErrorType().name(),
                    "AI 服务暂时不可用，请稍后重试"
            );
        }
        if (error instanceof BusinessException businessException) {
            return new SseErrorPayload(
                    "CHAT_REQUEST_" + businessException.getCode(),
                    "聊天请求处理失败，请稍后重试"
            );
        }
        return new SseErrorPayload("CHAT_INTERNAL_ERROR", "聊天服务暂时不可用，请稍后重试");
    }

    private record SseErrorPayload(String code, String message) {
    }
}
