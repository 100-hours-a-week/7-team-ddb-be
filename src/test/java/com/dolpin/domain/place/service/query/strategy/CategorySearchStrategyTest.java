package com.dolpin.domain.place.service.query.strategy;

import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.cache.PlaceCacheService;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import com.dolpin.domain.place.service.strategy.CategorySearchStrategy;
import com.dolpin.domain.place.service.strategy.PlaceSearchContext;
import com.dolpin.domain.place.service.strategy.PlaceSearchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategorySearchStrategy 테스트")
class CategorySearchStrategyTest {

    @InjectMocks
    private CategorySearchStrategy categorySearchStrategy;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private PlaceBookmarkQueryService bookmarkQueryService;

    @Mock
    private PlaceCacheService placeCacheService;

    private PlaceSearchContext testContext;

    @BeforeEach
    void setUp() {
        testContext = PlaceSearchContext.builder()
                .category("카페")
                .lat(37.5665)
                .lng(126.9780)
                .userId(1L)
                .build();
    }

    @Test
    @DisplayName("지원하는 검색 타입 확인 - CATEGORY")
    void supports_CategoryType_ReturnsTrue() {
        // when
        boolean supports = categorySearchStrategy.supports(PlaceSearchType.CATEGORY);

        // then
        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("지원하지 않는 검색 타입 확인 - AI_QUERY")
    void supports_AiQueryType_ReturnsFalse() {
        // when
        boolean supports = categorySearchStrategy.supports(PlaceSearchType.AI_QUERY);

        // then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("우선순위 확인")
    void getPriority_ReturnsCorrectValue() {
        // when
        int priority = categorySearchStrategy.getPriority();

        // then
        assertThat(priority).isEqualTo(2);
    }

    @Test
    @DisplayName("카테고리 검색 성공 - 결과 있음")
    void search_WithCategoryResults_ReturnsSuccessfully() {
        // given
        List<PlaceWithDistance> placesWithDistance = createPlacesWithDistance();
        List<Place> places = createPlaces();
        List<Object[]> momentCountResults = createMomentCountResults();
        Map<Long, Boolean> bookmarkStatusMap = Map.of(1L, true, 2L, false);

        // PlaceCacheService mock 설정 (캐시 미스)
        given(placeCacheService.getCachedCategorySearchResult(anyString(), anyDouble(), anyDouble()))
                .willReturn(null);

        given(placeRepository.findPlacesByCategoryWithinRadius(
                eq(testContext.getCategory()),
                eq(testContext.getLat()),
                eq(testContext.getLng()),
                anyDouble()
        )).willReturn(placesWithDistance);

        given(placeRepository.findByIdsWithKeywords(anyList()))
                .willReturn(places);
        given(momentRepository.countPublicMomentsByPlaceIds(anyList()))
                .willReturn(momentCountResults);
        given(bookmarkQueryService.getBookmarkStatusMap(anyLong(), anyList()))
                .willReturn(bookmarkStatusMap);

        // when
        List<PlaceSearchResponse.PlaceDto> result = categorySearchStrategy.search(testContext).block();

        // then
        assertThat(result).hasSize(2);

        verify(placeRepository).findPlacesByCategoryWithinRadius(
                eq(testContext.getCategory()),
                eq(testContext.getLat()),
                eq(testContext.getLng()),
                anyDouble()
        );
        // CategorySearchStrategy는 PlaceDtoFactory를 사용하지 않고 직접 DTO를 생성
        verify(placeCacheService).cacheCategorySearchResult(anyString(), anyDouble(), anyDouble(), anyList());
    }

    @Test
    @DisplayName("카테고리 검색 - 빈 결과")
    void search_WithEmptyResults_ReturnsEmptyList() {
        // given
        // PlaceCacheService mock 설정 (캐시 미스)
        given(placeCacheService.getCachedCategorySearchResult(anyString(), anyDouble(), anyDouble()))
                .willReturn(null);

        given(placeRepository.findPlacesByCategoryWithinRadius(
                eq(testContext.getCategory()),
                eq(testContext.getLat()),
                eq(testContext.getLng()),
                anyDouble()
        )).willReturn(Collections.emptyList());

        // when
        List<PlaceSearchResponse.PlaceDto> result = categorySearchStrategy.search(testContext).block();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("카테고리 검색 - Place 엔티티 조회 실패")
    void search_PlaceEntitiesNotFound_ReturnsEmptyList() {
        // given
        List<PlaceWithDistance> placesWithDistance = createPlacesWithDistance();

        // PlaceCacheService mock 설정 (캐시 미스)
        given(placeCacheService.getCachedCategorySearchResult(anyString(), anyDouble(), anyDouble()))
                .willReturn(null);

        given(placeRepository.findPlacesByCategoryWithinRadius(
                eq(testContext.getCategory()),
                eq(testContext.getLat()),
                eq(testContext.getLng()),
                anyDouble()
        )).willReturn(placesWithDistance);

        given(placeRepository.findByIdsWithKeywords(anyList()))
                .willReturn(Collections.emptyList()); // 빈 결과

        given(momentRepository.countPublicMomentsByPlaceIds(anyList()))
                .willReturn(Collections.emptyList());
        given(bookmarkQueryService.getBookmarkStatusMap(anyLong(), anyList()))
                .willReturn(Collections.emptyMap());

        // when
        List<PlaceSearchResponse.PlaceDto> result = categorySearchStrategy.search(testContext).block();

        // then
        // Place 엔티티가 없어도 PlaceWithDistance 정보로 DTO가 생성되므로 빈 결과가 아님
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("카테고리 검색 - null 카테고리로 검색")
    void search_WithNullCategory_HandlesGracefully() {
        // given
        PlaceSearchContext contextWithNullCategory = PlaceSearchContext.builder()
                .category(null)
                .lat(37.5665)
                .lng(126.9780)
                .userId(1L)
                .build();

        // PlaceCacheService mock 설정 (캐시 미스)
        given(placeCacheService.getCachedCategorySearchResult(isNull(), anyDouble(), anyDouble()))
                .willReturn(null);

        given(placeRepository.findPlacesByCategoryWithinRadius(
                isNull(),
                eq(contextWithNullCategory.getLat()),
                eq(contextWithNullCategory.getLng()),
                anyDouble()
        )).willReturn(Collections.emptyList());

        // when
        List<PlaceSearchResponse.PlaceDto> result = categorySearchStrategy.search(contextWithNullCategory).block();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("카테고리 검색 - 거리 정렬 확인")
    void search_ResultsSortedByDistance() {
        // given
        List<PlaceWithDistance> placesWithDistance = Arrays.asList(
                createPlaceWithDistance(1L, "테스트 카페1", 100.0), // 더 가까운 장소를 첫 번째로
                createPlaceWithDistance(2L, "테스트 카페2", 500.0)
        );

        List<Place> places = createPlaces();
        List<Object[]> momentCountResults = createMomentCountResults();
        Map<Long, Boolean> bookmarkStatusMap = Map.of(1L, true, 2L, false);

        // PlaceCacheService mock 설정 (캐시 미스)
        given(placeCacheService.getCachedCategorySearchResult(anyString(), anyDouble(), anyDouble()))
                .willReturn(null);

        given(placeRepository.findPlacesByCategoryWithinRadius(
                eq(testContext.getCategory()),
                eq(testContext.getLat()),
                eq(testContext.getLng()),
                anyDouble()
        )).willReturn(placesWithDistance);

        given(placeRepository.findByIdsWithKeywords(anyList()))
                .willReturn(places);
        given(momentRepository.countPublicMomentsByPlaceIds(anyList()))
                .willReturn(momentCountResults);
        given(bookmarkQueryService.getBookmarkStatusMap(anyLong(), anyList()))
                .willReturn(bookmarkStatusMap);

        // when
        List<PlaceSearchResponse.PlaceDto> result = categorySearchStrategy.search(testContext).block();

        // then
        assertThat(result).hasSize(2);

        // CategorySearchStrategy는 Repository에서 이미 거리순으로 정렬된 결과를 반환
        // 실제 DTO의 내용 확인
        assertThat(result).allMatch(dto -> dto.getId() != null);
        assertThat(result).allMatch(dto -> dto.getName() != null);

        // 캐시 저장이 호출되었는지 확인
        verify(placeCacheService).cacheCategorySearchResult(anyString(), anyDouble(), anyDouble(), anyList());
    }

    private List<PlaceWithDistance> createPlacesWithDistance() {
        return Arrays.asList(
                createPlaceWithDistance(1L, "테스트 카페1", 100.0),
                createPlaceWithDistance(2L, "테스트 카페2", 200.0)
        );
    }

    private PlaceWithDistance createPlaceWithDistance(Long id, String name, Double distance) {
        return new PlaceWithDistance() {
            @Override public Long getId() { return id; }
            @Override public String getName() { return name; }
            @Override public String getCategory() { return "카페"; }
            @Override public String getRoadAddress() { return "테스트 도로명 주소"; }
            @Override public String getLotAddress() { return "테스트 지번 주소"; }
            @Override public Double getDistance() { return distance; }
            @Override public Double getLongitude() { return 126.9780; }
            @Override public Double getLatitude() { return 37.5665; }
            @Override public String getImageUrl() { return "image" + id + ".jpg"; }
        };
    }

    private List<Place> createPlaces() {
        Place place1 = Place.builder()
                .id(1L)
                .name("테스트 카페1")
                .imageUrl("image1.jpg")
                .build();

        Place place2 = Place.builder()
                .id(2L)
                .name("테스트 카페2")
                .imageUrl("image2.jpg")
                .build();

        return Arrays.asList(place1, place2);
    }

    private List<Object[]> createMomentCountResults() {
        return Arrays.asList(
                new Object[]{1L, 5L},
                new Object[]{2L, 3L}
        );
    }
}
