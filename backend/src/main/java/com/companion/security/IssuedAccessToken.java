package com.companion.security;

import java.time.LocalDateTime;

public record IssuedAccessToken(
        String value,
        String tokenId,
        LocalDateTime expiresAt
) {
}
