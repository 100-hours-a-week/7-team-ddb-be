package com.dolpin.domain.place.service.query;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceQueryServiceImpl 테스트 - Template Method & Strategy Pattern 적용")
class PlaceQueryServiceImplTest {

    @InjectMocks
    private PlaceQueryServiceImpl placeQueryService;

    // Repository
    @Mock
    private PlaceRepository placeRepository;

    // Cache Service
    @Mock
    private PlaceCacheService placeCacheService;

    // Template Method 구현체들
    @Mock
    private FullPlaceDetailQuery fullPlaceDetailQuery;

    @Mock
    private SimplePlaceDetailQuery simplePlaceDetailQuery;

    // Strategy Pattern
    @Mock
    private PlaceSearchStrategyFactory placeSearchStrategyFactory;

    @Mock
    private PlaceSearchStrategy mockSearchStrategy;

    @Nested
    @DisplayName("카테고리 조회 테스트")
    class CategoryTest {

        @Test
        @DisplayName("카테고리 조회 성공 - 캐시 히트")
        void getAllCategories_CacheHit_Success() {
            // given
            List<String> cachedCategories = Arrays.asList("카페", "레스토랑", "바");
            given(placeCacheService.getCachedCategories()).willReturn(cachedCategories);

            // when
            PlaceCategoryResponse result = placeQueryService.getAllCategories();

            // then
            assertThat(result.getCategories()).hasSize(3);
            assertThat(result.getCategories()).containsExactly("카페", "레스토랑", "바");

            // 캐시 히트이므로 Repository 호출 없음
            verifyNoInteractions(placeRepository);
        }

        @Test
        @DisplayName("카테고리 조회 성공 - 캐시 미스")
        void getAllCategories_CacheMiss_Success() {
            // given
            List<String> dbCategories = Arrays.asList("카페", "레스토랑", "바", "베이커리");
            given(placeCacheService.getCachedCategories()).willReturn(null);
            given(placeRepository.findDistinctCategories()).willReturn(dbCategories);

            // when
            PlaceCategoryResponse result = placeQueryService.getAllCategories();

            // then
            assertThat(result.getCategories()).hasSize(4);
            assertThat(result.getCategories()).containsExactly("카페", "레스토랑", "바", "베이커리");

            verify(placeRepository).findDistinctCategories();
            verify(placeCacheService).cacheCategories(dbCategories);
        }

        @Test
        @DisplayName("카테고리 조회 - 빈 결과")
        void getAllCategories_EmptyResult_Success() {
            // given
            given(placeCacheService.getCachedCategories()).willReturn(null);
            given(placeRepository.findDistinctCategories()).willReturn(Collections.emptyList());

            // when
            PlaceCategoryResponse result = placeQueryService.getAllCategories();

            // then
            assertThat(result.getCategories()).isEmpty();
            verify(placeCacheService).cacheCategories(Collections.emptyList());
        }
    }

    @Nested
    @DisplayName("Place 상세 조회 테스트 - Template Method Pattern")
    class PlaceDetailTest {

        @Test
        @DisplayName("로그인 사용자 상세 조회 - FullPlaceDetailQuery 사용")
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
            assertThat(result.getIsBookmarked()).isTrue(); // 북마크 정보 포함

            verify(fullPlaceDetailQuery).getPlaceDetail(placeId, userId);
            verifyNoInteractions(simplePlaceDetailQuery);
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
            assertThat(result.getIsBookmarked()).isNull(); // 북마크 정보 없음

            verify(simplePlaceDetailQuery).getPlaceDetail(placeId, null);
            verifyNoInteractions(fullPlaceDetailQuery);
        }

        private PlaceDetailResponse createMockDetailResponse() {
            Map<String, Object> location = Map.of(
                    "type", "Point",
                    "coordinates", new double[]{126.9780, 37.5665}
            );

            PlaceDetailResponse.Schedule schedule = PlaceDetailResponse.Schedule.builder()
                    .day("mon")
                    .hours("09:00~22:00")
                    .breakTime("15:00~16:00")
                    .build();

            PlaceDetailResponse.OpeningHours openingHours = PlaceDetailResponse.OpeningHours.builder()
                    .status("영업중")
                    .schedules(List.of(schedule))
                    .build();

            PlaceDetailResponse.Menu menu = PlaceDetailResponse.Menu.builder()
                    .name("아메리카노")
                    .price(4500)
                    .build();

            return PlaceDetailResponse.builder()
                    .id(1L)
                    .name("테스트 카페")
                    .address("서울시 강남구 테헤란로 123")
                    .thumbnail("image.jpg")
                    .location(location)
                    .keywords(Arrays.asList("커피", "카페", "디저트"))
                    .description("맛있는 커피를 파는 카페")
                    .phone("02-1234-5678")
                    .isBookmarked(true) // 북마크 정보 포함
                    .openingHours(openingHours)
                    .menu(Arrays.asList(menu))
                    .build();
        }

