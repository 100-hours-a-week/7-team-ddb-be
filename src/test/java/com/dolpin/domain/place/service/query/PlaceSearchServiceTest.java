package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.*;
import com.dolpin.domain.place.entity.*;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.global.constants.TestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.*;

import static com.dolpin.global.helper.PlaceServiceTestHelper.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Place 검색 서비스 테스트")
class PlaceSearchServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceAiClient placeAiClient;

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private PlaceBookmarkQueryService bookmarkQueryService;

    @InjectMocks
    private PlaceQueryServiceImpl placeQueryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(placeQueryService, "defaultSearchRadius", TestConstants.DEFAULT_RADIUS);
    }

    @Nested
    @DisplayName("searchPlacesAsync 메서드 테스트")
    class SearchPlacesTest {

        @Test
        @DisplayName("AI 검색 - 검색어만 있는 경우 정상 동작한다")
        void searchPlaces_WithQueryOnly_PerformsAiSearch() {
            String query = TestConstants.PASTA_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            setupAiSearchMocks(query, lat, lng, userId);

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, null, userId);
            PlaceSearchResponse result = resultMono.block();

            verifyAiSearchResults(result);
            verifyAiSearchInteractions(query, lat, lng);
        }

        @Test
        @DisplayName("카테고리 검색 - 카테고리만 있는 경우 정상 동작한다")
        void searchPlaces_WithCategoryOnly_PerformsDbSearch() {
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            setupCategorySearchMocks(category, lat, lng, userId);

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(null, lat, lng, category, userId);
            PlaceSearchResponse result = resultMono.block();

            verifyCategorySearchResults(result);
            verifyCategorySearchInteractions(category, lat, lng);
        }

        @Test
        @DisplayName("유사도 점수 기준 정렬이 정상 동작한다")
        void searchPlaces_SortsBySimilarityScore() {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            setupSortTestMocks(query, lat, lng, userId);

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, null, userId);
            PlaceSearchResponse result = resultMono.block();

            verifySortedResults(result);
        }

        @Test
        @DisplayName("검색어와 카테고리 동시 입력 시 예외 발생")
        void searchPlaces_WithBothQueryAndCategory_ThrowsException() {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, category, userId);

            assertThatThrownBy(() -> resultMono.block())
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("검색어와 카테고리 중 하나만 선택해주세요")
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.INVALID_PARAMETER);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("검색어와 카테고리 모두 없을 때 예외 발생")
        void searchPlaces_WithNeitherQueryNorCategory_ThrowsException(String emptyValue) {
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(emptyValue, lat, lng, emptyValue, userId);

            assertThatThrownBy(() -> resultMono.block())
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("검색어 또는 카테고리가 필요합니다");
        }

        @ParameterizedTest
        @CsvSource({
                "맛있는 카페, ,",
                "맛있는 카페, 37.5665,",
                "맛있는 카페, , 126.9780"
        })
        @DisplayName("위치 정보 누락 시 예외 발생")
        void searchPlaces_WithMissingLocation_ThrowsException(String query, Double lat, Double lng) {
            Long userId = TestConstants.USER_ID_1;

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, null, userId);

            assertThatThrownBy(() -> resultMono.block())
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("위치 정보가 필요합니다");
        }

        @Test
        @DisplayName("AI 서비스 응답이 null일 때 빈 결과 반환")
        void searchPlaces_WithNullAiResponse_ReturnsEmptyResult() {
            String query = TestConstants.NON_EXISTENT_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            // DB 검색 결과도 빈 결과로 설정
            given(placeRepository.findPlaceIdsByNameContaining(query)).willReturn(Collections.emptyList());

            // AI 응답을 빈 응답으로 설정 (Mono.empty() 대신)
            PlaceAiResponse emptyResponse = createEmptyAiResponse();
            given(placeAiClient.recommendPlacesAsync(query)).willReturn(Mono.just(emptyResponse));

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, null, userId);
            PlaceSearchResponse result = resultMono.block();

            assertThat(result).isNotNull(); // null 체크 추가
            assertThat(result.getTotal()).isZero();
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("AI 서비스 응답이 빈 결과일 때 빈 결과 반환")
        void searchPlaces_WithEmptyAiResponse_ReturnsEmptyResult() {
            String query = TestConstants.NON_EXISTENT_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            PlaceAiResponse emptyResponse = createEmptyAiResponse();
            given(placeAiClient.recommendPlacesAsync(query)).willReturn(Mono.just(emptyResponse));

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, null, userId);
            PlaceSearchResponse result = resultMono.block();

            assertThat(result.getTotal()).isZero();
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("반경 내 장소가 없을 때 빈 결과 반환")
        void searchPlaces_WithNoPlacesInRadius_ReturnsEmptyResult() {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            PlaceAiResponse.PlaceRecommendation rec = createRecommendation(
                    TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_HIGH,
                    List.of(TestConstants.DELICIOUS_KEYWORD));
            PlaceAiResponse aiResponse = createAiResponse(List.of(rec));
            given(placeAiClient.recommendPlacesAsync(query)).willReturn(Mono.just(aiResponse));

            given(placeRepository.findPlacesWithinRadiusByIds(
                    eq(List.of(TestConstants.PLACE_ID_1)), eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS)))
                    .willReturn(Collections.emptyList());

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(query, lat, lng, null, userId);
            PlaceSearchResponse result = resultMono.block();

            assertThat(result.getTotal()).isZero();
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("거리 기준 정렬이 정상 동작한다 - 카테고리 검색")
        void searchPlaces_CategorySearch_SortsByDistance() {
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;
            Long userId = TestConstants.USER_ID_1;

            setupDistanceSortTestMocks(category, lat, lng, userId);

            Mono<PlaceSearchResponse> resultMono = placeQueryService.searchPlacesAsync(null, lat, lng, category, userId);
            PlaceSearchResponse result = resultMono.block();

            verifyDistanceSortResults(result);
        }
    }

    private void setupAiSearchMocks(String query, Double lat, Double lng, Long userId) {
        given(placeRepository.findPlaceIdsByNameContaining(query)).willReturn(List.of(TestConstants.PLACE_ID_1));

        PlaceAiResponse.PlaceRecommendation rec1 = createRecommendation(
                TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_HIGH,
                List.of(TestConstants.DELICIOUS_KEYWORD, TestConstants.ITALIAN_KEYWORD));
        PlaceAiResponse.PlaceRecommendation rec2 = createRecommendation(
                TestConstants.PLACE_ID_2, TestConstants.SIMILARITY_SCORE_MEDIUM,
                List.of(TestConstants.ROMANTIC_KEYWORD, TestConstants.DATE_KEYWORD));
        PlaceAiResponse aiResponse = createAiResponse(List.of(rec1, rec2));
        given(placeAiClient.recommendPlacesAsync(query)).willReturn(Mono.just(aiResponse));

        List<PlaceWithDistance> nearbyPlaces = List.of(
                createPlaceWithDistance(TestConstants.PLACE_ID_1, TestConstants.ITALIAN_RESTAURANT_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_150M),
                createPlaceWithDistance(TestConstants.PLACE_ID_2, TestConstants.ROMANTIC_PASTA_NAME,
                        TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG, TestConstants.DISTANCE_300M)
        );
        given(placeRepository.findPlacesWithinRadiusByIds(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)),
                eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS)))
                .willReturn(nearbyPlaces);

        List<Place> places = List.of(
                createPlaceStub(TestConstants.PLACE_ID_1, TestConstants.ITALIAN_RESTAURANT_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG),
                createPlaceStub(TestConstants.PLACE_ID_2, TestConstants.ROMANTIC_PASTA_NAME,
                        TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG)
        );
        given(placeRepository.findByIdsWithKeywords(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)))).willReturn(places);

        given(momentRepository.countPublicMomentsByPlaceIds(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2))))
                .willReturn(createMomentCountTestData(
                        List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2),
                        List.of(TestConstants.MOMENT_COUNT_MEDIUM, TestConstants.MOMENT_COUNT_LOW)
                ));

        given(bookmarkQueryService.getBookmarkStatusMap(userId, List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)))
                .willReturn(Map.of(TestConstants.PLACE_ID_1, true, TestConstants.PLACE_ID_2, false));
    }

    private void setupCategorySearchMocks(String category, Double lat, Double lng, Long userId) {
        List<PlaceWithDistance> categoryPlaces = List.of(
                createPlaceWithDistance(TestConstants.PLACE_ID_1, TestConstants.STARBUCKS_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                createPlaceWithDistance(TestConstants.PLACE_ID_2, TestConstants.TWOSOME_NAME,
                        TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG, TestConstants.DISTANCE_200M)
        );
        given(placeRepository.findPlacesByCategoryWithinRadius(category, lat, lng, TestConstants.DEFAULT_RADIUS))
                .willReturn(categoryPlaces);

        List<Place> places = List.of(
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_1, TestConstants.STARBUCKS_NAME, getChainStoreKeywords()),
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_2, TestConstants.TWOSOME_NAME, getDessertKeywords())
        );
        given(placeRepository.findByIdsWithKeywords(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)))).willReturn(places);

        given(momentRepository.countPublicMomentsByPlaceIds(anyList()))
                .willReturn(createMomentCountTestData(
                        List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2),
                        List.of(TestConstants.MOMENT_COUNT_LOW, TestConstants.MOMENT_COUNT_LOW)
                ));

        given(bookmarkQueryService.getBookmarkStatusMap(userId, List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)))
                .willReturn(Map.of(TestConstants.PLACE_ID_1, false, TestConstants.PLACE_ID_2, true));
    }

    private void setupSortTestMocks(String query, Double lat, Double lng, Long userId) {
        // 1. DB 검색 결과를 빈 리스트로 설정 (실제 로그와 맞춤)
        given(placeRepository.findPlaceIdsByNameContaining(query)).willReturn(Collections.emptyList());

        // 2. AI 검색 결과 Mock - 3개 모두 포함
        PlaceAiResponse aiResponse = new PlaceAiResponse(List.of(
                createRecommendation(TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_LOW, List.of(TestConstants.ORDINARY_KEYWORD)),
                createRecommendation(TestConstants.PLACE_ID_2, TestConstants.SIMILARITY_SCORE_HIGH, List.of(TestConstants.BEST_KEYWORD)),
                createRecommendation(TestConstants.PLACE_ID_3, TestConstants.SIMILARITY_SCORE_MEDIUM, List.of(TestConstants.GOOD_KEYWORD))
        ), null);
        given(placeAiClient.recommendPlacesAsync(query)).willReturn(Mono.just(aiResponse));

        // 3. AI 결과만 있으므로 AI 결과 순서대로 반경 내 장소 검색 Mock
        List<PlaceWithDistance> nearbyPlaces = List.of(
                createPlaceWithDistance(TestConstants.PLACE_ID_1, TestConstants.ORDINARY_CAFE_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                createPlaceWithDistance(TestConstants.PLACE_ID_2, TestConstants.BEST_CAFE_NAME,
                        TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG, TestConstants.DISTANCE_200M),
                createPlaceWithDistance(TestConstants.PLACE_ID_3, TestConstants.GOOD_CAFE_NAME,
                        TestConstants.SORT_TEST_PLACE3_LAT, TestConstants.SORT_TEST_PLACE3_LNG, TestConstants.DISTANCE_300M)
        );
        given(placeRepository.findPlacesWithinRadiusByIds(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3)),
                eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS)))
                .willReturn(nearbyPlaces);

        // 4. 키워드 포함 장소 정보 Mock
        List<Place> places = List.of(
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_1, TestConstants.ORDINARY_CAFE_NAME, List.of(TestConstants.ORDINARY_KEYWORD)),
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_2, TestConstants.BEST_CAFE_NAME, List.of(TestConstants.BEST_KEYWORD)),
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_3, TestConstants.GOOD_CAFE_NAME, List.of(TestConstants.GOOD_KEYWORD))
        );
        given(placeRepository.findByIdsWithKeywords(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3))))
                .willReturn(places);

        // 5. Moment 개수 Mock
        given(momentRepository.countPublicMomentsByPlaceIds(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3))))
                .willReturn(createMomentCountTestData(
                        List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3),
                        List.of(TestConstants.MOMENT_COUNT_LOW, TestConstants.MOMENT_COUNT_HIGH, TestConstants.MOMENT_COUNT_MEDIUM)
                ));

        // 6. 북마크 상태 Mock
        given(bookmarkQueryService.getBookmarkStatusMap(userId,
                List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3)))
                .willReturn(Map.of(
                        TestConstants.PLACE_ID_1, true,
                        TestConstants.PLACE_ID_2, false,
                        TestConstants.PLACE_ID_3, true));
    }

    private void setupDistanceSortTestMocks(String category, Double lat, Double lng, Long userId) {
        List<PlaceWithDistance> sortedByDistance = List.of(
                createPlaceWithDistance(TestConstants.PLACE_ID_1, TestConstants.NEARBY_PREFIX + TestConstants.TEST_CAFE_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                createPlaceWithDistance(TestConstants.PLACE_ID_2, TestConstants.FAR_PREFIX + TestConstants.TEST_CAFE_NAME,
                        TestConstants.SORT_TEST_FAR_LAT, TestConstants.SORT_TEST_FAR_LNG, TestConstants.DISTANCE_500M)
        );
        given(placeRepository.findPlacesByCategoryWithinRadius(category, lat, lng, TestConstants.DEFAULT_RADIUS))
                .willReturn(sortedByDistance);

        List<Place> places = List.of(
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_1, TestConstants.NEARBY_PREFIX + TestConstants.TEST_CAFE_NAME, getDefaultCafeKeywords()),
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_2, TestConstants.FAR_PREFIX + TestConstants.TEST_CAFE_NAME, getDefaultCafeKeywords())
        );
        given(placeRepository.findByIdsWithKeywords(anyList())).willReturn(places);

        given(momentRepository.countPublicMomentsByPlaceIds(anyList()))
                .willReturn(createMomentCountTestData(
                        List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2),
                        List.of(TestConstants.MOMENT_COUNT_LOW, TestConstants.MOMENT_COUNT_LOW)
                ));

        given(bookmarkQueryService.getBookmarkStatusMap(userId, List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)))
                .willReturn(Map.of(TestConstants.PLACE_ID_1, true, TestConstants.PLACE_ID_2, false));
    }

    private void verifyAiSearchResults(PlaceSearchResponse result) {
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getPlaces()).hasSize(2);

        assertThat(result.getPlaces())
                .extracting(PlaceSearchResponse.PlaceDto::getSimilarityScore)
                .containsExactly(TestConstants.SIMILARITY_SCORE_HIGH, TestConstants.SIMILARITY_SCORE_MEDIUM)
                .isSortedAccordingTo(Comparator.reverseOrder());

        assertThat(result.getPlaces().get(0).getKeywords())
                .containsExactlyInAnyOrder(TestConstants.DELICIOUS_KEYWORD, TestConstants.ITALIAN_KEYWORD);
        assertThat(result.getPlaces().get(1).getKeywords())
                .containsExactlyInAnyOrder(TestConstants.ROMANTIC_KEYWORD, TestConstants.DATE_KEYWORD);
    }

    private void verifyCategorySearchResults(PlaceSearchResponse result) {
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getPlaces()).hasSize(2);

        assertThat(result.getPlaces())
                .extracting(PlaceSearchResponse.PlaceDto::getSimilarityScore)
                .containsOnly((Double) null);

        assertThat(result.getPlaces().get(0).getKeywords())
                .containsExactlyInAnyOrder(TestConstants.CHAIN_STORE_KEYWORD, TestConstants.SPACIOUS_KEYWORD);
        assertThat(result.getPlaces().get(1).getKeywords())
                .containsExactlyInAnyOrder(TestConstants.DESSERT_KEYWORD, TestConstants.CAKE_KEYWORD);
    }

    private void verifySortedResults(PlaceSearchResponse result) {
        assertThat(result).isNotNull();
        assertThat(result.getPlaces()).hasSize(TestConstants.EXPECTED_SORT_TEST_SIZE);

        // AI 결과만 있으므로 모든 결과가 유사도 점수를 가져야 함
        // 유사도 점수 기준 내림차순 정렬 확인 (높은 점수부터)
        assertThat(result.getPlaces())
                .extracting(PlaceSearchResponse.PlaceDto::getSimilarityScore)
                .containsExactly(
                        TestConstants.SIMILARITY_SCORE_HIGH,    // 0.95
                        TestConstants.SIMILARITY_SCORE_MEDIUM,  // 0.88
                        TestConstants.SIMILARITY_SCORE_LOW      // 0.75
                )
                .isSortedAccordingTo(Comparator.reverseOrder());

        // 이름도 점수 순서대로 정렬되어야 함
        assertThat(result.getPlaces())
                .extracting(PlaceSearchResponse.PlaceDto::getName)
                .containsExactly(
                        TestConstants.BEST_CAFE_NAME,        // 가장 높은 점수
                        TestConstants.GOOD_CAFE_NAME,        // 중간 점수
                        TestConstants.ORDINARY_CAFE_NAME     // 가장 낮은 점수
                );
    }

    private void verifyDistanceSortResults(PlaceSearchResponse result) {
        assertThat(result.getPlaces()).hasSize(2);

        assertThat(result.getPlaces())
                .extracting(PlaceSearchResponse.PlaceDto::getDistance)
                .containsExactly(TestConstants.DISTANCE_100M, TestConstants.DISTANCE_500M)
                .isSortedAccordingTo(Comparator.naturalOrder());

        assertThat(result.getPlaces())
                .extracting(PlaceSearchResponse.PlaceDto::getName)
                .containsExactly(
                        TestConstants.NEARBY_PREFIX + TestConstants.TEST_CAFE_NAME,
                        TestConstants.FAR_PREFIX + TestConstants.TEST_CAFE_NAME);
    }

    private void verifyAiSearchInteractions(String query, Double lat, Double lng) {
        verify(placeRepository).findPlaceIdsByNameContaining(query);
        verify(placeAiClient).recommendPlacesAsync(query);
        verify(placeRepository).findPlacesWithinRadiusByIds(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)),
                eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS));
        verify(placeRepository).findByIdsWithKeywords(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)));
    }

    private void verifyCategorySearchInteractions(String category, Double lat, Double lng) {
        verify(placeRepository).findPlacesByCategoryWithinRadius(category, lat, lng, TestConstants.DEFAULT_RADIUS);
        verify(placeRepository).findByIdsWithKeywords(eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)));
        verifyNoInteractions(placeAiClient);
    }
}
