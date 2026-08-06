package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.ChatSendRequest;
import com.companion.dto.request.ChatSessionCreateRequest;
import com.companion.dto.response.ChatMessageVO;
import com.companion.dto.response.ChatSessionVO;
import com.companion.security.AuthenticatedUser;
import com.companion.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ExecutorService executorService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
        this.executorService = Executors.newCachedThreadPool();
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

        Flux<String> flux = chatService.sendMessage(userId, request);

        flux.subscribe(
                token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException e) {
                        log.error("发送token失败", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("流式响应错误", error);
                    try {
                        emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                    } catch (IOException e) {
                        log.error("发送错误事件失败", e);
                    }
                    emitter.completeWithError(error);
                },
                () -> {
                    try {
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    } catch (IOException e) {
                        log.error("发送完成事件失败", e);
                    }
                    emitter.complete();
                }
        );

        emitter.onCompletion(() -> log.info("SSE连接完成: sessionId={}", request.getSessionId()));
        emitter.onTimeout(() -> log.warn("SSE连接超时: sessionId={}", request.getSessionId()));
        emitter.onError(t -> log.error("SSE连接错误: sessionId={}", request.getSessionId(), t));

        return emitter;
    }
}