        private PlaceDetailResponse createMockDetailResponseWithoutBookmark() {
            Map<String, Object> location = Map.of(
                    "type", "Point",
                    "coordinates", new double[]{126.9780, 37.5665}
            );

            PlaceDetailResponse.Schedule schedule = PlaceDetailResponse.Schedule.builder()
                    .day("mon")
                    .hours("09:00~22:00")
                    .breakTime("15:00~16:00")
                    .build();

            PlaceDetailResponse.OpeningHours openingHours = PlaceDetailResponse.OpeningHours.builder()
                    .status("영업중")
                    .schedules(List.of(schedule))
                    .build();

            PlaceDetailResponse.Menu menu = PlaceDetailResponse.Menu.builder()
                    .name("아메리카노")
                    .price(4500)
                    .build();

            return PlaceDetailResponse.builder()
                    .id(1L)
                    .name("테스트 카페")
                    .address("서울시 강남구 테헤란로 123")
                    .thumbnail("image.jpg")
                    .location(location)
                    .keywords(Arrays.asList("커피", "카페", "디저트"))
                    .description("맛있는 커피를 파는 카페")
                    .phone("02-1234-5678")
                    .isBookmarked(null) // 북마크 정보 없음
                    .openingHours(openingHours)
                    .menu(Arrays.asList(menu))
                    .build();
        }
    }

    @Nested
    @DisplayName("Place 검색 테스트 - Strategy Pattern")
    class PlaceSearchTest {

        @Test
        @DisplayName("AI 검색 - Strategy Factory 사용")
        void searchPlacesAsync_WithQuery_UsesAiStrategy() {
            // given
            String query = "맛있는 파스타";
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            List<PlaceSearchResponse.PlaceDto> mockPlaces = createMockPlacesList();
            PlaceSearchResponse expectedResponse = PlaceSearchResponse.builder()
                    .places(mockPlaces)
                    .total(mockPlaces.size())
                    .build();

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

            verify(placeSearchStrategyFactory).getStrategy(PlaceSearchType.AI_QUERY);
            verify(mockSearchStrategy).search(any(PlaceSearchContext.class));
        }

        @Test
        @DisplayName("카테고리 검색 - Strategy Factory 사용")
        void searchPlacesAsync_WithCategory_UsesCategoryStrategy() {
            // given
            String category = "카페";
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            List<PlaceSearchResponse.PlaceDto> mockPlaces = createMockPlacesList();

            given(placeSearchStrategyFactory.getStrategy(PlaceSearchType.CATEGORY))
                    .willReturn(mockSearchStrategy);
            given(mockSearchStrategy.search(any(PlaceSearchContext.class)))
                    .willReturn(Mono.just(mockPlaces));

            // when
            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(null, lat, lng, category, userId);
            PlaceSearchResponse result = resultMono.block();

            // then
            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getPlaces()).hasSize(2);

            verify(placeSearchStrategyFactory).getStrategy(PlaceSearchType.CATEGORY);
            verify(mockSearchStrategy).search(any(PlaceSearchContext.class));
        }

        @Test
        @DisplayName("검색 파라미터 검증 실패 - query와 category 모두 있음")
        void searchPlacesAsync_InvalidParams_ThrowsException() {
            // given - query와 category 둘 다 제공 (잘못된 파라미터)
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

        @Test
        @DisplayName("검색 파라미터 검증 실패 - query와 category 모두 없음")
        void searchPlacesAsync_NoParams_ThrowsException() {
            // given - query와 category 둘 다 없음
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            // when & then
            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(null, lat, lng, null, userId);

            assertThatThrownBy(() -> resultMono.block())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("검색어 또는 카테고리가 필요합니다");
        }

        @Test
        @DisplayName("검색 결과 없음")
        void searchPlacesAsync_EmptyResult_ReturnsEmptyResponse() {
            // given
            String query = "존재하지않는장소";
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            given(placeSearchStrategyFactory.getStrategy(PlaceSearchType.AI_QUERY))
                    .willReturn(mockSearchStrategy);
            given(mockSearchStrategy.search(any(PlaceSearchContext.class)))
                    .willReturn(Mono.just(Collections.emptyList()));

            // when
            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, null, userId);
            PlaceSearchResponse result = resultMono.block();

            // then
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("Strategy 에러 전파")
        void searchPlacesAsync_StrategyError_PropagatesError() {
            // given
            String query = "테스트";
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            RuntimeException expectedException = new RuntimeException("Strategy 오류");
            given(placeSearchStrategyFactory.getStrategy(PlaceSearchType.AI_QUERY))
                    .willReturn(mockSearchStrategy);
            given(mockSearchStrategy.search(any(PlaceSearchContext.class)))
                    .willReturn(Mono.error(expectedException));

            // when & then
            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, null, userId);

            assertThatThrownBy(() -> resultMono.block())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Strategy 오류");
        }

        private List<PlaceSearchResponse.PlaceDto> createMockPlacesList() {
            return Arrays.asList(
                    PlaceSearchResponse.PlaceDto.builder()
                            .id(1L)
                            .name("테스트 카페1")
                            .thumbnail("image1.jpg")
                            .distance(100.0)
                            .momentCount(5L)
                            .keywords(Arrays.asList("커피", "카페"))
                            .isBookmarked(true)
                            .build(),
                    PlaceSearchResponse.PlaceDto.builder()
                            .id(2L)
                            .name("테스트 카페2")
                            .thumbnail("image2.jpg")
                            .distance(200.0)
                            .momentCount(3L)
                            .keywords(Arrays.asList("디저트", "카페"))
                            .isBookmarked(false)
                            .build()
            );
        }
    }

    @Nested
    @DisplayName("PlaceSearchContext 생성 테스트")
    class PlaceSearchContextTest {

        @Test
        @DisplayName("AI 검색 Context 생성")
        void createSearchContext_ForAiQuery_CreatesCorrectContext() {
            // given
            String query = "맛있는 파스타";
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            given(placeSearchStrategyFactory.getStrategy(PlaceSearchType.AI_QUERY))
                    .willReturn(mockSearchStrategy);
            given(mockSearchStrategy.search(any(PlaceSearchContext.class)))
                    .willAnswer(invocation -> {
                        PlaceSearchContext context = invocation.getArgument(0);

                        // Context 검증
                        assertThat(context.getQuery()).isEqualTo(query);
                        assertThat(context.getCategory()).isNull();
                        assertThat(context.getLat()).isEqualTo(lat);
                        assertThat(context.getLng()).isEqualTo(lng);
                        assertThat(context.getUserId()).isEqualTo(userId);

                        return Mono.just(Collections.emptyList());
                    });

            // when
            placeQueryService.searchPlacesAsync(query, lat, lng, null, userId).block();

            // then
            // mockSearchStrategy.search() 호출 시 Context 검증 완료
        }

        @Test
        @DisplayName("카테고리 검색 Context 생성")
        void createSearchContext_ForCategoryQuery_CreatesCorrectContext() {
            // given
            String category = "카페";
            Double lat = 37.5665;
            Double lng = 126.9780;
            Long userId = 1L;

            given(placeSearchStrategyFactory.getStrategy(PlaceSearchType.CATEGORY))
                    .willReturn(mockSearchStrategy);
            given(mockSearchStrategy.search(any(PlaceSearchContext.class)))
                    .willAnswer(invocation -> {
                        PlaceSearchContext context = invocation.getArgument(0);

                        // Context 검증
                        assertThat(context.getQuery()).isNull();
                        assertThat(context.getCategory()).isEqualTo(category);
                        assertThat(context.getLat()).isEqualTo(lat);
                        assertThat(context.getLng()).isEqualTo(lng);
                        assertThat(context.getUserId()).isEqualTo(userId);

                        return Mono.just(Collections.emptyList());
                    });

            // when
            placeQueryService.searchPlacesAsync(null, lat, lng, category, userId).block();

            // then
            // mockSearchStrategy.search() 호출 시 Context 검증 완료
        }
    }
}
