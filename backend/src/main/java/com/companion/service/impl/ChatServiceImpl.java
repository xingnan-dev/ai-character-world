package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.AiService;
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
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.mapper.PersonalityMapper;
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
    private final PersonalityMapper personalityMapper;
    private final AiService aiService;

    @Override
    public ChatSessionVO createSession(Long userId, ChatSessionCreateRequest request) {
        Avatar avatar = avatarMapper.selectById(request.getAvatarId());
        if (avatar == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setAvatarId(request.getAvatarId());
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
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        session.setStatus(0);
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);
    }

    @Override
    public Flux<String> sendMessage(Long userId, ChatSendRequest request) {
        ChatSession session = chatSessionMapper.selectById(request.getSessionId());
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Avatar avatar = avatarMapper.selectById(session.getAvatarId());
        if (avatar == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "头像不存在");
        }

        Personality personality = null;
        if (avatar.getPersonalityId() != null) {
            personality = personalityMapper.selectById(avatar.getPersonalityId());
        }
        if (personality == null) {
            personality = personalityMapper.selectOne(
                    new QueryWrapper<Personality>().eq("avatar_id", session.getAvatarId())
            );
        }
        if (personality == null) {
            personality = personalityMapper.selectOne(
                    new QueryWrapper<Personality>().orderByDesc("id").last("LIMIT 1")
            );
        }
        if (personality == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "该头像尚未设置人格");
        }

        return aiService.chatStream(userId, session.getId(), request.getContent(), personality);
    }

    @Override
    public List<ChatMessageVO> getMessageList(Long sessionId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );
        return messages.stream()
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());
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
        vo.setRole(msg.getRole());
        vo.setContent(msg.getContent());
        vo.setEmotion(msg.getEmotion());
        vo.setCreateTime(msg.getCreateTime() != null ? msg.getCreateTime().toString() : null);
        return vo;
    }
}