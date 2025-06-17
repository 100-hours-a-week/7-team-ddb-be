package com.dolpin.domain.place.service.cache;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import com.dolpin.domain.moment.repository.MomentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CachedPlaceSearchService {

    private final PlaceRepository placeRepository;
    private final PlaceCacheService placeCacheService;
    private final PlaceBookmarkQueryService bookmarkQueryService;
    private final MomentRepository momentRepository;

    @Value("${place.search.default-radius}")
    private double defaultSearchRadius;

    @Transactional(readOnly = true)
    public List<PlaceSearchResponse.PlaceDto> searchByCategoryWithCache(
            String category, Double lat, Double lng, Long userId) {

        long startTime = System.currentTimeMillis();

        // 1단계: 위치 기반 필터링 (캐시 불가 - 실시간 계산 필요)
        List<PlaceWithDistance> nearbyPlaces = placeRepository.findPlacesByCategoryWithinRadius(
                category, lat, lng, defaultSearchRadius);

        if (nearbyPlaces.isEmpty()) {
            log.debug("No places found within radius for category: {}", category);
            return Collections.emptyList();
        }

        List<Long> placeIds = nearbyPlaces.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        // 2단계: 기본 장소 정보 조회 (캐시 적용)
        List<Place> cachedPlaces = placeCacheService.getPlacesByIdsWithCache(placeIds);
        Map<Long, Place> placeMap = cachedPlaces.stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));

        // 3단계: 실시간 데이터 조회 (캐시 불가)
        Map<Long, Long> momentCountMap = getMomentCountMap(placeIds);
        Map<Long, Boolean> bookmarkStatusMap = bookmarkQueryService.getBookmarkStatusMap(userId, placeIds);

        // 4단계: 결과 조합
        List<PlaceSearchResponse.PlaceDto> result = nearbyPlaces.stream()
                .map(placeWithDistance -> {
                    Place place = placeMap.get(placeWithDistance.getId());
                    Boolean isBookmarked = bookmarkStatusMap.getOrDefault(placeWithDistance.getId(), false);

                    if (place != null) {
                        List<String> keywords = place.getKeywords().stream()
                                .map(pk -> pk.getKeyword().getKeyword())
                                .collect(Collectors.toList());

                        return convertToPlaceDto(placeWithDistance, keywords, momentCountMap, isBookmarked);
                    } else {
                        return convertToPlaceDto(placeWithDistance, Collections.emptyList(), momentCountMap, isBookmarked);
                    }
                })
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info("Category search completed in {}ms (cached: {}, total: {})",
                duration, cachedPlaces.size(), result.size());

        return result;
    }

    @Cacheable(value = "places-by-region",
            key = "#category + ':' + T(java.lang.Math).round(#lat * 100) + ':' + T(java.lang.Math).round(#lng * 100)")
    @Transactional(readOnly = true)
    public List<PlaceWithDistance> getPlacesByRegionWithCache(String category, Double lat, Double lng) {
        log.info("Fetching places for region cache: category={}, lat={}, lng={}", category,
                Math.round(lat * 100) / 100.0, Math.round(lng * 100) / 100.0);

        List<PlaceWithDistance> places = placeRepository.findPlacesByCategoryWithinRadius(
                category, lat, lng, defaultSearchRadius);

        log.info("Cached {} places for region", places.size());
        return places;
    }

    // 헬퍼 메서드들
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

    private PlaceSearchResponse.PlaceDto convertToPlaceDto(
            PlaceWithDistance placeWithDistance, List<String> keywords,
            Map<Long, Long> momentCountMap, Boolean isBookmarked) {

        Double convertedDistance = convertDistance(placeWithDistance.getDistance());
        Long momentCount = momentCountMap.getOrDefault(placeWithDistance.getId(), 0L);

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{
                placeWithDistance.getLongitude(),
                placeWithDistance.getLatitude()
        });

        return PlaceSearchResponse.PlaceDto.builder()
                .id(placeWithDistance.getId())
                .name(placeWithDistance.getName())
                .thumbnail(placeWithDistance.getImageUrl())
                .distance(convertedDistance)
                .momentCount(momentCount)
                .keywords(keywords)
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
