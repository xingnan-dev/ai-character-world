package com.companion.service;

import com.companion.dto.request.ChatSendRequest;
import com.companion.dto.request.ChatSessionCreateRequest;
import com.companion.dto.response.ChatMessageVO;
import com.companion.dto.response.ChatSessionVO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    ChatSessionVO createSession(Long userId, ChatSessionCreateRequest request);

    List<ChatSessionVO> getSessionList(Long userId);

    void deleteSession(Long userId, Long sessionId);

    Flux<String> sendMessage(Long userId, ChatSendRequest request);

    List<ChatMessageVO> getMessageList(Long sessionId);
}