package com.dolpin.global.security;

import com.dolpin.domain.auth.service.token.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.http.Cookie;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private static final Set<String> NOISY_ENDPOINTS = new HashSet<>(Collections.singletonList("/api/v1/health"));

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isNoisyEndpoint = NOISY_ENDPOINTS.contains(path);

        // Only log non-noisy endpoints at INFO level
        if (!isNoisyEndpoint) {
            log.info("JwtAuthenticationFilter processing: {}", path);
        } else if (log.isDebugEnabled()) {
            // Log noisy endpoints only at DEBUG level
            log.debug("JwtAuthenticationFilter processing health check: {}", path);
        }

        try {
            String token = extractToken(request, isNoisyEndpoint);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                if (!isNoisyEndpoint) {
                    log.info("Valid token for user ID: {}", userId);
                }

                // UserDetails 생성
                UserDetails userDetails = new User(
                        userId.toString(),
                        "",
                        Collections.emptyList()
                );

                // Authentication 객체 생성 및 저장
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (!isNoisyEndpoint) {
                    log.info("Authentication set for user ID: {}", userId);
                }
            } else {
                if (!isNoisyEndpoint) {
                    log.warn("No valid token found");
                } else if (log.isDebugEnabled()) {
                    log.debug("No valid token found for health check");
                }
            }
        } catch (Exception e) {
            log.error("JWT Authentication failed: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request, boolean isNoisyEndpoint) {
        // 1. 쿠키에서 토큰 추출
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                    return cookie.getValue();
                }
            }
        }

        // Only log at WARN level for non-health check endpoints
        if (!isNoisyEndpoint) {
            log.warn("No token found in cookies");
        } else if (log.isDebugEnabled()) {
            log.debug("No token found in cookies for health check");
        }

        return null;
    }
}