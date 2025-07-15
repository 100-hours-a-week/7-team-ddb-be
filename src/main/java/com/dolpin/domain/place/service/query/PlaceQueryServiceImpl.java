package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.PlaceBusinessStatusResponse;
import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.cache.PlaceCacheService;
import com.dolpin.domain.place.service.strategy.PlaceSearchContext;
import com.dolpin.domain.place.service.strategy.PlaceSearchStrategy;
import com.dolpin.domain.place.service.strategy.PlaceSearchStrategyFactory;
import com.dolpin.domain.place.service.strategy.PlaceSearchType;
import com.dolpin.domain.place.service.template.FullPlaceDetailQuery;
import com.dolpin.domain.place.service.template.SimpleBusinessStatusQuery;
import com.dolpin.domain.place.service.template.SimplePlaceDetailQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceQueryServiceImpl implements PlaceQueryService {

    // 기존 의존성들
    private final PlaceRepository placeRepository;
    private final PlaceCacheService placeCacheService;

    // Template Method 구현체들
    private final FullPlaceDetailQuery fullPlaceDetailQuery;
    private final SimplePlaceDetailQuery simplePlaceDetailQuery;

    private final PlaceSearchStrategyFactory placeSearchStrategyFactory;

    @Value("${place.search.default-radius}")
    private double defaultSearchRadius;

    @Override
    @Transactional(readOnly = true)
    public PlaceCategoryResponse getAllCategories() {
        // 1. 캐시에서 조회
        List<String> cachedCategories = placeCacheService.getCachedCategories();
        if (cachedCategories != null) {
            log.debug("카테고리 목록 캐시 히트: count={}", cachedCategories.size());
            return PlaceCategoryResponse.builder()
                    .categories(cachedCategories)
                    .build();
        }

        // 2. 캐시 미스: DB 조회 후 캐시 저장
        log.debug("카테고리 목록 DB 조회");
        List<String> categories = placeRepository.findDistinctCategories();

        // 3. 캐시 저장 (비동기)
        placeCacheService.cacheCategories(categories);

        return PlaceCategoryResponse.builder()
                .categories(categories)
                .build();
    }

    @Override
    public Mono<PlaceSearchResponse> searchPlacesAsync(String query, Double lat, Double lng, String category, Long userId) {
        return executeSearchLogicAsync(query, lat, lng, category, userId, null);
    }

    @Override
    public Mono<PlaceSearchResponse> searchPlacesWithDevTokenAsync(String query, Double lat, Double lng, String category, String devToken, Long userId) {
        return executeSearchLogicAsync(query, lat, lng, category, userId, devToken);
    }

    private Mono<PlaceSearchResponse> executeSearchLogicAsync(String query, Double lat, Double lng, String category, Long userId, String devToken) {
        PlaceSearchContext context = PlaceSearchContext.builder()
                .query(query)
                .lat(lat)
                .lng(lng)
                .category(category)
                .userId(userId)
                .devToken(devToken)
                .build();

        return Mono.fromCallable(() -> {
                    context.validate(); // 파라미터 검증
                    return context;
                })
                .flatMap(validContext -> {
                    PlaceSearchType searchType = validContext.determineSearchType();
                    PlaceSearchStrategy strategy = placeSearchStrategyFactory.getStrategy(searchType);
                    return strategy.search(validContext);
                })
                .map(placeDtos -> PlaceSearchResponse.builder()
                        .total(placeDtos.size())
                        .places(placeDtos)
                        .build())
                .doOnSuccess(response -> log.debug("검색 완료: 총 {}개 결과", response.getTotal()))
                .doOnError(error -> log.error("검색 실패: {}", error.getMessage()));
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceDetailResponse getPlaceDetail(Long placeId, Long userId) {
        return fullPlaceDetailQuery.getPlaceDetail(placeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceDetailResponse getPlaceDetailWithoutBookmark(Long placeId) {
        return simplePlaceDetailQuery.getPlaceDetail(placeId, null);
    }

    private final SimpleBusinessStatusQuery simpleBusinessStatusQuery;

    @Override
    @Transactional(readOnly = true)
    public PlaceBusinessStatusResponse getPlaceBusinessStatus(Long placeId) {
        return simpleBusinessStatusQuery.getBusinessStatus(placeId);
    }
}
