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
            // given
            String query = TestConstants.PASTA_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            // AI 응답 설정 - Reflection 제거
            PlaceAiResponse.PlaceRecommendation rec1 = createRecommendation(
                    TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_HIGH,
                    List.of(TestConstants.DELICIOUS_KEYWORD, TestConstants.ITALIAN_KEYWORD));
            PlaceAiResponse.PlaceRecommendation rec2 = createRecommendation(
                    TestConstants.PLACE_ID_2, TestConstants.SIMILARITY_SCORE_MEDIUM,
                    List.of(TestConstants.ROMANTIC_KEYWORD, TestConstants.DATE_KEYWORD));
            PlaceAiResponse aiResponse = new PlaceAiResponse(List.of(rec1, rec2));
            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

            // 거리 내 장소 조회 결과 - 메서드명 수정
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

            // 키워드 포함 장소 조회 (AI 검색에서는 기본 정보만 필요)
            List<Place> places = List.of(
                    createMockPlace(TestConstants.PLACE_ID_1, TestConstants.ITALIAN_RESTAURANT_NAME,
                            TestConstants.CENTER_LAT, TestConstants.CENTER_LNG),
                    createMockPlace(TestConstants.PLACE_ID_2, TestConstants.ROMANTIC_PASTA_NAME,
                            TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG)
            );
            given(placeRepository.findByIdsWithKeywords(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)))).willReturn(places);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getPlaces()).hasSize(2);

            // 유사도 점수 기준 정렬 확인 (높은 점수가 먼저)
            assertThat(result.getPlaces().get(0).getSimilarityScore()).isEqualTo(TestConstants.SIMILARITY_SCORE_HIGH);
            assertThat(result.getPlaces().get(1).getSimilarityScore()).isEqualTo(TestConstants.SIMILARITY_SCORE_MEDIUM);

            // AI 키워드 사용 확인
            assertThat(result.getPlaces().get(0).getKeywords())
                    .containsExactlyInAnyOrder(TestConstants.DELICIOUS_KEYWORD, TestConstants.ITALIAN_KEYWORD);
            assertThat(result.getPlaces().get(1).getKeywords())
                    .containsExactlyInAnyOrder(TestConstants.ROMANTIC_KEYWORD, TestConstants.DATE_KEYWORD);

            // 메서드 호출 검증
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
            // given
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            // 카테고리별 거리 내 장소 조회 결과 - 메서드명 수정
            List<PlaceWithDistance> categoryPlaces = List.of(
                    createPlaceWithDistance(TestConstants.PLACE_ID_1, TestConstants.STARBUCKS_NAME,
                            TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                    createPlaceWithDistance(TestConstants.PLACE_ID_2, TestConstants.TWOSOME_NAME,
                            TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG, TestConstants.DISTANCE_200M)
            );
            given(placeRepository.findPlacesByCategoryWithinRadius(category, lat, lng, TestConstants.DEFAULT_RADIUS))
                    .willReturn(categoryPlaces);

            // 키워드 포함 장소들 (카테고리 검색에서는 ID와 키워드 필요)
            List<Place> places = List.of(
                    createMockPlaceWithKeywords(TestConstants.PLACE_ID_1, TestConstants.STARBUCKS_NAME, getChainStoreKeywords()),
                    createMockPlaceWithKeywords(TestConstants.PLACE_ID_2, TestConstants.TWOSOME_NAME, getDessertKeywords())
            );
            given(placeRepository.findByIdsWithKeywords(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)))).willReturn(places);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(null, lat, lng, category);

            // then
            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getPlaces()).hasSize(2);

            // 카테고리 검색은 유사도 점수가 null
            assertThat(result.getPlaces().get(0).getSimilarityScore()).isNull();
            assertThat(result.getPlaces().get(1).getSimilarityScore()).isNull();

            // DB 키워드 사용 확인
            assertThat(result.getPlaces().get(0).getKeywords())
                    .containsExactlyInAnyOrder(TestConstants.CHAIN_STORE_KEYWORD, TestConstants.SPACIOUS_KEYWORD);
            assertThat(result.getPlaces().get(1).getKeywords())
                    .containsExactlyInAnyOrder(TestConstants.DESSERT_KEYWORD, TestConstants.CAKE_KEYWORD);

            // 메서드 호출 검증
            verify(placeRepository).findPlacesByCategoryWithinRadius(category, lat, lng, TestConstants.DEFAULT_RADIUS);
            verify(placeRepository).findByIdsWithKeywords(eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2)));
            verifyNoInteractions(placeAiClient);
        }

        @Test
        @DisplayName("유사도 점수 기준 정렬이 정상 동작한다")
        void searchPlaces_SortsBySimilarityScore() {
            // given
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            // 의도적으로 순서를 섞은 AI 응답 - Helper 메서드 활용
            PlaceAiResponse aiResponse = createSortTestAiResponse();
            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

            // 거리 내 장소들 - Helper 메서드 활용
            List<PlaceWithDistance> nearbyPlaces = createSortTestPlaceWithDistances();
            given(placeRepository.findPlacesWithinRadiusByIds(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3)),
                    eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS)))
                    .willReturn(nearbyPlaces);

            // 키워드 포함 장소들 - Helper 메서드 활용
            List<Place> places = createSortTestPlaces();
            given(placeRepository.findByIdsWithKeywords(
                    eq(List.of(TestConstants.PLACE_ID_1, TestConstants.PLACE_ID_2, TestConstants.PLACE_ID_3))))
                    .willReturn(places);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getPlaces()).hasSize(3);

            // 유사도 점수 순으로 정렬되어야 함 (0.95 > 0.88 > 0.75)
            List<Double> scores = result.getPlaces().stream()
                    .map(PlaceSearchResponse.PlaceDto::getSimilarityScore)
                    .toList();
            assertThat(scores).containsExactly(
                    TestConstants.SIMILARITY_SCORE_HIGH,
                    TestConstants.SIMILARITY_SCORE_MEDIUM,
                    TestConstants.SIMILARITY_SCORE_LOW);

            // 장소 이름도 유사도 점수 순으로 정렬되어야 함
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
            // given
            String query = TestConstants.CAFE_SEARCH_QUERY;
            String category = TestConstants.CAFE_CATEGORY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            // when & then
            assertThatThrownBy(() -> placeQueryService.searchPlaces(query, lat, lng, category))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("검색어와 카테고리 중 하나만 선택해주세요")
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.INVALID_PARAMETER);
        }

        @Test
        @DisplayName("검색어와 카테고리 모두 없을 때 예외 발생")
        void searchPlaces_WithNeitherQueryNorCategory_ThrowsException() {
            // given
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            // when & then
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
            // when & then
            assertThatThrownBy(() -> placeQueryService.searchPlaces(query, lat, lng, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("위치 정보가 필요합니다");
        }

        @Test
        @DisplayName("AI 서비스 응답이 null일 때 빈 결과 반환")
        void searchPlaces_WithNullAiResponse_ReturnsEmptyResult() {
            // given
            String query = TestConstants.NON_EXISTENT_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            given(placeAiClient.recommendPlaces(query)).willReturn(null);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("AI 서비스 응답이 빈 결과일 때 빈 결과 반환")
        void searchPlaces_WithEmptyAiResponse_ReturnsEmptyResult() {
            // given
            String query = TestConstants.NON_EXISTENT_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            PlaceAiResponse emptyResponse = new PlaceAiResponse(Collections.emptyList());
            given(placeAiClient.recommendPlaces(query)).willReturn(emptyResponse);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("반경 내 장소가 없을 때 빈 결과 반환")
        void searchPlaces_WithNoPlacesInRadius_ReturnsEmptyResult() {
            // given
            String query = TestConstants.CAFE_SEARCH_QUERY;
            Double lat = TestConstants.CENTER_LAT;
            Double lng = TestConstants.CENTER_LNG;

            // createRecommendation 메서드 활용 (Reflection 제거)
            PlaceAiResponse.PlaceRecommendation rec = createRecommendation(
                    TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_HIGH,
                    List.of(TestConstants.DELICIOUS_KEYWORD));
            PlaceAiResponse aiResponse = new PlaceAiResponse(List.of(rec));
            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

            // 반경 내 장소 없음
            given(placeRepository.findPlacesWithinRadiusByIds(
                    eq(List.of(TestConstants.PLACE_ID_1)), eq(lat), eq(lng), eq(TestConstants.DEFAULT_RADIUS)))
                    .willReturn(Collections.emptyList());

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }
    }
}
