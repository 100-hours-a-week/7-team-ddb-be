// src/main/java/com/dolpin/domain/moment/service/cache/MomentCacheService.java
package com.dolpin.domain.moment.service.cache;

import com.dolpin.global.redis.service.RedisService;
import com.dolpin.global.redis.util.CacheKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentCacheService {

    private final RedisService redisService;

    // TTL 설정
    private static final Duration COMMENT_COUNT_TTL = Duration.ofHours(1);
    private static final Duration VIEW_COUNT_TTL = Duration.ofMinutes(30);

    /**
     * 댓글 수 조회
     */
    public Long getCommentCount(Long momentId) {
        String key = CacheKeyUtil.commentCount(momentId);
        try {
            return redisService.get(key, Long.class);
        } catch (Exception e) {
            log.warn("댓글 수 조회 실패: momentId={}", momentId, e);
            return null;
        }
    }

    /**
     * 여러 기록의 댓글 수 배치 조회
     */
    public Map<Long, Long> getCommentCounts(List<Long> momentIds) {
        if (momentIds == null || momentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> keys = momentIds.stream()
                    .map(CacheKeyUtil::commentCount)
                    .collect(Collectors.toList());

            List<Object> values = redisService.multiGet(keys);
            Map<Long, Long> result = new HashMap<>();

            for (int i = 0; i < momentIds.size(); i++) {
                Object value = values.get(i);
                if (value instanceof Number) {
                    result.put(momentIds.get(i), ((Number) value).longValue());
                }
            }

            log.debug("댓글 수 배치 조회: {}/{} 히트", result.size(), momentIds.size());
            return result;
        } catch (Exception e) {
            log.warn("댓글 수 배치 조회 실패", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 댓글 수 배치 캐싱
     */
    public void cacheCommentCountsBatch(Map<Long, Long> commentCounts) {
        if (commentCounts.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> cacheData = commentCounts.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> CacheKeyUtil.commentCount(entry.getKey()),
                            Map.Entry::getValue
                    ));

            redisService.batchSetWithTtlSimple(cacheData, COMMENT_COUNT_TTL);
            log.debug("댓글 수 배치 캐시: count={}", commentCounts.size());
        } catch (Exception e) {
            log.warn("댓글 수 배치 캐시 실패: count={}", commentCounts.size(), e);
        }
    }

    // ===================== 조회 수 캐시 (실제 사용) =====================

    /**
     * 조회 수 캐시 저장 (비동기)
     */
    @Async("bookmarkCacheExecutor")
    public void cacheViewCount(Long momentId, Long viewCount) {
        String key = CacheKeyUtil.momentViewCount(momentId);
        try {
            redisService.set(key, viewCount, VIEW_COUNT_TTL);
            log.debug("조회 수 캐시: momentId={}, count={}", momentId, viewCount);
        } catch (Exception e) {
            log.warn("조회 수 캐시 실패: momentId={}", momentId, e);
        }
    }

    /**
     * 조회 수 조회
     */
    public Long getViewCount(Long momentId) {
        String key = CacheKeyUtil.momentViewCount(momentId);
        try {
            return redisService.get(key, Long.class);
        } catch (Exception e) {
            log.warn("조회 수 조회 실패: momentId={}", momentId, e);
            return null;
        }
    }

    /**
     * 여러 기록의 조회 수 배치 조회
     */
    public Map<Long, Long> getViewCounts(List<Long> momentIds) {
        if (momentIds == null || momentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> keys = momentIds.stream()
                    .map(CacheKeyUtil::momentViewCount)
                    .collect(Collectors.toList());

            List<Object> values = redisService.multiGet(keys);
            Map<Long, Long> result = new HashMap<>();

            for (int i = 0; i < momentIds.size(); i++) {
                Object value = values.get(i);
                if (value instanceof Number) {
                    result.put(momentIds.get(i), ((Number) value).longValue());
                }
            }

            log.debug("조회 수 배치 조회: {}/{} 히트", result.size(), momentIds.size());
            return result;
        } catch (Exception e) {
            log.warn("조회 수 배치 조회 실패", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 조회 수 배치 캐싱
     */
    public void cacheViewCountsBatch(Map<Long, Long> viewCounts) {
        if (viewCounts.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> cacheData = viewCounts.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> CacheKeyUtil.momentViewCount(entry.getKey()),
                            Map.Entry::getValue
                    ));

            redisService.batchSetWithTtlSimple(cacheData, VIEW_COUNT_TTL);
            log.debug("조회 수 배치 캐시: count={}", viewCounts.size());
        } catch (Exception e) {
            log.warn("조회 수 배치 캐시 실패: count={}", viewCounts.size(), e);
        }
    }

    /**
     * 댓글 수 캐시 무효화
     */
    public void invalidateCommentCount(Long momentId) {
        String key = CacheKeyUtil.commentCount(momentId);
        try {
            redisService.delete(key);
            log.debug("댓글 수 캐시 무효화: momentId={}", momentId);
        } catch (Exception e) {
            log.warn("댓글 수 캐시 무효화 실패: momentId={}", momentId, e);
        }
    }
}
