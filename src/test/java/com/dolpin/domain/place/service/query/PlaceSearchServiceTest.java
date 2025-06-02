package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.*;
import com.dolpin.domain.place.entity.*;
import com.dolpin.domain.place.repository.PlaceRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static com.dolpin.global.helper.PlaceTestHelper.*;
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

            PlaceAiResponse.PlaceRecommendation rec1 = createRecommendation(
                    TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_HIGH,
                    List.of(TestConstants.DELICIOUS_KEYWORD, TestConstants.ITALIAN_KEYWORD));
            PlaceAiResponse.PlaceRecommendation rec2 = createRecommendation(
                    TestConstants.PLACE_ID_2, TestConstants.SIMILARITY_SCORE_MEDIUM,
                    List.of(TestConstants.ROMANTIC_KEYWORD, TestConstants.DATE_KEYWORD));
            PlaceAiResponse aiResponse = new PlaceAiResponse(List.of(rec1, rec2));
            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

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
                    createMockPlace(TestConstants.PLACE_ID_1, TestConstants.ITALIAN_RESTAURANT_NAME,
                            TestConstants.CENTER_LAT, TestConstants.CENTER_LNG),
                    createMockPlace(TestConstants.PLACE_ID_2, TestConstants.ROMANTIC_PASTA_NAME,
                            TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG)
            );
            given(placeRepository.findByIdsWithKeywords(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)))).willReturn(places);

            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getPlaces()).hasSize(2);

            assertThat(result.getPlaces().get(0).getSimilarityScore()).isEqualTo(TestConstants.SIMILARITY_SCORE_HIGH);
            assertThat(result.getPlaces().get(1).getSimilarityScore()).isEqualTo(TestConstants.SIMILARITY_SCORE_MEDIUM);

            assertThat(result.getPlaces().get(0).getKeywords())
                    .containsExactlyInAnyOrder(TestConstants.DELICIOUS_KEYWORD, TestConstants.ITALIAN_KEYWORD);
            assertThat(result.getPlaces().get(1).getKeywords())
                    .containsExactlyInAnyOrder(TestConstants.ROMANTIC_KEYWORD, TestConstants.DATE_KEYWORD);

            verify(placeAiClient).recommendPlaces(query);
            verify(placeRepository).findPlacesWithinRadiusByIds(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)),
                    eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS));
            verify(placeRepository).findByIdsWithKeywords(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)));
        }

        @Test
        @DisplayName("카테고리 검색 - 카테고리만 있는 경우 정상 동작한다")
        void searchPlaces_WithCategoryOnly_PerformsDbSearch() {
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            List<PlaceWithDistance> categoryPlaces = List.of(
                    createPlaceWithDistance(TestConstants.PLACE_ID_1, TestConstants.STARBUCKS_NAME,
                            TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                    createPlaceWithDistance(TestConstants.PLACE_ID_2, TestConstants.TWOSOME_NAME,
                            TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG, TestConstants.DISTANCE_200M)
            );
            given(placeRepository.findPlacesByCategoryWithinRadius(category, lat, lng, TestConstants.DEFAULT_RADIUS))
                    .willReturn(categoryPlaces);

            List<Place> places = List.of(
                    createMockPlaceWithKeywords(TestConstants.PLACE_ID_1, TestConstants.STARBUCKS_NAME, getChainStoreKeywords()),
                    createMockPlaceWithKeywords(TestConstants.PLACE_ID_2, TestConstants.TWOSOME_NAME, getDessertKeywords())
            );
            given(placeRepository.findByIdsWithKeywords(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)))).willReturn(places);

            PlaceSearchResponse result = placeQueryService.searchPlaces(null, lat, lng, category);

            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getPlaces()).hasSize(2);

            assertThat(result.getPlaces().get(0).getSimilarityScore()).isNull();
            assertThat(result.getPlaces().get(1).getSimilarityScore()).isNull();

            assertThat(result.getPlaces().get(0).getKeywords())
                    .containsExactlyInAnyOrder(TestConstants.CHAIN_STORE_KEYWORD, TestConstants.SPACIOUS_KEYWORD);
            assertThat(result.getPlaces().get(1).getKeywords())
                    .containsExactlyInAnyOrder(TestConstants.DESSERT_KEYWORD, TestConstants.CAKE_KEYWORD);

            verify(placeRepository).findPlacesByCategoryWithinRadius(category, lat, lng, TestConstants.DEFAULT_RADIUS);
            verify(placeRepository).findByIdsWithKeywords(eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)));
            verifyNoInteractions(placeAiClient);
        }

        @Test
        @DisplayName("유사도 점수 기준 정렬이 정상 동작한다")
        void searchPlaces_SortsBySimilarityScore() {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceAiResponse aiResponse = createSortTestAiResponse();
            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

            List<PlaceWithDistance> nearbyPlaces = createSortTestPlaceWithDistances();
            given(placeRepository.findPlacesWithinRadiusByIds(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3)),
                    eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS)))
                    .willReturn(nearbyPlaces);

            List<Place> places = createSortTestPlaces();
            given(placeRepository.findByIdsWithKeywords(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3))))
                    .willReturn(places);

            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            assertThat(result.getPlaces()).hasSize(3);

            List<Double> scores = result.getPlaces().stream()
                    .map(PlaceSearchResponse.PlaceDto::getSimilarityScore)
                    .toList();
            assertThat(scores).containsExactly(
                    TestConstants.SIMILARITY_SCORE_HIGH,
                    TestConstants.SIMILARITY_SCORE_MEDIUM,
                    TestConstants.SIMILARITY_SCORE_LOW);

            List<String> names = result.getPlaces().stream()
                    .map(PlaceSearchResponse.PlaceDto::getName)
                    .toList();
            assertThat(names).containsExactly(
                    TestConstants.BEST_CAFE_NAME,
                    TestConstants.GOOD_CAFE_NAME,
                    TestConstants.ORDINARY_CAFE_NAME);
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

        @Test
        @DisplayName("검색어와 카테고리 모두 없을 때 예외 발생")
        void searchPlaces_WithNeitherQueryNorCategory_ThrowsException() {
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            assertThatThrownBy(() -> placeQueryService.searchPlaces(null, lat, lng, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("검색어 또는 카테고리가 필요합니다");

            assertThatThrownBy(() -> placeQueryService.searchPlaces(TestConstants.EMPTY_STRING, lat, lng, TestConstants.EMPTY_STRING))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("검색어 또는 카테고리가 필요합니다");

            assertThatThrownBy(() -> placeQueryService.searchPlaces("   ", lat, lng, "   "))
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

            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("AI 서비스 응답이 빈 결과일 때 빈 결과 반환")
        void searchPlaces_WithEmptyAiResponse_ReturnsEmptyResult() {
            String query = TestConstants.NON_EXISTENT_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceAiResponse emptyResponse = new PlaceAiResponse(Collections.emptyList());
            given(placeAiClient.recommendPlaces(query)).willReturn(emptyResponse);

            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("반경 내 장소가 없을 때 빈 결과 반환")
        void searchPlaces_WithNoPlacesInRadius_ReturnsEmptyResult() {
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceAiResponse.PlaceRecommendation rec = createRecommendation(
                    TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_HIGH,
                    List.of(TestConstants.DELICIOUS_KEYWORD));
            PlaceAiResponse aiResponse = new PlaceAiResponse(List.of(rec));
            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

            given(placeRepository.findPlacesWithinRadiusByIds(
                    eq(List.of(TestConstants.PLACE_ID_1)), eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS)))
                    .willReturn(Collections.emptyList());

            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }
    }
}
