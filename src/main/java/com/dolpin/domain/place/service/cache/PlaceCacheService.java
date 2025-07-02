// src/main/java/com/dolpin/domain/place/service/cache/PlaceCacheService.java
package com.dolpin.domain.place.service.cache;

import com.dolpin.global.redis.service.RedisService;
import com.dolpin.global.redis.util.CacheKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceCacheService {

    private final RedisService redisService;

    // TTL 설정 - 카테고리는 자주 변경되지 않으므로 24시간
    private static final Duration CATEGORIES_TTL = Duration.ofHours(24);
    private static final Duration CATEGORY_SEARCH_TTL = Duration.ofMinutes(30);

    // ===================== 카테고리 캐시 =====================

    /**
     * 카테고리 목록 캐시 저장 (비동기)
     */
    @Async("bookmarkCacheExecutor")
    public void cacheCategories(List<String> categories) {
        String key = CacheKeyUtil.placeCategories();
        try {
            redisService.set(key, categories, CATEGORIES_TTL);
            log.debug("카테고리 목록 캐시 저장: count={}", categories.size());
        } catch (Exception e) {
            log.warn("카테고리 목록 캐시 저장 실패", e);
        }
    }

    /**
     * 카테고리 목록 캐시 조회
     */
    @SuppressWarnings("unchecked")
    public List<String> getCachedCategories() {
        String key = CacheKeyUtil.placeCategories();
        try {
            Object cached = redisService.get(key);
            if (cached instanceof List) {
                List<String> categories = (List<String>) cached;
                log.debug("카테고리 목록 캐시 히트: count={}", categories.size());
                return categories;
            }
        } catch (Exception e) {
            log.warn("카테고리 목록 캐시 조회 실패", e);
        }
        return null; // 캐시 미스
    }

    /**
     * 카테고리 캐시 무효화 (비동기)
     */
    @Async("bookmarkCacheExecutor")
    public void invalidateCategories() {
        String key = CacheKeyUtil.placeCategories();
        try {
            redisService.delete(key);
            log.debug("카테고리 캐시 무효화 완료");
        } catch (Exception e) {
            log.warn("카테고리 캐시 무효화 실패", e);
        }
    }

    // ===================== 카테고리 검색 캐시 =====================

    /**
     * 카테고리별 검색 결과 캐시 저장 (비동기)
     */
    @Async("bookmarkCacheExecutor")
    public void cacheCategorySearchResult(String category, Double lat, Double lng,
                                          List<CategorySearchCacheItem> searchResult) {
        String key = CacheKeyUtil.placeRegion(category, lat, lng);
        try {
            redisService.set(key, searchResult, CATEGORY_SEARCH_TTL);
            log.debug("카테고리 검색 결과 캐시 저장: category={}, lat={}, lng={}, count={}",
                    category, lat, lng, searchResult.size());
        } catch (Exception e) {
            log.warn("카테고리 검색 결과 캐시 저장 실패: category={}, lat={}, lng={}",
                    category, lat, lng, e);
        }
    }

    /**
     * 카테고리별 검색 결과 캐시 조회
     */
    @SuppressWarnings("unchecked")
    public List<CategorySearchCacheItem> getCachedCategorySearchResult(String category, Double lat, Double lng) {
        String key = CacheKeyUtil.placeRegion(category, lat, lng);
        try {
            Object cached = redisService.get(key);
            if (cached instanceof List) {
                List<CategorySearchCacheItem> result = (List<CategorySearchCacheItem>) cached;
                log.debug("카테고리 검색 결과 캐시 히트: category={}, lat={}, lng={}, count={}",
                        category, lat, lng, result.size());
                return result;
            }
        } catch (Exception e) {
            log.warn("카테고리 검색 결과 캐시 조회 실패: category={}, lat={}, lng={}",
                    category, lat, lng, e);
        }
        return null; // 캐시 미스
    }

    /**
     * 특정 카테고리의 모든 캐시 무효화 (비동기)
     */
    @Async("bookmarkCacheExecutor")
    public void invalidateCategorySearchCache(String category) {
        String pattern = "place:region:" + category + ":*";
        try {
            redisService.deleteByPattern(pattern);
            log.debug("카테고리 검색 캐시 무효화 완료: category={}", category);
        } catch (Exception e) {
            log.warn("카테고리 검색 캐시 무효화 실패: category={}", category, e);
        }
    }

    // ===================== 캐시 아이템 클래스 =====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CategorySearchCacheItem {
        private Long placeId;
        private String placeName;
        private String thumbnail;
        private Double distance;
        private Double longitude;
        private Double latitude;
        private String category;
        private List<String> keywords;
        private Long momentCount;
        private Boolean isBookmarked;
    }

}
