package com.dolpin.domain.place.service.query;

import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.place.client.PlaceAiClient;
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
import com.dolpin.domain.place.service.template.SimplePlaceDetailQuery;
import com.dolpin.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

// 1. 메인 PlaceQueryServiceImpl 테스트 (수정됨)
@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceQueryServiceImpl 통합 테스트 - 리팩토링 후")
class PlaceQueryServiceImplTest {

    // ============= 리팩토링된 의존성들 =============

    // 기존 의존성들
    @Mock private PlaceRepository placeRepository;
    @Mock private PlaceAiClient placeAiClient;
    @Mock private MomentRepository momentRepository;
    @Mock private PlaceBookmarkQueryService bookmarkQueryService;
    @Mock private PlaceCacheService placeCacheService;

    // 1단계: Template Method 구현체들
    @Mock private FullPlaceDetailQuery fullPlaceDetailQuery;
    @Mock private SimplePlaceDetailQuery simplePlaceDetailQuery;

    // 2단계: Strategy Factory
    @Mock private PlaceSearchStrategyFactory placeSearchStrategyFactory;
    @Mock private PlaceSearchStrategy mockSearchStrategy;

    @InjectMocks
    private PlaceQueryServiceImpl placeQueryService;

    @Nested
    @DisplayName("Place 상세 조회 테스트 (Template Method 적용)")
    class PlaceDetailTest {

        @Test
        @DisplayName("일반 사용자 상세 조회 - FullPlaceDetailQuery 사용")
        void getPlaceDetail_WithUser_UsesFullQuery() {
            // given
            Long placeId = 1L;
            Long userId = 1L;
            PlaceDetailResponse expectedResponse = createMockDetailResponse();

            given(fullPlaceDetailQuery.getPlaceDetail(placeId, userId))
                    .willReturn(expectedResponse);

            // when
            PlaceDetailResponse result = placeQueryService.getPlaceDetail(placeId, userId);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            verify(fullPlaceDetailQuery).getPlaceDetail(placeId, userId);
            verifyNoInteractions(simplePlaceDetailQuery); // 다른 구현체는 사용하지 않음
        }

        @Test
        @DisplayName("비로그인 사용자 상세 조회 - SimplePlaceDetailQuery 사용")
        void getPlaceDetailWithoutBookmark_UsesSimpleQuery() {
            // given
            Long placeId = 1L;
            PlaceDetailResponse expectedResponse = createMockDetailResponseWithoutBookmark();

            given(simplePlaceDetailQuery.getPlaceDetail(placeId, null))
                    .willReturn(expectedResponse);

            // when
            PlaceDetailResponse result = placeQueryService.getPlaceDetailWithoutBookmark(placeId);

            // then
            assertThat(result).isEqualTo(expectedResponse);
            verify(simplePlaceDetailQuery).getPlaceDetail(placeId, null);
            verifyNoInteractions(fullPlaceDetailQuery); // 다른 구현체는 사용하지 않음
        }

        private PlaceDetailResponse createMockDetailResponse() {
            return PlaceDetailResponse.builder()
                    .id(1L)
                    .name("테스트 카페")
                    .isBookmarked(true) // 북마크 정보 포함
                    .build();
        }

        private PlaceDetailResponse createMockDetailResponseWithoutBookmark() {
            return PlaceDetailResponse.builder()
                    .id(1L)
                    .name("테스트 카페")
                    .isBookmarked(null) // 북마크 정보 없음
                    .build();
        }
    }

    @Nested
    @DisplayName("검색 테스트 (Strategy Pattern 적용)")
    class SearchTest {

        @Test
        @DisplayName("AI 검색 - PlaceSearchStrategy 팩토리 사용")
        void searchPlacesAsync_WithQuery_UsesAiStrategy() {
            // given
            String query = "맛있는 파스타";
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            List<PlaceSearchResponse.PlaceDto> mockPlaces = createMockPlacesList();

            // Strategy Factory가 AI 전략을 반환하도록 설정
            given(placeSearchStrategyFactory.getStrategy(PlaceSearchType.AI_QUERY))
                    .willReturn(mockSearchStrategy);
            given(mockSearchStrategy.search(any(PlaceSearchContext.class)))
                    .willReturn(Mono.just(mockPlaces));

            // when
            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, null, userId);
            PlaceSearchResponse result = resultMono.block();

