package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.AiService;
import com.companion.chat.ChatMessageLifecycleService;
import com.companion.chat.SessionPersonalityResolver;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.chat.model.PersonalitySnapshot;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.ChatSendRequest;
import com.companion.dto.request.ChatSessionCreateRequest;
import com.companion.dto.response.ChatMessageVO;
import com.companion.dto.response.ChatSessionVO;
import com.companion.entity.Avatar;
import com.companion.entity.ChatMessage;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;
import com.companion.entity.enums.ChatMessageStatus;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AvatarMapper avatarMapper;
    private final SessionPersonalityResolver sessionPersonalityResolver;
    private final ChatMessageLifecycleService chatMessageLifecycleService;
    private final AiService aiService;

    @Override
    public ChatSessionVO createSession(Long userId, ChatSessionCreateRequest request) {
        Avatar avatar = findOwnedActiveAvatar(userId, request.getAvatarId());
        if (avatar == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        PersonalitySnapshot personalitySnapshot = sessionPersonalityResolver.resolveForNewSession(userId, avatar);

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setAvatarId(request.getAvatarId());
        session.setPersonalityId(personalitySnapshot.personalityId());
        session.setPersonalitySnapshot(sessionPersonalityResolver.encode(personalitySnapshot));
        session.setPersonalitySnapshotVersion(personalitySnapshot.snapshotVersion());
        session.setTitle(request.getTitle() != null ? request.getTitle() : avatar.getName());
        session.setStatus(1);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.insert(session);

        ChatSessionVO vo = convertToVO(session);
        vo.setAvatarName(avatar.getName());
        return vo;
    }

    @Override
    public List<ChatSessionVO> getSessionList(Long userId) {
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .eq("status", 1)
                        .orderByDesc("create_time")
        );
        return sessions.stream()
                .map(session -> {
                    ChatSessionVO vo = convertToVO(session);
                    Avatar avatar = avatarMapper.selectById(session.getAvatarId());
                    if (avatar != null) {
                        vo.setAvatarName(avatar.getName());
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        ChatSession session = findOwnedActiveSession(userId, sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        chatSessionMapper.deleteById(sessionId);
    }

    @Override
    public Flux<String> sendMessage(Long userId, ChatSendRequest request) {
        ChatSession session = findOwnedActiveSession(userId, request.getSessionId());
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Avatar avatar = findOwnedActiveAvatar(userId, session.getAvatarId());
        if (avatar == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "头像不存在");
        }

        Personality personality = sessionPersonalityResolver.resolveFromSession(session);
        ChatMessageExchange exchange = chatMessageLifecycleService.createExchange(
                session.getId(), request.getContent(), request.getRequestId()
        );

        if (!exchange.created()) {
            return replayExistingExchange(exchange, request.getContent());
        }

        return aiService.chatStream(userId, session.getId(), request.getContent(), personality, exchange);
    }

    @Override
    public List<ChatMessageVO> getMessageList(Long userId, Long sessionId) {
        ChatSession session = findOwnedActiveSession(userId, sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        List<ChatMessage> messages = chatMessageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );
        return messages.stream()
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());
    }

    private ChatSession findOwnedActiveSession(Long userId, Long sessionId) {
        return chatSessionMapper.selectOne(
                new QueryWrapper<ChatSession>()
                        .eq("id", sessionId)
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
    }

    private Avatar findOwnedActiveAvatar(Long userId, Long avatarId) {
        return avatarMapper.selectOne(
                new QueryWrapper<Avatar>()
                        .eq("id", avatarId)
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
    }

    private ChatSessionVO convertToVO(ChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setAvatarId(session.getAvatarId());
        vo.setTitle(session.getTitle());
        vo.setCreateTime(session.getCreateTime() != null ? session.getCreateTime().toString() : null);
        return vo;
    }

    private ChatMessageVO convertToMessageVO(ChatMessage msg) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(msg.getId());
        vo.setSessionId(msg.getSessionId());
        vo.setRequestId(msg.getRequestId());
        vo.setRole(msg.getRole());
        vo.setContent(msg.getContent());
        vo.setEmotion(msg.getEmotion());
        vo.setStatus(msg.getStatus());
        vo.setErrorCode(msg.getErrorCode());
        vo.setErrorMessage(msg.getErrorMessage());
        vo.setCreateTime(msg.getCreateTime() != null ? msg.getCreateTime().toString() : null);
        return vo;
    }

    private Flux<String> replayExistingExchange(ChatMessageExchange exchange, String requestedContent) {
        ChatMessage userMessage = chatMessageMapper.selectById(exchange.userMessageId());
        ChatMessage assistantMessage = chatMessageMapper.selectById(exchange.assistantMessageId());
        if (userMessage == null || assistantMessage == null) {
            return Flux.error(new BusinessException(ResultCode.SERVER_ERROR.getCode(), "幂等消息记录不完整"));
        }
        if (!requestedContent.equals(userMessage.getContent())) {
            return Flux.error(new BusinessException(409, "requestId已用于其他消息内容"));
        }
        if (ChatMessageStatus.COMPLETED.getCode() == assistantMessage.getStatus()) {
            return Flux.just(assistantMessage.getContent());
        }
        return Flux.error(new BusinessException(409, "相同requestId的聊天请求已存在"));
    }
}
