package com.companion.security;

public record AuthenticatedUser(
        Long userId,
        String username,
        String role,
        String tokenId
) {
}
