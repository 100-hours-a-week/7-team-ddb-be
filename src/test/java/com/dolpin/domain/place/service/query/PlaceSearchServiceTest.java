package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.*;
import com.dolpin.domain.place.entity.*;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static com.dolpin.global.helper.PlaceTestHelper.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
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
        ReflectionTestUtils.setField(placeQueryService, "defaultSearchRadius", DEFAULT_RADIUS);
    }

    @Nested
    @DisplayName("searchPlaces 메서드 테스트")
    class SearchPlacesTest {

        @Test
        @DisplayName("검색어만 있는 경우 AI 검색 로직이 정상 동작한다")
        void searchPlaces_WithQueryOnly_PerformsAiSearch() {
            // given
            String query = "맛있는 파스타";
            Double lat = 37.5665;
            Double lng = 126.9780;

            // AI 응답 설정
            PlaceAiResponse.PlaceRecommendation rec1 = createRecommendation(1L, 0.95, List.of("맛있는", "이탈리안"));
            PlaceAiResponse.PlaceRecommendation rec2 = createRecommendation(2L, 0.88, List.of("분위기좋은", "데이트"));
            PlaceAiResponse aiResponse = new PlaceAiResponse(List.of(rec1, rec2));

            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

            // 거리 계산 결과
            List<PlaceWithDistance> nearbyPlaces = List.of(
                    createMockPlaceWithDistance(1L, "이탈리안 레스토랑", 37.5665, 126.9780, 150.0),
                    createMockPlaceWithDistance(2L, "로맨틱 파스타", 37.5670, 126.9785, 300.0)
            );
            given(placeRepository.findPlacesWithinRadiusByIds(eq(List.of(1L, 2L)), eq(lat), eq(lng), eq(DEFAULT_RADIUS)))
                    .willReturn(nearbyPlaces);

            // 키워드 포함 장소 조회
            List<Place> places = List.of(
                    createMockPlace(1L, "이탈리안 레스토랑", 37.5665, 126.9780),
                    createMockPlace(2L, "로맨틱 파스타", 37.5670, 126.9785)
            );
            given(placeRepository.findByIdsWithKeywords(eq(List.of(1L, 2L)))).willReturn(places);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getPlaces()).hasSize(2);

            // 유사도 점수 기준 정렬 확인 (높은 점수가 먼저)
            PlaceSearchResponse.PlaceDto firstPlace = result.getPlaces().get(0);
            PlaceSearchResponse.PlaceDto secondPlace = result.getPlaces().get(1);
            assertThat(firstPlace.getSimilarityScore()).isGreaterThan(secondPlace.getSimilarityScore());
            assertThat(firstPlace.getSimilarityScore()).isEqualTo(0.95);
            assertThat(secondPlace.getSimilarityScore()).isEqualTo(0.88);

            // AI 키워드 사용 확인
            assertThat(firstPlace.getKeywords()).containsExactlyInAnyOrder("맛있는", "이탈리안");
            assertThat(secondPlace.getKeywords()).containsExactlyInAnyOrder("분위기좋은", "데이트");

            verify(placeAiClient).recommendPlaces(query);
            verify(placeRepository).findPlacesWithinRadiusByIds(eq(List.of(1L, 2L)), eq(lat), eq(lng), eq(DEFAULT_RADIUS));
            verify(placeRepository).findByIdsWithKeywords(eq(List.of(1L, 2L)));
        }

        @Test
        @DisplayName("카테고리만 있는 경우 DB 직접 검색이 정상 동작한다")
        void searchPlaces_WithCategoryOnly_PerformsDbSearch() {
            // given
            String category = "카페";
            Double lat = 37.5665;
            Double lng = 126.9780;

            List<PlaceWithDistance> categoryPlaces = List.of(
                    createMockPlaceWithDistance(1L, "스타벅스", 37.5665, 126.9780, 100.0),
                    createMockPlaceWithDistance(2L, "투썸플레이스", 37.5670, 126.9785, 200.0)
            );
            given(placeRepository.findPlacesByCategoryWithinRadius(category, lat, lng, DEFAULT_RADIUS))
                    .willReturn(categoryPlaces);

            // 키워드 포함 장소들 (ID 명시적 설정)
            Place place1 = mock(Place.class);
            given(place1.getId()).willReturn(1L);
            List<PlaceKeyword> keywords1 = List.of("체인점", "넓은").stream().map(kw -> {
                Keyword keyword = mock(Keyword.class);
                given(keyword.getKeyword()).willReturn(kw);
                PlaceKeyword placeKeyword = mock(PlaceKeyword.class);
                given(placeKeyword.getKeyword()).willReturn(keyword);
                return placeKeyword;
            }).toList();
            given(place1.getKeywords()).willReturn(keywords1);

            Place place2 = mock(Place.class);
            given(place2.getId()).willReturn(2L);
            List<PlaceKeyword> keywords2 = List.of("디저트", "케이크").stream().map(kw -> {
                Keyword keyword = mock(Keyword.class);
                given(keyword.getKeyword()).willReturn(kw);
                PlaceKeyword placeKeyword = mock(PlaceKeyword.class);
                given(placeKeyword.getKeyword()).willReturn(keyword);
                return placeKeyword;
            }).toList();
            given(place2.getKeywords()).willReturn(keywords2);

            List<Place> places = List.of(place1, place2);
            given(placeRepository.findByIdsWithKeywords(eq(List.of(1L, 2L)))).willReturn(places);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(null, lat, lng, category);

            // then
            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getPlaces()).hasSize(2);

            // 카테고리 검색은 유사도 점수가 null
            assertThat(result.getPlaces().get(0).getSimilarityScore()).isNull();
            assertThat(result.getPlaces().get(1).getSimilarityScore()).isNull();

            // DB 키워드 사용 확인
            assertThat(result.getPlaces().get(0).getKeywords()).containsExactlyInAnyOrder("체인점", "넓은");

            verify(placeRepository).findPlacesByCategoryWithinRadius(category, lat, lng, DEFAULT_RADIUS);
            verify(placeRepository).findByIdsWithKeywords(eq(List.of(1L, 2L)));
            verifyNoInteractions(placeAiClient);
        }

        @Test
        @DisplayName("검색어와 카테고리 동시 입력 시 예외가 발생한다")
        void searchPlaces_WithBothQueryAndCategory_ThrowsException() {
            // given
            String query = "맛있는 카페";
            String category = "카페";
            Double lat = 37.5665;
            Double lng = 126.9780;

            // when & then
            assertThatThrownBy(() -> placeQueryService.searchPlaces(query, lat, lng, category))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("검색어와 카테고리 중 하나만 선택해주세요")
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.INVALID_PARAMETER);
        }

        @Test
        @DisplayName("검색어와 카테고리 모두 없을 때 예외가 발생한다")
        void searchPlaces_WithNeitherQueryNorCategory_ThrowsException() {
            // given
            Double lat = 37.5665;
            Double lng = 126.9780;

            // when & then
            assertThatThrownBy(() -> placeQueryService.searchPlaces(null, lat, lng, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("검색어 또는 카테고리가 필요합니다");

            assertThatThrownBy(() -> placeQueryService.searchPlaces("", lat, lng, ""))
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
        @DisplayName("위치 정보(lat, lng) 누락 시 예외가 발생한다")
        void searchPlaces_WithMissingLocation_ThrowsException(String query, Double lat, Double lng) {
            // when & then
            assertThatThrownBy(() -> placeQueryService.searchPlaces(query, lat, lng, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("위치 정보가 필요합니다");
        }

        @Test
        @DisplayName("AI 서비스 응답이 null일 때 빈 결과를 반환한다")
        void searchPlaces_WithNullAiResponse_ReturnsEmptyResult() {
            // given
            String query = "존재하지 않는 검색어";
            Double lat = 37.5665;
            Double lng = 126.9780;

            given(placeAiClient.recommendPlaces(query)).willReturn(null);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("AI 서비스 응답이 빈 결과일 때 빈 결과를 반환한다")
        void searchPlaces_WithEmptyAiResponse_ReturnsEmptyResult() {
            // given
            String query = "존재하지 않는 검색어";
            Double lat = 37.5665;
            Double lng = 126.9780;

            PlaceAiResponse emptyResponse = new PlaceAiResponse(Collections.emptyList());
            given(placeAiClient.recommendPlaces(query)).willReturn(emptyResponse);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("반경 내 장소가 없을 때 빈 결과를 반환한다")
        void searchPlaces_WithNoPlacesInRadius_ReturnsEmptyResult() {
            // given
            String query = "맛있는 카페";
            Double lat = 37.5665;
            Double lng = 126.9780;

            PlaceAiResponse.PlaceRecommendation rec = createRecommendation(1L, 0.95, List.of("맛있는"));
            PlaceAiResponse aiResponse = new PlaceAiResponse(List.of(rec));
            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

            // 반경 내 장소 없음
            given(placeRepository.findPlacesWithinRadiusByIds(eq(List.of(1L)), eq(lat), eq(lng), eq(DEFAULT_RADIUS)))
                    .willReturn(Collections.emptyList());

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getPlaces()).isEmpty();
        }

        @Test
        @DisplayName("유사도 점수 기준 정렬이 정상 동작한다")
        void searchPlaces_SortsBySimilarityScore() {
            // given
            String query = "카페";
            Double lat = 37.5665;
            Double lng = 126.9780;

            // 유사도 점수가 다른 추천 결과 (의도적으로 순서를 섞음)
            PlaceAiResponse aiResponse = new PlaceAiResponse(List.of(
                    createRecommendation(1L, 0.75, List.of("평범한")),
                    createRecommendation(2L, 0.95, List.of("최고")),
                    createRecommendation(3L, 0.88, List.of("좋은"))
            ));
            given(placeAiClient.recommendPlaces(query)).willReturn(aiResponse);

            List<PlaceWithDistance> nearbyPlaces = List.of(
                    createMockPlaceWithDistance(1L, "평범한 카페", 37.5665, 126.9780, 100.0),
                    createMockPlaceWithDistance(2L, "최고의 카페", 37.5666, 126.9781, 200.0),
                    createMockPlaceWithDistance(3L, "좋은 카페", 37.5667, 126.9782, 300.0)
            );
            given(placeRepository.findPlacesWithinRadiusByIds(eq(List.of(1L, 2L, 3L)), eq(lat), eq(lng), eq(DEFAULT_RADIUS)))
                    .willReturn(nearbyPlaces);

            // Place 객체들을 개별적으로 생성 (ID와 location 명시적 설정)
            Place place1 = mock(Place.class);
            given(place1.getId()).willReturn(1L);
            given(place1.getName()).willReturn("평범한 카페");
            given(place1.getLocation()).willReturn(GEOMETRY_FACTORY.createPoint(new Coordinate(126.9780, 37.5665)));
            List<PlaceKeyword> keywords1 = List.of("평범한").stream().map(kw -> {
                Keyword keyword = mock(Keyword.class);
                given(keyword.getKeyword()).willReturn(kw);
                PlaceKeyword placeKeyword = mock(PlaceKeyword.class);
                given(placeKeyword.getKeyword()).willReturn(keyword);
                return placeKeyword;
            }).toList();
            given(place1.getKeywords()).willReturn(keywords1);

            Place place2 = mock(Place.class);
            given(place2.getId()).willReturn(2L);
            given(place2.getName()).willReturn("최고의 카페");
            given(place2.getLocation()).willReturn(GEOMETRY_FACTORY.createPoint(new Coordinate(126.9781, 37.5666)));
            List<PlaceKeyword> keywords2 = List.of("최고").stream().map(kw -> {
                Keyword keyword = mock(Keyword.class);
                given(keyword.getKeyword()).willReturn(kw);
                PlaceKeyword placeKeyword = mock(PlaceKeyword.class);
                given(placeKeyword.getKeyword()).willReturn(keyword);
                return placeKeyword;
            }).toList();
            given(place2.getKeywords()).willReturn(keywords2);

            Place place3 = mock(Place.class);
            given(place3.getId()).willReturn(3L);
            given(place3.getName()).willReturn("좋은 카페");
            given(place3.getLocation()).willReturn(GEOMETRY_FACTORY.createPoint(new Coordinate(126.9782, 37.5667)));
            List<PlaceKeyword> keywords3 = List.of("좋은").stream().map(kw -> {
                Keyword keyword = mock(Keyword.class);
                given(keyword.getKeyword()).willReturn(kw);
                PlaceKeyword placeKeyword = mock(PlaceKeyword.class);
                given(placeKeyword.getKeyword()).willReturn(keyword);
                return placeKeyword;
            }).toList();
            given(place3.getKeywords()).willReturn(keywords3);

            List<Place> places = List.of(place1, place2, place3);
            given(placeRepository.findByIdsWithKeywords(eq(List.of(1L, 2L, 3L)))).willReturn(places);

            // when
            PlaceSearchResponse result = placeQueryService.searchPlaces(query, lat, lng, null);

            // then
            assertThat(result.getPlaces()).hasSize(3);

            // 유사도 점수 순으로 정렬되어야 함 (0.95 > 0.88 > 0.75)
            List<Double> scores = result.getPlaces().stream()
                    .map(PlaceSearchResponse.PlaceDto::getSimilarityScore)
                    .toList();
            assertThat(scores).containsExactly(0.95, 0.88, 0.75);

            // 해당하는 장소 이름도 확인
            List<String> names = result.getPlaces().stream()
                    .map(PlaceSearchResponse.PlaceDto::getName)
                    .toList();
            assertThat(names).containsExactly("최고의 카페", "좋은 카페", "평범한 카페");
        }
    }
}
