package com.dolpin.domain.place.service.strategy;

import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.cache.PlaceCacheService;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CategorySearchStrategy implements PlaceSearchStrategy {

    private final PlaceRepository placeRepository;
    private final MomentRepository momentRepository;
    private final PlaceBookmarkQueryService bookmarkQueryService;
    private final PlaceCacheService placeCacheService;

    @Value("${place.search.default-radius}")
    private double defaultSearchRadius;

    public CategorySearchStrategy(PlaceRepository placeRepository,
                                  MomentRepository momentRepository,
                                  PlaceBookmarkQueryService bookmarkQueryService,
                                  PlaceCacheService placeCacheService) {
        this.placeRepository = placeRepository;
        this.momentRepository = momentRepository;
        this.bookmarkQueryService = bookmarkQueryService;
        this.placeCacheService = placeCacheService;
    }

    @Override
    public boolean supports(PlaceSearchType searchType) {
        return searchType == PlaceSearchType.CATEGORY;
    }

    @Override
    public int getPriority() {
        return 2; // AI 검색보다 낮은 우선순위
    }

    @Override
    public Mono<List<PlaceSearchResponse.PlaceDto>> search(PlaceSearchContext context) {
        log.debug("카테고리 검색 시작: category={}, lat={}, lng={}",
                context.getCategory(), context.getLat(), context.getLng());

        return Mono.fromCallable(() -> searchByCategory(context))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> log.debug("카테고리 검색 완료: 결과 수={}", result.size()))
                .doOnError(error -> log.error("카테고리 검색 실패: {}", error.getMessage()));
    }

    private List<PlaceSearchResponse.PlaceDto> searchByCategory(PlaceSearchContext context) {
        String category = context.getCategory();
        Double lat = context.getLat();
        Double lng = context.getLng();
        Long userId = context.getUserId();

        // 1. 캐시에서 조회
        List<PlaceCacheService.CategorySearchCacheItem> cachedItems =
                placeCacheService.getCachedCategorySearchResult(category, lat, lng);

        if (cachedItems != null) {
            return processCachedCategorySearchResult(cachedItems, userId);
        }

        // 2. 캐시 미스: DB 조회 후 캐시 저장
        return loadCategorySearchFromDbAndCache(category, lat, lng, userId);
    }

    private List<PlaceSearchResponse.PlaceDto> processCachedCategorySearchResult(
            List<PlaceCacheService.CategorySearchCacheItem> cachedItems, Long userId) {

        if (cachedItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> placeIds = cachedItems.stream()
                .map(PlaceCacheService.CategorySearchCacheItem::getPlaceId)
                .collect(Collectors.toList());

        Map<Long, Boolean> bookmarkStatusMap = bookmarkQueryService.getBookmarkStatusMap(userId, placeIds);

        return cachedItems.stream()
                .map(item -> convertCacheItemToDto(item, bookmarkStatusMap.getOrDefault(item.getPlaceId(), false)))
                .collect(Collectors.toList());
    }

    private List<PlaceSearchResponse.PlaceDto> loadCategorySearchFromDbAndCache(
            String category, Double lat, Double lng, Long userId) {

        log.debug("카테고리 검색 DB 조회: category={}, lat={}, lng={}", category, lat, lng);

        List<PlaceWithDistance> searchResults = placeRepository.findPlacesByCategoryWithinRadius(
                category, lat, lng, defaultSearchRadius);

        if (searchResults.isEmpty()) {
            placeCacheService.cacheCategorySearchResult(category, lat, lng, Collections.emptyList());
            return Collections.emptyList();
        }

        List<Long> placeIds = searchResults.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        Map<Long, Long> momentCountMap = getMomentCountMap(placeIds);
        Map<Long, Boolean> bookmarkStatusMap = bookmarkQueryService.getBookmarkStatusMap(userId, placeIds);
        List<Place> placesWithKeywords = placeRepository.findByIdsWithKeywords(placeIds);
        Map<Long, Place> placeMap = placesWithKeywords.stream()
                .collect(Collectors.toMap(Place::getId, place -> place));

        List<PlaceCacheService.CategorySearchCacheItem> cacheItems = searchResults.stream()
                .map(placeWithDistance -> {
                    Place place = placeMap.get(placeWithDistance.getId());
                    List<String> keywords = place != null ?
                            place.getKeywords().stream()
                                    .map(pk -> pk.getKeyword().getKeyword())
                                    .collect(Collectors.toList()) :
                            Collections.emptyList();

                    return PlaceCacheService.CategorySearchCacheItem.builder()
                            .placeId(placeWithDistance.getId())
                            .placeName(placeWithDistance.getName())  // name이 아닌 placeName
                            .thumbnail(placeWithDistance.getImageUrl())
                            .distance(convertDistance(placeWithDistance.getDistance()))
                            .longitude(placeWithDistance.getLongitude())
                            .latitude(placeWithDistance.getLatitude())
                            .category(category)
                            .keywords(keywords)
                            .momentCount(momentCountMap.getOrDefault(placeWithDistance.getId(), 0L))
                            .isBookmarked(null) // 캐시에는 북마크 상태 저장하지 않음
                            .build();
                })
                .collect(Collectors.toList());

        placeCacheService.cacheCategorySearchResult(category, lat, lng, cacheItems);

        return cacheItems.stream()
                .map(item -> convertCacheItemToDto(item, bookmarkStatusMap.getOrDefault(item.getPlaceId(), false)))
                .collect(Collectors.toList());
    }

    // TODO: 다음 단계에서 공통 유틸리티로 분리
    private Map<Long, Long> getMomentCountMap(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = momentRepository.countPublicMomentsByPlaceIds(placeIds);
        Map<Long, Long> momentCountMap = new HashMap<>();

        for (Object[] result : results) {
            Long placeId = (Long) result[0];
            Long count = (Long) result[1];
            momentCountMap.put(placeId, count);
        }

        return momentCountMap;
    }

    private PlaceSearchResponse.PlaceDto convertCacheItemToDto(
            PlaceCacheService.CategorySearchCacheItem cacheItem, Boolean isBookmarked) {

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{cacheItem.getLongitude(), cacheItem.getLatitude()});

        return PlaceSearchResponse.PlaceDto.builder()
                .id(cacheItem.getPlaceId())
                .name(cacheItem.getPlaceName())  // getName()이 아닌 getPlaceName()
                .thumbnail(cacheItem.getThumbnail())
                .distance(cacheItem.getDistance())
                .momentCount(cacheItem.getMomentCount())
                .keywords(cacheItem.getKeywords())
                .location(locationMap)
                .isBookmarked(isBookmarked)
                .similarityScore(null)
                .build();
    }

    private Double convertDistance(Double distanceInMeters) {
        if (distanceInMeters == null) return 0.0;

        if (distanceInMeters < 1000) {
            return (double) Math.round(distanceInMeters);
        } else {
            return BigDecimal.valueOf(distanceInMeters / 1000.0)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }
}
