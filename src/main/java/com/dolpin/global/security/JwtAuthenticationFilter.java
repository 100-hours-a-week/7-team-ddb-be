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

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("JwtAuthenticationFilter processing: {}", request.getRequestURI());

        try {
            String token = extractToken(request);
            log.info("Token extracted: {}", token != null ? "exists" : "null");

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                log.info("Valid token for user ID: {}", userId);

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
                log.info("Authentication set for user ID: {}", userId);
            } else {
                log.warn("No valid token found");
            }
        } catch (Exception e) {
            log.error("JWT Authentication failed: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // 디버그 레벨로 변경하여 필요할 때만 로그가 출력되도록 함
        if (log.isDebugEnabled()) {
            log.debug("Extracting token from request");
        }

        // 1. 쿠키에서 토큰 추출
        Cookie[] cookies = request.getCookies();

        // 쿠키 개수만 간략히 로깅
        if (log.isDebugEnabled() && cookies != null) {
            log.debug("Processing {} cookies", cookies.length);
        }

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                // 접근 토큰 쿠키를 찾았을 때만 로그 출력
                if ("access_token".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                    return cookie.getValue();
                }
            }
        }

        // 경고 로그는 유지 (토큰을 찾지 못한 경우는 중요한 정보이므로)
        log.warn("No token found in cookies");
        return null;
    }
}