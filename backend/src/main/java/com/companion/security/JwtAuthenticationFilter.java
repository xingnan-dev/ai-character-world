package com.companion.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final TokenSessionService tokenSessionService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   TokenSessionService tokenSessionService) {
        this.jwtTokenService = jwtTokenService;
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthenticatedUser user = jwtTokenService.parseAccessToken(token);
                if (!tokenSessionService.isActive(user.userId(), user.tokenId())) {
                    throw new IllegalArgumentException("JWT session is not active");
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.role()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                log.debug("JWT authentication failed: {}", e.getClass().getSimpleName());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtTokenService.getHeaderName());
        String prefix = jwtTokenService.getTokenPrefix();
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(prefix)) {
            return bearerToken.substring(prefix.length());
        }
        return null;
    }
}
