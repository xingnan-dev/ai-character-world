package com.companion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String DEFAULT_ROLE = "USER";

    private final JwtProperties properties;
    private SecretKey signingKey;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initializeSigningKey() {
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String username) {
        return issueAccessToken(userId, username).value();
    }

    public IssuedAccessToken issueAccessToken(Long userId, String username) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + properties.getExpiration() * 1000);
        String tokenId = UUID.randomUUID().toString();

        String value = Jwts.builder()
                .setId(tokenId)
                .setIssuer(properties.getIssuer())
                .setAudience(properties.getAudience())
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", DEFAULT_ROLE)
                .claim("tokenType", ACCESS_TOKEN_TYPE)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(signingKey)
                .compact();

        return new IssuedAccessToken(
                value,
                tokenId,
                LocalDateTime.ofInstant(expiresAt.toInstant(), ZoneId.systemDefault())
        );
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .setAllowedClockSkewSeconds(30)
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String tokenType = claims.get("tokenType", String.class);
        if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
            throw new IllegalArgumentException("JWT is not an access token");
        }

        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        if (userId == null || username == null || username.isBlank() || role == null || role.isBlank()) {
            throw new IllegalArgumentException("JWT is missing required claims");
        }

        return new AuthenticatedUser(userId, username, role, claims.getId());
    }

    public String getHeaderName() {
        return properties.getHeader();
    }

    public String getTokenPrefix() {
        return properties.getPrefix();
    }
}
