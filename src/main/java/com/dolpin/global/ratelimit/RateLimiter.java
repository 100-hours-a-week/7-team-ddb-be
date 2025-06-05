package com.dolpin.global.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimiter {

    private final Map<String, LimitConfig> limitConfigs = new ConcurrentHashMap<>();
    private final Map<String, ServiceCounter> counters = new ConcurrentHashMap<>();

    @Value("${ai.service.rate-limit.max-requests}")
    private int aiServiceMaxRequests;

    @Value("${ai.service.rate-limit.period}")
    private int aiServicePeriodSeconds;

    // dev 전용 토큰 (환경변수)
    @Value("${ai.service.dev-bypass-token:}")
    private String devBypassToken;

    // dev 환경 체크
    @Value("${spring.profiles.active:}")
    private String activeProfile;

    public void init() {
        registerLimit("ai-service", aiServiceMaxRequests, Duration.ofSeconds(aiServicePeriodSeconds));

        // dev 환경에서 토큰이 설정되어 있으면 알림
        if (isDevEnvironment() && devBypassToken != null && !devBypassToken.isEmpty()) {
            log.warn(" DEV MODE: Rate limit bypass token is active! Token: {}***",
                    devBypassToken.substring(0, Math.min(4, devBypassToken.length())));
        }
    }

    public void registerLimit(String serviceName, int maxRequests, Duration period) {
        limitConfigs.put(serviceName, new LimitConfig(maxRequests, period));
        counters.put(serviceName, new ServiceCounter(Instant.now()));
    }

    // 기존 메서드 (하위 호환성 유지)
    public boolean allowRequest(String serviceName) {
        LimitConfig config = limitConfigs.get(serviceName);
        if (config == null) {
            return true;
        }

        ServiceCounter counter = counters.get(serviceName);
        Instant now = Instant.now();

        // 제한 기간이 지났으면 카운터 리셋
        if (Duration.between(counter.resetTime, now).compareTo(config.period) > 0) {
            counter.count.set(0);
            counter.resetTime = now;
        }

        // 현재 카운트가 최대 요청 수보다 작으면 허용
        if (counter.count.incrementAndGet() <= config.maxRequests) {
            return true;
        } else {
            return false;
        }
    }

    // 새로 추가된 메서드 (토큰 지원)
    public boolean allowRequest(String serviceName, String token) {
        // dev 환경에서 토큰이 일치하면 우회
        if (isDevBypass(token)) {
            log.debug(" DEV: Rate limit bypassed for service: {}", serviceName);
            return true;
        }

        // 기존 로직 그대로 호출
        return allowRequest(serviceName);
    }

    // dev 환경 체크
    private boolean isDevEnvironment() {
        return "dev".equals(activeProfile) || (activeProfile != null && activeProfile.contains("dev"));
    }

    // dev 토큰 검증
    private boolean isDevBypass(String token) {
        return isDevEnvironment() &&
                devBypassToken != null &&
                !devBypassToken.isEmpty() &&
                devBypassToken.equals(token);
    }

    // 남은 요청 수 확인 메서드
    public int getRemainingRequests(String serviceName) {
        LimitConfig config = limitConfigs.get(serviceName);
        if (config == null) {
            return -1; // 설정이 없으면 -1 반환
        }

        ServiceCounter counter = counters.get(serviceName);
        Instant now = Instant.now();

        // 제한 기간이 지났는지 확인
        if (Duration.between(counter.resetTime, now).compareTo(config.period) > 0) {
            return config.maxRequests; // 기간이 지났으면 최대 요청 수 반환
        }

        return Math.max(0, config.maxRequests - counter.count.get());
    }

    private static class LimitConfig {
        final int maxRequests;
        final Duration period;

        LimitConfig(int maxRequests, Duration period) {
            this.maxRequests = maxRequests;
            this.period = period;
        }
    }

    private static class ServiceCounter {
        final AtomicInteger count = new AtomicInteger(0);
        Instant resetTime;

        ServiceCounter(Instant resetTime) {
            this.resetTime = resetTime;
        }
    }
}
