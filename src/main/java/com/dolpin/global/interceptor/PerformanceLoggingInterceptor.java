package com.dolpin.global.interceptor;

import com.dolpin.global.util.LoggingUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Component
public class PerformanceLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final String HANDLER_METHOD_ATTRIBUTE = "handlerMethod";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 요청 시작 시간 기록
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        request.setAttribute(HANDLER_METHOD_ATTRIBUTE, handler.toString());

        // MDC에 핸들러 정보 추가
        MDC.put("handler", handler.getClass().getSimpleName());

        // 인증된 사용자 ID 설정 (Security Filter 이후이므로 정확함)
        setUserIdInMDC();

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
        // 컨트롤러 처리 완료 후 로직
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime != null) {
            long processingTime = System.currentTimeMillis() - startTime;
            MDC.put("controllerTime", String.valueOf(processingTime));
            log.debug("Controller processing completed in {}ms", processingTime);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);

        if (startTime != null) {
            long totalTime = System.currentTimeMillis() - startTime;
            String endpoint = request.getMethod() + " " + request.getRequestURI();

            // 최종 사용자 ID 추출 (인증 완료 후)
            String userIdStr = getCurrentUserId();
            Long userId = "anonymous".equals(userIdStr) || "unknown".equals(userIdStr) || "processing".equals(userIdStr)
                    ? null : parseUserId(userIdStr);

            // MDC 업데이트
            MDC.put("userId", userIdStr);

            // API 응답 로깅
            LoggingUtils.logApiResponse(log, endpoint, response.getStatus(), totalTime, userId);

            // 느린 API 경고
            if (totalTime > 1000) {
                log.warn("Slow API detected - endpoint: {}, duration: {}ms, handler: {}, user: {}",
                        endpoint, totalTime, request.getAttribute(HANDLER_METHOD_ATTRIBUTE), userIdStr);
            }

            // 예외 발생 시 로깅
            if (ex != null) {
                log.error("Request failed - endpoint: {}, duration: {}ms, user: {}, error: {}",
                        endpoint, totalTime, userIdStr, ex.getMessage(), ex);
            }
        }

        // MDC 정리
        MDC.remove("handler");
        MDC.remove("controllerTime");
    }

    private void setUserIdInMDC() {
        String userId = getCurrentUserId();
        MDC.put("userId", userId);
    }

    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                return userDetails.getUsername();
            } else {
                return "anonymous";
            }
        } catch (Exception e) {
            log.debug("Failed to get user ID: {}", e.getMessage());
            return "unknown";
        }
    }

    private Long parseUserId(String userIdStr) {
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
