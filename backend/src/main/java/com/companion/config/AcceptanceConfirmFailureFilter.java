package com.companion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Acceptance-only switch: a key fails its first confirm request, then passes through. */
@Component
@Profile("acceptance")
public class AcceptanceConfirmFailureFilter extends OncePerRequestFilter {
    private final Set<String> failedKeys = ConcurrentHashMap.newKeySet();

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader("X-Acceptance-Fail-Confirm-Once");
        if ("POST".equals(request.getMethod())
                && "/api/character-images/confirm".equals(request.getRequestURI())
                && key != null && !key.isBlank() && failedKeys.add(key)) {
            response.setStatus(503);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"msg\":\"acceptance confirm failure once\",\"data\":null}");
            return;
        }
        chain.doFilter(request, response);
    }
}
