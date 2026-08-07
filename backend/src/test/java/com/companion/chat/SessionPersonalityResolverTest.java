package com.companion.chat;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.chat.model.PersonalitySnapshot;
import com.companion.common.exception.BusinessException;
import com.companion.entity.Avatar;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;
import com.companion.mapper.PersonalityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionPersonalityResolverTest {

    @Mock private PersonalityMapper personalityMapper;

    private PersonalitySnapshotCodec codec;
    private DefaultSessionPersonalityResolver resolver;

    @BeforeEach
    void setUp() {
        codec = new PersonalitySnapshotCodec(new ObjectMapper());
        resolver = new DefaultSessionPersonalityResolver(personalityMapper, codec);
    }

    @Test
    void bindsOnlyPersonalityExplicitlyOwnedByTheAvatar() {
        Avatar avatar = avatar(10L, 20L, 30L);
        Personality personality = personality(30L, 20L, "原始人格");
        when(personalityMapper.selectOne(any())).thenAnswer(invocation -> {
            QueryWrapper<Personality> query = invocation.getArgument(0);
            query.getSqlSegment();
            Map<String, Object> values = query.getParamNameValuePairs();
            assertThat(values.values()).contains(30L, 20L, 1);
            return personality;
        });

        PersonalitySnapshot snapshot = resolver.resolveForNewSession(10L, avatar);

        assertThat(snapshot.personalityId()).isEqualTo(30L);
        assertThat(snapshot.avatarId()).isEqualTo(20L);
        assertThat(snapshot.name()).isEqualTo("原始人格");
        verify(personalityMapper).selectOne(any());
    }

    @Test
    void rejectsAnotherUsersAvatarBeforeReadingPersonality() {
        Avatar userBAvatar = avatar(11L, 20L, 30L);

        assertThatThrownBy(() -> resolver.resolveForNewSession(10L, userBAvatar))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资源不存在");

        verifyNoInteractions(personalityMapper);
    }

    @Test
    void oldSessionKeepsSnapshotAfterSourcePersonalityChanges() {
        Personality source = personality(30L, 20L, "原始人格");
        PersonalitySnapshot originalSnapshot = PersonalitySnapshot.from(source);

        ChatSession session = new ChatSession();
        session.setAvatarId(20L);
        session.setPersonalityId(30L);
        session.setPersonalitySnapshot(codec.encode(originalSnapshot));
        session.setPersonalitySnapshotVersion(PersonalitySnapshot.CURRENT_VERSION);

        source.setName("修改后人格");
        Personality resolved = resolver.resolveFromSession(session);

        assertThat(resolved.getName()).isEqualTo("原始人格");
        verifyNoInteractions(personalityMapper);
    }

    @Test
    void unboundLegacySessionNeverFallsBackToLatestPersonality() {
        ChatSession legacySession = new ChatSession();
        legacySession.setAvatarId(20L);

        assertThatThrownBy(() -> resolver.resolveFromSession(legacySession))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("会话未绑定人格");

        verify(personalityMapper, never()).selectOne(any());
    }

    private Avatar avatar(Long userId, Long avatarId, Long personalityId) {
        Avatar avatar = new Avatar();
        avatar.setId(avatarId);
        avatar.setUserId(userId);
        avatar.setPersonalityId(personalityId);
        avatar.setStatus(1);
        return avatar;
    }

    private Personality personality(Long id, Long avatarId, String name) {
        Personality personality = new Personality();
        personality.setId(id);
        personality.setAvatarId(avatarId);
        personality.setName(name);
        personality.setCorePersonality("温柔");
        personality.setIdentity("虚拟伴侣");
        personality.setLanguageStyle("自然");
        return personality;
    }
}
