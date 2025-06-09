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

    @InjectMocks
    private PlaceQueryServiceImpl placeQueryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(placeQueryService, "defaultSearchRadius", TestConstants.DEFAULT_RADIUS);
    }

    @Nested
    @DisplayName("searchPlaces 메서드 테스트")
    class SearchPlacesTest {

        @Test
        @DisplayName("AI 검색 - 검색어만 있는 경우 정상 동작한다")
        void searchPlaces_WithQueryOnly_PerformsAiSearch() {
            String query = TestConstants.PASTA_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            setupAiSearchMocks(query, lat, lng);

            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            verifyAiSearchResults(result);
            verifyAiSearchInteractions(query, lat, lng);
        }

        @Test
        @DisplayName("카테고리 검색 - 카테고리만 있는 경우 정상 동작한다")
        void searchPlaces_WithCategoryOnly_PerformsDbSearch() {
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            setupCategorySearchMocks(category, lat, lng);

            PlaceSearchResponse result = placeQueryService.searchPlaces(null, lat, lng, category);

            verifyCategorySearchResults(result);
            verifyCategorySearchInteractions(category, lat, lng);
        }

        @Test
        @DisplayName("유사도 점수 기준 정렬이 정상 동작한다")
        void searchPlaces_SortsBySimilarityScore() {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            setupSortTestMocks(query, lat, lng);

            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            verifySortedResults(result);
        }

        @Test
        @DisplayName("검색어와 카테고리 동시 입력 시 예외 발생")
        void searchPlaces_WithBothQueryAndCategory_ThrowsException() {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            assertThatThrownBy(() -> placeQueryService.searchPlaces(query, lat, lng, category))
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

            assertThatThrownBy(() -> placeQueryService.searchPlaces(emptyValue, lat, lng, emptyValue))
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
            assertThatThrownBy(() -> placeQueryService.searchPlaces(query, lat, lng, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("위치 정보가 필요합니다");
        }

        @Test
        @DisplayName("AI 서비스 응답이 null일 때 빈 결과 반환")
        void searchPlaces_WithNullAiResponse_ReturnsEmptyResult() {
            String query = TestConstants.NON_EXISTENT_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            given(placeAiClient.recommendPlaces(query)).willReturn(null);

            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            assertThat(result.getTotal()).isZero();
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("AI 서비스 응답이 빈 결과일 때 빈 결과 반환")
        void searchPlaces_WithEmptyAiResponse_ReturnsEmptyResult() {
            String query = TestConstants.NON_EXISTENT_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceAiResponse emptyResponse = createEmptyAiResponse();
            given(placeAiClient.recommendPlaces(query)).willReturn(emptyResponse);

            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            assertThat(result.getTotal()).isZero();
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("반경 내 장소가 없을 때 빈 결과 반환")
        void searchPlaces_WithNoPlacesInRadius_ReturnsEmptyResult() {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceAiResponse.PlaceRecommendation rec = createRecommendationStub(
                    TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_HIGH,
                    List.of(TestConstants.DELICIOUS_KEYWORD));
            PlaceAiResponse aiResponse = createAiResponseStub(List.of(rec));
            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

            given(placeRepository.findPlacesWithinRadiusByIds(
                    eq(List.of(TestConstants.PLACE_ID_1)), eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS)))
                    .willReturn(Collections.emptyList());

            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            assertThat(result.getTotal()).isZero();
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("거리 기준 정렬이 정상 동작한다 - 카테고리 검색")
        void searchPlaces_CategorySearch_SortsByDistance() {
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            setupDistanceSortTestMocks(category, lat, lng);

            PlaceSearchResponse result = placeQueryService.searchPlaces(null, lat, lng, category);

            verifyDistanceSortResults(result);
        }
    }

    private void setupAiSearchMocks(String query, Double lat, Double lng) {
        PlaceAiResponse.PlaceRecommendation rec1 = createRecommendationStub(
                TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_HIGH,
                List.of(TestConstants.DELICIOUS_KEYWORD, TestConstants.ITALIAN_KEYWORD));
        PlaceAiResponse.PlaceRecommendation rec2 = createRecommendationStub(
                TestConstants.PLACE_ID_2, TestConstants.SIMILARITY_SCORE_MEDIUM,
                List.of(TestConstants.ROMANTIC_KEYWORD, TestConstants.DATE_KEYWORD));
        PlaceAiResponse aiResponse = createAiResponseStub(List.of(rec1, rec2));
        given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

        List<PlaceWithDistance> nearbyPlaces = List.of(
                createPlaceWithDistanceStub(TestConstants.PLACE_ID_1, TestConstants.ITALIAN_RESTAURANT_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_150M),
                createPlaceWithDistanceStub(TestConstants.PLACE_ID_2, TestConstants.ROMANTIC_PASTA_NAME,
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
    }

    private void setupCategorySearchMocks(String category, Double lat, Double lng) {
        List<PlaceWithDistance> categoryPlaces = List.of(
                createPlaceWithDistanceStub(TestConstants.PLACE_ID_1, TestConstants.STARBUCKS_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                createPlaceWithDistanceStub(TestConstants.PLACE_ID_2, TestConstants.TWOSOME_NAME,
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
    }

    private void setupSortTestMocks(String query, Double lat, Double lng) {
        PlaceAiResponse aiResponse = createSortTestAiResponseStub();
        given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

        List<PlaceWithDistance> nearbyPlaces = createSortTestPlaceWithDistanceStubs();
        given(placeRepository.findPlacesWithinRadiusByIds(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3)),
                eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS)))
                .willReturn(nearbyPlaces);

        List<Place> places = createSortTestPlaceStubs();
        given(placeRepository.findByIdsWithKeywords(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3))))
                .willReturn(places);

        given(momentRepository.countPublicMomentsByPlaceIds(
                eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3))))
                .willReturn(createMomentCountTestData(
                        List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3),
                        List.of(TestConstants.MOMENT_COUNT_HIGH, TestConstants.MOMENT_COUNT_MEDIUM, TestConstants.MOMENT_COUNT_LOW)
                ));
    }

    private void setupDistanceSortTestMocks(String category, Double lat, Double lng) {
        List<PlaceWithDistance> sortedByDistance = List.of(
                createPlaceWithDistanceStub(TestConstants.PLACE_ID_1, TestConstants.NEARBY_PREFIX + TestConstants.TEST_CAFE_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                createPlaceWithDistanceStub(TestConstants.PLACE_ID_2, TestConstants.FAR_PREFIX + TestConstants.TEST_CAFE_NAME,
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
        assertThat(result.getPlaces()).hasSize(TestConstants.EXPECTED_SORT_TEST_SIZE);

        assertThat(result.getPlaces())
                .extracting(PlaceSearchResponse.PlaceDto::getSimilarityScore)
                .containsExactly(
                        TestConstants.SIMILARITY_SCORE_HIGH,
                        TestConstants.SIMILARITY_SCORE_MEDIUM,
                        TestConstants.SIMILARITY_SCORE_LOW)
                .isSortedAccordingTo(Comparator.reverseOrder());

        assertThat(result.getPlaces())
                .extracting(PlaceSearchResponse.PlaceDto::getName)
                .containsExactly(
                        TestConstants.BEST_CAFE_NAME,
                        TestConstants.GOOD_CAFE_NAME,
                        TestConstants.ORDINARY_CAFE_NAME);
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
        verify(placeAiClient).recommendPlaces(query);
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
