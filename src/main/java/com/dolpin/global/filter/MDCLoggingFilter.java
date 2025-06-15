package com.dolpin.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE) // 가장 나중에 실행되도록 변경
public class MDCLoggingFilter extends OncePerRequestFilter {

    private static final String USER_ID_KEY = "userId";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String REQUEST_PATH_KEY = "requestPath";
    private static final String REQUEST_METHOD_KEY = "method";
    private static final String CLIENT_IP_KEY = "clientIp";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 요청 ID 생성 및 설정
            String requestId = generateRequestId();
            MDC.put(REQUEST_ID_KEY, requestId);

            // 요청 정보 설정
            MDC.put(REQUEST_PATH_KEY, request.getRequestURI());
            MDC.put(REQUEST_METHOD_KEY, request.getMethod());
            MDC.put(CLIENT_IP_KEY, getClientIpAddress(request));

            // 초기 사용자 ID 설정 (인증 전)
            MDC.put(USER_ID_KEY, "processing");

            // 응답 헤더에 요청 ID 추가
            response.setHeader("X-Request-ID", requestId);

            // 요청 시작 로그
            log.info("Request started - {} {} from {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    getClientIpAddress(request));

            long startTime = System.currentTimeMillis();

            // 다음 필터 실행 (인증 포함)
            filterChain.doFilter(request, response);

            // 인증 완료 후 실제 사용자 ID 설정
            setUserIdInMDC();

            // 요청 처리 시간 계산
            long duration = System.currentTimeMillis() - startTime;

            // 요청 완료 로그 (실제 사용자 ID와 함께)
            log.info("Request completed - {} {} - Status: {} - Duration: {}ms - User: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    MDC.get(USER_ID_KEY));

        } finally {
            // MDC 정리 (메모리 누수 방지)
            MDC.clear();
        }
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private void setUserIdInMDC() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String userId = userDetails.getUsername();
                MDC.put(USER_ID_KEY, userId);
                log.debug("✅ User authenticated: {}", userId);
            } else {
                MDC.put(USER_ID_KEY, "anonymous");
                log.debug("❌ No authentication found");
            }
        } catch (Exception e) {
            log.debug("Failed to set user ID in MDC: {}", e.getMessage());
            MDC.put(USER_ID_KEY, "unknown");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/health") ||
                path.startsWith("/actuator") ||
                path.startsWith("/favicon.ico");
    }
}
