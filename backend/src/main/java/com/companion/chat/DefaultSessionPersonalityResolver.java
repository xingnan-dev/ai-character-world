package com.companion.chat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.chat.model.PersonalitySnapshot;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.entity.Avatar;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;
import com.companion.mapper.PersonalityMapper;
import org.springframework.stereotype.Component;

@Component
public class DefaultSessionPersonalityResolver implements SessionPersonalityResolver {

    private final PersonalityMapper personalityMapper;
    private final PersonalitySnapshotCodec snapshotCodec;

    public DefaultSessionPersonalityResolver(PersonalityMapper personalityMapper,
                                             PersonalitySnapshotCodec snapshotCodec) {
        this.personalityMapper = personalityMapper;
        this.snapshotCodec = snapshotCodec;
    }

    @Override
    public PersonalitySnapshot resolveForNewSession(Long userId, Avatar avatar) {
        if (avatar == null || userId == null || !userId.equals(avatar.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Personality personality = null;
        if (avatar.getPersonalityId() != null) {
            personality = personalityMapper.selectOne(
                    new QueryWrapper<Personality>()
                            .eq("id", avatar.getPersonalityId())
                            .eq("avatar_id", avatar.getId())
                            .eq("status", 1)
            );
        }

        // This fallback is only for creating a new session from legacy avatars that do not yet
        // carry an explicit personality_id. It is never used to repair or read an old session.
        if (personality == null && avatar.getPersonalityId() == null) {
            personality = personalityMapper.selectOne(
                    new QueryWrapper<Personality>()
                            .eq("avatar_id", avatar.getId())
                            .eq("status", 1)
                            .orderByDesc("id")
                            .last("LIMIT 1")
            );
        }

        if (personality == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "该头像尚未设置人格");
        }
        return PersonalitySnapshot.from(personality);
    }

    @Override
    public String encode(PersonalitySnapshot snapshot) {
        return snapshotCodec.encode(snapshot);
    }

    @Override
    public Personality resolveFromSession(ChatSession session) {
        if (session == null
                || session.getPersonalityId() == null
                || session.getPersonalitySnapshot() == null
                || session.getPersonalitySnapshotVersion() == null) {
            throw unboundSession();
        }

        final PersonalitySnapshot snapshot;
        try {
            snapshot = snapshotCodec.decode(session.getPersonalitySnapshot());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "会话人格快照无效");
        }

        if (!session.getPersonalityId().equals(snapshot.personalityId())
                || !session.getAvatarId().equals(snapshot.avatarId())
                || !session.getPersonalitySnapshotVersion().equals(snapshot.snapshotVersion())) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "会话人格快照不一致");
        }
        return snapshot.toPersonality();
    }

    private BusinessException unboundSession() {
        return new BusinessException(ResultCode.NOT_FOUND.getCode(), "会话未绑定人格，请创建新会话");
    }
}
