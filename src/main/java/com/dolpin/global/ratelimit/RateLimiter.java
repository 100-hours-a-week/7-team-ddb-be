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

    public void init() {
        registerLimit("ai-service", aiServiceMaxRequests, Duration.ofSeconds(aiServicePeriodSeconds));
    }

    public void registerLimit(String serviceName, int maxRequests, Duration period) {
        limitConfigs.put(serviceName, new LimitConfig(maxRequests, period));
        counters.put(serviceName, new ServiceCounter(Instant.now()));
        log.info("Rate limit registered for {}: {} requests per {}",
                serviceName, maxRequests, period);
    }

    public boolean allowRequest(String serviceName) {
        LimitConfig config = limitConfigs.get(serviceName);
        if (config == null) {
            log.warn("No rate limit config found for service: {}", serviceName);
            return true;
        }

        ServiceCounter counter = counters.get(serviceName);
        Instant now = Instant.now();

        // 제한 기간이 지났으면 카운터 리셋
        if (Duration.between(counter.resetTime, now).compareTo(config.period) > 0) {
            counter.count.set(0);
            counter.resetTime = now;
            log.debug("Reset counter for service: {}", serviceName);
        }

        // 현재 카운트가 최대 요청 수보다 작으면 허용
        if (counter.count.incrementAndGet() <= config.maxRequests) {
            log.debug("Request allowed for {}: {}/{}",
                    serviceName, counter.count.get(), config.maxRequests);
            return true;
        } else {
            log.warn("Request denied for {}: Rate limit exceeded ({}/{})",
                    serviceName, counter.count.get(), config.maxRequests);
            return false;
        }
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