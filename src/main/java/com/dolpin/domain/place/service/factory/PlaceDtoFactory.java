package com.dolpin.domain.place.service.factory;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.service.cache.PlaceCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

@Slf4j
@Component
public class PlaceDtoFactory {

    private final List<PlaceDtoStrategy> strategies;

    public PlaceDtoFactory(List<PlaceDtoStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(PlaceDtoStrategy::getPriority))
                .collect(Collectors.toList());

        log.info("Place DTO 생성 전략 등록 완료: {}",
                strategies.stream()
                        .map(s -> s.getClass().getSimpleName())
                        .collect(Collectors.joining(", ")));
    }

    public PlaceSearchResponse.PlaceDto createPlaceDto(PlaceDtoContext context) {
        PlaceDtoStrategy strategy = strategies.stream()
                .filter(s -> s.supports(context))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("지원하는 DTO 생성 전략을 찾을 수 없습니다"));

        log.debug("Place DTO 생성: strategy={}, placeId={}",
                strategy.getClass().getSimpleName(), context.getPlace().getId());

        return strategy.createPlaceDto(context);
    }

    public PlaceSearchResponse.PlaceDto createPlaceDto(Place place, Map<Long, Long> momentCountMap,
                                                       Map<Long, Boolean> bookmarkStatusMap) {
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(place)
                .momentCount(momentCountMap.getOrDefault(place.getId(), 0L))
                .isBookmarked(bookmarkStatusMap.getOrDefault(place.getId(), false))
                .build();

        return createPlaceDto(context);
    }

    public PlaceSearchResponse.PlaceDto createAiSearchDto(Place place, Double similarityScore,
                                                          List<String> aiKeywords, Map<Long, Long> momentCountMap,
                                                          Map<Long, Boolean> bookmarkStatusMap) {
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(place)
                .similarityScore(similarityScore)
                .aiKeywords(aiKeywords)
                .momentCount(momentCountMap.getOrDefault(place.getId(), 0L))
                .isBookmarked(bookmarkStatusMap.getOrDefault(place.getId(), false))
                .build();

        return createPlaceDto(context);
    }

    public PlaceSearchResponse.PlaceDto createDistanceBasedDto(Place place, Double distance,
                                                               Map<Long, Long> momentCountMap,
                                                               Map<Long, Boolean> bookmarkStatusMap) {
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(place)
                .distance(distance)
                .momentCount(momentCountMap.getOrDefault(place.getId(), 0L))
                .isBookmarked(bookmarkStatusMap.getOrDefault(place.getId(), false))
                .build();

        return createPlaceDto(context);
    }

    public PlaceSearchResponse.PlaceDto createFromCacheItem(PlaceCacheService.CategorySearchCacheItem cacheItem,
                                                            Boolean isBookmarked) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{cacheItem.getLongitude(), cacheItem.getLatitude()});

        return PlaceSearchResponse.PlaceDto.builder()
                .id(cacheItem.getPlaceId())
                .name(cacheItem.getPlaceName())
                .thumbnail(cacheItem.getThumbnail())
                .distance(cacheItem.getDistance())
                .momentCount(cacheItem.getMomentCount())
                .keywords(cacheItem.getKeywords())
                .location(locationMap)
                .isBookmarked(isBookmarked)
                .similarityScore(null)
                .build();
    }
}