            // then
            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getPlaces()).hasSize(2);

            // Strategy Factory 사용 확인
            verify(placeSearchStrategyFactory).getStrategy(PlaceSearchType.AI_QUERY);
            verify(mockSearchStrategy).search(any(PlaceSearchContext.class));
        }

        @Test
        @DisplayName("카테고리 검색 - PlaceSearchStrategy 팩토리 사용")
        void searchPlacesAsync_WithCategory_UsesCategoryStrategy() {
            // given
            String category = "카페";
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            List<PlaceSearchResponse.PlaceDto> mockPlaces = createMockPlacesList();

            // Strategy Factory가 카테고리 전략을 반환하도록 설정
            given(placeSearchStrategyFactory.getStrategy(PlaceSearchType.CATEGORY))
                    .willReturn(mockSearchStrategy);
            given(mockSearchStrategy.search(any(PlaceSearchContext.class)))
                    .willReturn(Mono.just(mockPlaces));

            // when
            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(null, lat, lng, category, userId);
            PlaceSearchResponse result = resultMono.block();

            // then
            assertThat(result.getTotal()).isEqualTo(2);

            // Strategy Factory 사용 확인
            verify(placeSearchStrategyFactory).getStrategy(PlaceSearchType.CATEGORY);
            verify(mockSearchStrategy).search(any(PlaceSearchContext.class));
        }

        @Test
        @DisplayName("잘못된 파라미터 - 검증 실패")
        void searchPlacesAsync_WithInvalidParams_ThrowsException() {
            // given - query와 category 둘 다 있는 경우 (잘못된 파라미터)
            String query = "파스타";
            String category = "카페";
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            // when & then
            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, category, userId);

            assertThatThrownBy(() -> resultMono.block())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("검색어와 카테고리 중 하나만 선택해주세요");
        }

        private List<PlaceSearchResponse.PlaceDto> createMockPlacesList() {
            return List.of(
                    PlaceSearchResponse.PlaceDto.builder()
                            .id(1L)
                            .name("테스트 카페1")
                            .thumbnail("image1.jpg")
                            .distance(100.0)
                            .momentCount(5L)
                            .isBookmarked(false)
                            .build(),
                    PlaceSearchResponse.PlaceDto.builder()
                            .id(2L)
                            .name("테스트 카페2")
                            .thumbnail("image2.jpg")
                            .distance(200.0)
                            .momentCount(3L)
                            .isBookmarked(true)
                            .build()
            );
        }
    }

    @Nested
    @DisplayName("카테고리 조회 테스트 (기존 로직 유지)")
    class CategoryTest {

        @Test
        @DisplayName("카테고리 목록 조회 - 캐시 히트")
        void getAllCategories_WithCache_ReturnsFromCache() {
            // given
            List<String> cachedCategories = List.of("카페", "레스토랑", "바");
            given(placeCacheService.getCachedCategories()).willReturn(cachedCategories);

            // when
            PlaceCategoryResponse result = placeQueryService.getAllCategories();

            // then
            assertThat(result.getCategories()).hasSize(3);
            assertThat(result.getCategories()).containsExactlyInAnyOrder("카페", "레스토랑", "바");

            verify(placeCacheService).getCachedCategories();
            verifyNoInteractions(placeRepository); // DB 조회하지 않음
        }

        @Test
        @DisplayName("카테고리 목록 조회 - 캐시 미스, DB 조회")
        void getAllCategories_WithoutCache_QueriesDatabase() {
            // given
            List<String> dbCategories = List.of("카페", "레스토랑");
            given(placeCacheService.getCachedCategories()).willReturn(null); // 캐시 미스
            given(placeRepository.findDistinctCategories()).willReturn(dbCategories);

            // when
            PlaceCategoryResponse result = placeQueryService.getAllCategories();

            // then
            assertThat(result.getCategories()).hasSize(2);
            assertThat(result.getCategories()).containsExactlyInAnyOrder("카페", "레스토랑");

            verify(placeCacheService).getCachedCategories();
            verify(placeRepository).findDistinctCategories();
            verify(placeCacheService).cacheCategories(dbCategories); // 캐시 저장
        }
    }
}
