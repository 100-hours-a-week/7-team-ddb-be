package com.dolpin.global.scheduler;

import com.dolpin.global.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleCacheCleanupScheduler {

    private final RedisService redisService;

    /**
     * 매일 새벽 4시 - 만료된 캐시만 정리 (AI 스케줄러 1시간 전)
     */
    @Scheduled(cron = "0 5 1 * * *")
    public void cleanupExpiredCaches() {
        try {
            log.info("Starting cache cleanup");

            int totalCleaned = 0;

            // 1. 북마크 상태 캐시 정리
            totalCleaned += cleanExpiredKeys("bookmark:status:*");

            // 2. 댓글 수 캐시 정리
            totalCleaned += cleanExpiredKeys("comment:count:*");

            // 3. 조회 수 캐시 정리
            totalCleaned += cleanExpiredKeys("moment:view:*");

            log.info("Cache cleanup completed - cleaned {} keys", totalCleaned);

        } catch (Exception e) {
            log.error("Cache cleanup failed", e);
            // 실패해도 스케줄러는 계속 동작
        }
    }

    private int cleanExpiredKeys(String pattern) {
        try {
            Set<String> keys = redisService.getKeysByPattern(pattern);
            int cleaned = 0;

            for (String key : keys) {
                // TTL이 0 이하면 만료된 키
                Long ttl = redisService.getExpire(key);
                if (ttl != null && ttl <= 0) {
                    redisService.delete(key);
                    cleaned++;
                }
            }

            if (cleaned > 0) {
                log.debug("Cleaned {} expired keys for pattern: {}", cleaned, pattern);
            }

            return cleaned;

        } catch (Exception e) {
            log.warn("Failed to clean keys for pattern: {}", pattern, e);
            return 0;
        }
    }
}
