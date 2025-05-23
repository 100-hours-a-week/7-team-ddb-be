package com.dolpin.global.interceptor;

import com.dolpin.global.util.LoggingUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

            // 사용자 ID 추출
            String userIdStr = MDC.get("userId");
            Long userId = "anonymous".equals(userIdStr) || "unknown".equals(userIdStr)
                    ? null : Long.parseLong(userIdStr);

            // API 응답 로깅
            LoggingUtils.logApiResponse(log, endpoint, response.getStatus(), totalTime, userId);

            // 느린 API 경고
            if (totalTime > 1000) {
                log.warn("Slow API detected - endpoint: {}, duration: {}ms, handler: {}",
                        endpoint, totalTime, request.getAttribute(HANDLER_METHOD_ATTRIBUTE));
            }

            // 예외 발생 시 로깅
            if (ex != null) {
                log.error("Request failed - endpoint: {}, duration: {}ms, error: {}",
                        endpoint, totalTime, ex.getMessage(), ex);
            }
        }

        // MDC 정리
        MDC.remove("handler");
        MDC.remove("controllerTime");
    }
}