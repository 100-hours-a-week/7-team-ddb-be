package com.dolpin.domain.place.service.cache;

import com.dolpin.global.redis.service.RedisService;
import com.dolpin.global.redis.util.CacheKeyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkCacheService {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    // TTL 설정
    private static final Duration BOOKMARK_STATUS_TTL = Duration.ofHours(1);      // 개별 북마크 상태
    private static final Duration BOOKMARK_LIST_TTL = Duration.ofMinutes(15);     // 북마크 목록

    // ===================== 1. 개별 북마크 상태 캐시 =====================

    /**
     * 북마크 상태 캐시 저장 (비동기)
     */
    @Async("bookmarkCacheExecutor")
    public void cacheBookmarkStatus(Long userId, Long placeId, boolean isBookmarked) {
        String key = CacheKeyUtil.bookmarkStatus(userId, placeId);
        try {
            redisService.set(key, isBookmarked, BOOKMARK_STATUS_TTL);
            log.debug("북마크 상태 캐시: userId={}, placeId={}, status={}", userId, placeId, isBookmarked);
        } catch (Exception e) {
            log.warn("북마크 상태 캐시 실패: userId={}, placeId={}", userId, placeId, e);
        }
    }

    /**
     * 북마크 상태 조회
     */
    public Boolean getBookmarkStatus(Long userId, Long placeId) {
        String key = CacheKeyUtil.bookmarkStatus(userId, placeId);
        try {
            return redisService.get(key, Boolean.class);
        } catch (Exception e) {
            log.warn("북마크 상태 조회 실패: userId={}, placeId={}", userId, placeId, e);
            return null;
        }
    }

    /**
     * 여러 장소의 북마크 상태 배치 조회
     */
    public Map<Long, Boolean> getBookmarkStatuses(Long userId, List<Long> placeIds) {
        if (userId == null || placeIds == null || placeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> keys = placeIds.stream()
                    .map(placeId -> CacheKeyUtil.bookmarkStatus(userId, placeId))
                    .collect(Collectors.toList());

            List<Object> values = redisService.multiGet(keys);
            Map<Long, Boolean> result = new HashMap<>();

            for (int i = 0; i < placeIds.size(); i++) {
                Object value = values.get(i);
                if (value instanceof Boolean) {
                    result.put(placeIds.get(i), (Boolean) value);
                }
            }

            log.debug("북마크 상태 배치 조회: userId={}, {}/{} 히트", userId, result.size(), placeIds.size());
            return result;
        } catch (Exception e) {
            log.warn("북마크 상태 배치 조회 실패: userId={}", userId, e);
            return Collections.emptyMap();
        }
    }

    // ===================== 2. 북마크 목록 캐시 =====================

    /**
     * 북마크 목록 캐시 저장 (비동기)
     */
    @Async("bookmarkCacheExecutor")
    public void cacheUserBookmarkList(Long userId, List<UserBookmarkCacheItem> bookmarkItems) {
        String key = CacheKeyUtil.generateCompositeKey("bookmark:list", userId);
        try {
            String jsonData = objectMapper.writeValueAsString(bookmarkItems);
            redisService.set(key, jsonData, BOOKMARK_LIST_TTL);
            log.debug("북마크 목록 캐시 저장: userId={}, count={}", userId, bookmarkItems.size());
        } catch (Exception e) {
            log.warn("북마크 목록 캐시 저장 실패: userId={}", userId, e);
        }
    }

    /**
     * 북마크 목록 캐시 조회
     */
    public List<UserBookmarkCacheItem> getCachedUserBookmarkList(Long userId) {
        String key = CacheKeyUtil.generateCompositeKey("bookmark:list", userId);
        try {
            Object data = redisService.get(key);
            if (data != null) {
                String jsonData = data.toString();
                UserBookmarkCacheItem[] items = objectMapper.readValue(jsonData, UserBookmarkCacheItem[].class);
                List<UserBookmarkCacheItem> result = Arrays.asList(items);
                log.debug("북마크 목록 캐시 히트: userId={}, count={}", userId, result.size());
                return result;
            }
        } catch (Exception e) {
            log.warn("북마크 목록 캐시 조회 실패: userId={}", userId, e);
        }
        return null; // 캐시 미스
    }

    /**
     * 북마크 목록 캐시 무효화 (비동기)
     */
    @Async("bookmarkCacheExecutor")
    public void invalidateUserBookmarkList(Long userId) {
        String key = CacheKeyUtil.generateCompositeKey("bookmark:list", userId);
        try {
            redisService.delete(key);
            log.debug("북마크 목록 캐시 무효화: userId={}", userId);
        } catch (Exception e) {
            log.warn("북마크 목록 캐시 무효화 실패: userId={}", userId, e);
        }
    }

    // ===================== 캐시 아이템 클래스 =====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserBookmarkCacheItem {
        private Long placeId;
        private String placeName;
        private String thumbnail;
        private List<String> keywords;
        private LocalDateTime bookmarkCreatedAt;
    }

    // ===================== 배치 처리 메서드 추가 =====================

    /**
     * 북마크 상태 배치 캐싱 (동기)
     */
    public void cacheBookmarkStatusesBatch(Long userId, Map<Long, Boolean> bookmarkStatuses) {
        if (bookmarkStatuses.isEmpty()) return;

        int chunkSize = 20;
        List<Map.Entry<Long, Boolean>> entries = new ArrayList<>(bookmarkStatuses.entrySet());

        for (int i = 0; i < entries.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, entries.size());
            Map<String, Object> chunkData = new HashMap<>();

            for (int j = i; j < end; j++) {
                Map.Entry<Long, Boolean> entry = entries.get(j);
                String key = CacheKeyUtil.bookmarkStatus(userId, entry.getKey());
                chunkData.put(key, entry.getValue());
            }

            try {
                redisService.batchSetWithTtlSimple(chunkData, BOOKMARK_STATUS_TTL);
                log.debug("북마크 상태 청크 캐시: userId={}, chunk={}/{}",
                        userId, (i/chunkSize + 1), (entries.size() + chunkSize - 1) / chunkSize);
            } catch (Exception e) {
                log.warn("북마크 상태 청크 캐시 실패: userId={}, chunk={}", userId, i/chunkSize + 1, e);

            }
        }
    }

    /**
     * 청크 단위로 분할하여 비동기 배치 처리
     */
    @Async("bookmarkCacheExecutor")
    public void cacheBookmarkStatusesAsync(Long userId, Map<Long, Boolean> bookmarkStatuses) {
        // 큰 배치를 작은 청크로 분할
        int chunkSize = 50;
        List<Long> placeIds = new ArrayList<>(bookmarkStatuses.keySet());

        for (int i = 0; i < placeIds.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, placeIds.size());
            List<Long> chunk = placeIds.subList(i, end);

            Map<Long, Boolean> chunkData = chunk.stream()
                    .collect(Collectors.toMap(
                            placeId -> placeId,
                            placeId -> bookmarkStatuses.get(placeId)
                    ));

            cacheBookmarkStatusesBatch(userId, chunkData);
        }
    }
}
