package com.companion.security;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.companion.entity.AuthSession;
import com.companion.mapper.AuthSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenSessionService {

    private final AuthSessionMapper authSessionMapper;

    public void register(Long userId, IssuedAccessToken token) {
        AuthSession session = new AuthSession();
        session.setJti(token.tokenId());
        session.setUserId(userId);
        session.setExpiresAt(token.expiresAt());
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        authSessionMapper.insert(session);
    }

    public boolean isActive(Long userId, String tokenId) {
        if (userId == null || tokenId == null || tokenId.isBlank()) {
            return false;
        }
        return authSessionMapper.countActiveForEnabledUser(userId, tokenId) > 0;
    }

    public boolean revoke(Long userId, String tokenId) {
        if (userId == null || tokenId == null || tokenId.isBlank()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return authSessionMapper.update(
                null,
                new UpdateWrapper<AuthSession>()
                        .eq("jti", tokenId)
                        .eq("user_id", userId)
                        .isNull("revoked_at")
                        .set("revoked_at", now)
                        .set("update_time", now)
        ) > 0;
    }

    public int revokeAll(Long userId) {
        if (userId == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        return authSessionMapper.update(
                null,
                new UpdateWrapper<AuthSession>()
                        .eq("user_id", userId)
                        .isNull("revoked_at")
                        .set("revoked_at", now)
                        .set("update_time", now)
        );
    }
}
