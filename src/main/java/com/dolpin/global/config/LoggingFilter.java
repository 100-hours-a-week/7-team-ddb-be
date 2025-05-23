package com.dolpin.global.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements Filter {

    private static final String TRACE_ID = "traceId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_METHOD = "requestMethod";
    private static final String CLIENT_IP = "clientIp";
    private static final String USER_AGENT = "userAgent";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        long startTime = System.currentTimeMillis();
        
        try {
            // MDC 설정
            setupMDC(httpRequest);
            
            // 요청 로깅
            logRequest(httpRequest);
            
            chain.doFilter(request, response);
            
        } finally {
            // 응답 로깅
            logResponse(httpRequest, httpResponse, startTime);
            
            // MDC 정리
            MDC.clear();
        }
    }

    private void setupMDC(HttpServletRequest request) {
        // Trace ID 설정 (헤더에서 가져오거나 새로 생성)
        String traceId = request.getHeader("X-Trace-Id");
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TRACE_ID, traceId);
        
        // 기본 요청 정보
        MDC.put(REQUEST_URI, request.getRequestURI());
        MDC.put(REQUEST_METHOD, request.getMethod());
        MDC.put(CLIENT_IP, getClientIpAddress(request));
        MDC.put(USER_AGENT, request.getHeader("User-Agent"));
        
        // 사용자 ID는 인증 후에 설정될 예정 (JwtAuthenticationFilter에서)
    }

    private void logRequest(HttpServletRequest request) {
        if (shouldLogRequest(request)) {
            log.info(">>> REQUEST: {} {} from {}", 
                request.getMethod(), 
                request.getRequestURI(),
                getClientIpAddress(request)
            );
        }
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response, long startTime) {
        if (shouldLogRequest(request)) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< RESPONSE: {} {} - Status: {}, Duration: {}ms", 
                request.getMethod(), 
                request.getRequestURI(),
                response.getStatus(),
                duration
            );
        }
    }

    private boolean shouldLogRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // 헬스체크나 정적 리소스는 로그에서 제외
        return !uri.startsWith("/actuator") && 
               !uri.startsWith("/static") && 
               !uri.equals("/api/v1/health");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIP)) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }

    public static void setUserId(String userId) {
        if (StringUtils.hasText(userId)) {
            MDC.put(USER_ID, userId);
        }
    }

    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId.toString());
        }
    }
}