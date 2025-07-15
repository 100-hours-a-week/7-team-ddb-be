package com.dolpin.domain.place.service.strategy;

import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import com.dolpin.global.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiQuerySearchStrategy implements PlaceSearchStrategy {

    private final PlaceAiClient placeAiClient;
    private final PlaceRepository placeRepository;
    private final MomentRepository momentRepository;
    private final PlaceBookmarkQueryService bookmarkQueryService;

    public AiQuerySearchStrategy(PlaceAiClient placeAiClient,
                                 PlaceRepository placeRepository,
                                 MomentRepository momentRepository,
                                 PlaceBookmarkQueryService bookmarkQueryService) {
        this.placeAiClient = placeAiClient;
        this.placeRepository = placeRepository;
        this.momentRepository = momentRepository;
        this.bookmarkQueryService = bookmarkQueryService;
    }

    @Override
    public boolean supports(PlaceSearchType searchType) {
        return searchType == PlaceSearchType.AI_QUERY;
    }

    @Override
    public int getPriority() {
        return 1; // 높은 우선순위
    }

    @Override
    public Mono<List<PlaceSearchResponse.PlaceDto>> search(PlaceSearchContext context) {
        log.debug("AI 검색 시작: query={}, lat={}, lng={}",
                context.getQuery(), context.getLat(), context.getLng());

        return callAiService(context)
                .flatMap(aiResponse -> processAiResponse(aiResponse, context))
                .doOnSuccess(result -> log.debug("AI 검색 완료: 결과 수={}", result.size()))
                .doOnError(error -> log.error("AI 검색 실패: {}", error.getMessage()));
    }

    private Mono<PlaceAiResponse> callAiService(PlaceSearchContext context) {
        if (context.getDevToken() != null) {
            return placeAiClient.recommendPlacesAsync(
                    context.getQuery(), context.getDevToken());
        } else {
            return placeAiClient.recommendPlacesAsync(
                    context.getQuery());
        }
    }

    private Mono<List<PlaceSearchResponse.PlaceDto>> processAiResponse(
            PlaceAiResponse aiResponse, PlaceSearchContext context) {

        return Mono.fromCallable(() -> {
            if (aiResponse.getRecommendations() != null && !aiResponse.getRecommendations().isEmpty()) {
                // AI가 특정 장소들을 추천한 경우
                return processAiRecommendations(aiResponse, context);
            } else if (StringUtils.isNotBlank(aiResponse.getPlaceCategory())) {
                // AI가 카테고리를 추천한 경우
                return processCategoryFallback(aiResponse.getPlaceCategory(), context);
            } else {
                return Collections.<PlaceSearchResponse.PlaceDto>emptyList();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private List<PlaceSearchResponse.PlaceDto> processAiRecommendations(
            PlaceAiResponse aiResponse, PlaceSearchContext context) {

        List<Long> placeIds = aiResponse.getRecommendations().stream()
                .map(PlaceAiResponse.PlaceRecommendation::getId)
                .collect(Collectors.toList());

        Map<Long, Double> similarityScores = aiResponse.getRecommendations().stream()
                .collect(Collectors.toMap(
                        PlaceAiResponse.PlaceRecommendation::getId,
                        PlaceAiResponse.PlaceRecommendation::getSimilarityScore
                ));

        Map<Long, List<String>> keywordsByPlaceId = aiResponse.getRecommendations().stream()
                .collect(Collectors.toMap(
                        PlaceAiResponse.PlaceRecommendation::getId,
                        PlaceAiResponse.PlaceRecommendation::getKeyword
                ));

        // DB에서 반경 내 장소 정보 조회
        List<PlaceWithDistance> placesWithDistance = placeRepository.findPlacesWithinRadiusByIds(
                placeIds, context.getLat(), context.getLng(), 1000.0);

        if (placesWithDistance.isEmpty()) {
            return Collections.emptyList();
        }

        // Place 객체를 따로 조회해야 할 수도 있음 (키워드 정보 포함)
        List<Long> foundPlaceIds = placesWithDistance.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        List<Place> places = placeRepository.findByIdsWithKeywords(foundPlaceIds);
        Map<Long, Place> placeMap = places.stream()
                .collect(Collectors.toMap(Place::getId, place -> place));

        Map<Long, Long> momentCountMap = getMomentCountMap(foundPlaceIds);
        Map<Long, Boolean> bookmarkStatusMap = bookmarkQueryService
                .getBookmarkStatusMap(context.getUserId(), foundPlaceIds);

        // DTO 변환
        return placesWithDistance.stream()
                .map(placeWithDistance -> {
                    Place place = placeMap.get(placeWithDistance.getId());
                    if (place == null) {
                        return null;
                    }
                    return convertToPlaceDto(
                            place,
                            placeWithDistance.getDistance(),
                            similarityScores.get(place.getId()),
                            keywordsByPlaceId.get(place.getId()),
                            momentCountMap,
                            bookmarkStatusMap.getOrDefault(place.getId(), false)
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<PlaceSearchResponse.PlaceDto> processCategoryFallback(
            String category, PlaceSearchContext context) {

        log.debug("AI가 카테고리 추천: {}", category);

        // 카테고리 검색으로 폴백 (기존 로직 재사용)
        List<PlaceWithDistance> searchResults = placeRepository.findPlacesByCategoryWithinRadius(
                category, context.getLat(), context.getLng(), 1000.0);

        if (searchResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> placeIds = searchResults.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        Map<Long, Long> momentCountMap = getMomentCountMap(placeIds);
        Map<Long, Boolean> bookmarkStatusMap = bookmarkQueryService
                .getBookmarkStatusMap(context.getUserId(), placeIds);

        List<Place> placesWithKeywords = placeRepository.findByIdsWithKeywords(placeIds);
        Map<Long, Place> placeMap = placesWithKeywords.stream()
                .collect(Collectors.toMap(Place::getId, place -> place));

        return searchResults.stream()
                .map(placeWithDistance -> {
                    Place place = placeMap.get(placeWithDistance.getId());
                    return convertToPlaceDto(
                            place,
                            placeWithDistance.getDistance(),
                            null, // AI similarity score 없음
                            null, // AI keywords 없음
                            momentCountMap,
                            bookmarkStatusMap.getOrDefault(place.getId(), false)
                    );
                })
                .collect(Collectors.toList());
    }

    // TODO: 다음 단계에서 별도 클래스로 분리 예정
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

    // TODO: 다음 단계에서 Factory 패턴으로 분리 예정
    private PlaceSearchResponse.PlaceDto convertToPlaceDto(Place place, Double distance,
                                                           Double similarityScore, List<String> aiKeywords,
                                                           Map<Long, Long> momentCountMap, Boolean isBookmarked) {
        // 기존 convertToPlaceDto 로직 그대로 사용
        Double convertedDistance = convertDistance(distance);
        Long momentCount = momentCountMap.getOrDefault(place.getId(), 0L);

        List<String> keywords;
        if (aiKeywords != null && !aiKeywords.isEmpty()) {
            keywords = aiKeywords;
        } else {
            keywords = place.getKeywords().stream()
                    .map(pk -> pk.getKeyword().getKeyword())
                    .collect(Collectors.toList());
        }

        Point location = place.getLocation();
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});

        return PlaceSearchResponse.PlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .thumbnail(place.getImageUrl())
                .distance(convertedDistance)
                .momentCount(momentCount)
                .keywords(keywords)
                .location(locationMap)
                .isBookmarked(isBookmarked)
                .similarityScore(similarityScore)
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
