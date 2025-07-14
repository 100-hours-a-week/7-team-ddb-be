package com.dolpin.domain.place.service.strategy;

import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.factory.PlaceDtoFactory;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import com.dolpin.global.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AiQuerySearchStrategy implements PlaceSearchStrategy {

    private final PlaceAiClient placeAiClient;
    private final PlaceRepository placeRepository;
    private final MomentRepository momentRepository;
    private final PlaceBookmarkQueryService bookmarkQueryService;
    private final PlaceDtoFactory placeDtoFactory; // Factory 추가

    public AiQuerySearchStrategy(PlaceAiClient placeAiClient, PlaceRepository placeRepository, MomentRepository momentRepository, PlaceBookmarkQueryService bookmarkQueryService, PlaceDtoFactory placeDtoFactory) {
        this.placeAiClient = placeAiClient;
        this.placeRepository = placeRepository;
        this.momentRepository = momentRepository;
        this.bookmarkQueryService = bookmarkQueryService;
        this.placeDtoFactory = placeDtoFactory;
    }

    @Override
    public boolean supports(PlaceSearchType searchType) {
        return searchType == PlaceSearchType.AI_QUERY;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public Mono<List<PlaceSearchResponse.PlaceDto>> search(PlaceSearchContext context) {
        log.debug("AI 검색 시작: query={}", context.getQuery());

        return callAiService(context)
                .flatMap(aiResponse -> processAiResponse(aiResponse, context))
                .doOnSuccess(result -> log.debug("AI 검색 완료: 결과 수={}", result.size()));
    }

    private Mono<PlaceAiResponse> callAiService(PlaceSearchContext context) {
        if (context.getDevToken() != null) {
            return placeAiClient.recommendPlacesAsync(context.getQuery(), context.getDevToken());
        } else {
            return placeAiClient.recommendPlacesAsync(context.getQuery());
        }
    }

    private Mono<List<PlaceSearchResponse.PlaceDto>> processAiResponse(
            PlaceAiResponse aiResponse, PlaceSearchContext context) {

        return Mono.fromCallable(() -> {
            if (aiResponse.getRecommendations() != null && !aiResponse.getRecommendations().isEmpty()) {
                return processAiRecommendations(aiResponse, context);
            } else if (StringUtils.isNotBlank(aiResponse.getPlaceCategory())) {
                return processCategoryFallback(aiResponse.getPlaceCategory(), context);
            } else {
                return Collections.<PlaceSearchResponse.PlaceDto>emptyList();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private List<PlaceSearchResponse.PlaceDto> processAiRecommendations(
            PlaceAiResponse aiResponse, PlaceSearchContext context) {

        // null 값 필터링된 추천 목록만 사용
        List<PlaceAiResponse.PlaceRecommendation> validRecommendations = aiResponse.getRecommendations().stream()
                .filter(Objects::nonNull)
                .filter(rec -> rec.getId() != null)
                .collect(Collectors.toList());

        if (validRecommendations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> placeIds = validRecommendations.stream()
                .map(PlaceAiResponse.PlaceRecommendation::getId)
                .collect(Collectors.toList());

        // null 안전성을 위한 Map 생성
        Map<Long, Double> similarityScores = validRecommendations.stream()
                .filter(rec -> rec.getSimilarityScore() != null)
                .collect(Collectors.toMap(
                        PlaceAiResponse.PlaceRecommendation::getId,
                        PlaceAiResponse.PlaceRecommendation::getSimilarityScore,
                        (existing, replacement) -> existing
                ));

        Map<Long, List<String>> keywordsByPlaceId = validRecommendations.stream()
                .filter(rec -> rec.getKeyword() != null && !rec.getKeyword().isEmpty())
                .collect(Collectors.toMap(
                        PlaceAiResponse.PlaceRecommendation::getId,
                        PlaceAiResponse.PlaceRecommendation::getKeyword,
                        (existing, replacement) -> existing
                ));

        List<PlaceWithDistance> placesWithDistance = placeRepository.findPlacesWithinRadiusByIds(
                placeIds, context.getLat(), context.getLng(), 1000.0);

        if (placesWithDistance.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> foundPlaceIds = placesWithDistance.stream()
                .map(PlaceWithDistance::getId)
                .collect(Collectors.toList());

        List<Place> places = placeRepository.findByIdsWithKeywords(foundPlaceIds);
        Map<Long, Place> placeMap = places.stream()
                .collect(Collectors.toMap(Place::getId, place -> place));

        Map<Long, Long> momentCountMap = getMomentCountMap(foundPlaceIds);
        Map<Long, Boolean> bookmarkStatusMap = bookmarkQueryService
                .getBookmarkStatusMap(context.getUserId(), foundPlaceIds);

        // Factory를 사용한 DTO 생성
        return placesWithDistance.stream()
                .map(placeWithDistance -> {
                    Place place = placeMap.get(placeWithDistance.getId());
                    if (place == null) {
                        return null;
                    }

                    try {
                        return placeDtoFactory.createAiSearchDto(
                                place,
                                similarityScores.get(place.getId()),
                                keywordsByPlaceId.get(place.getId()),
                                momentCountMap,
                                bookmarkStatusMap
                        );
                    } catch (Exception e) {
                        log.error("DTO 생성 실패: placeId={}, error={}", place.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<PlaceSearchResponse.PlaceDto> processCategoryFallback(
            String category, PlaceSearchContext context) {

        log.debug("AI가 카테고리 추천: {}", category);

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

        // Factory를 사용한 DTO 생성
        return searchResults.stream()
                .map(placeWithDistance -> {
                    Place place = placeMap.get(placeWithDistance.getId());
                    if (place == null) {
                        return null;
                    }

                    return placeDtoFactory.createDistanceBasedDto(
                            place,
                            placeWithDistance.getDistance(),
                            momentCountMap,
                            bookmarkStatusMap
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

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
}
