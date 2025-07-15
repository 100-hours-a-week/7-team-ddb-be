package com.dolpin.domain.place.service.query.strategy;

import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.factory.PlaceDtoFactory;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import com.dolpin.domain.place.service.strategy.AiQuerySearchStrategy;
import com.dolpin.domain.place.service.strategy.PlaceSearchContext;
import com.dolpin.domain.place.service.strategy.PlaceSearchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiQuerySearchStrategy 테스트")
class AiQuerySearchStrategyTest {

    @InjectMocks
    private AiQuerySearchStrategy aiQuerySearchStrategy;

    @Mock
    private PlaceAiClient placeAiClient;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private PlaceBookmarkQueryService bookmarkQueryService;

    @Mock
    private PlaceDtoFactory placeDtoFactory;

    private PlaceSearchContext testContext;

    @BeforeEach
    void setUp() {
        testContext = PlaceSearchContext.builder()
                .query("맛있는 파스타")
                .lat(37.5665)
                .lng(126.9780)
                .userId(1L)
                .build();
    }

    @Test
    @DisplayName("지원하는 검색 타입 확인 - AI_QUERY")
    void supports_AiQueryType_ReturnsTrue() {
        // when
        boolean supports = aiQuerySearchStrategy.supports(PlaceSearchType.AI_QUERY);

        // then
        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("지원하지 않는 검색 타입 확인 - CATEGORY")
    void supports_CategoryType_ReturnsFalse() {
        // when
        boolean supports = aiQuerySearchStrategy.supports(PlaceSearchType.CATEGORY);

        // then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("우선순위 확인")
    void getPriority_ReturnsCorrectValue() {
        // when
        int priority = aiQuerySearchStrategy.getPriority();

        // then
        assertThat(priority).isEqualTo(1);
    }

    @Test
    @DisplayName("AI 검색 성공 - 추천 결과 있음")
    void search_WithRecommendations_ReturnsSuccessfully() {
        // given
        PlaceAiResponse aiResponse = createAiResponseWithRecommendations();
        List<PlaceWithDistance> placesWithDistance = createPlacesWithDistance(); // 2개 반환
        List<Place> places = createPlaces();
        List<Object[]> momentCountResults = createMomentCountResults();
        Map<Long, Boolean> bookmarkStatusMap = Map.of(1L, true, 2L, false);
        PlaceSearchResponse.PlaceDto expectedDto = createExpectedDto();

        given(placeAiClient.recommendPlacesAsync(testContext.getQuery()))
                .willReturn(Mono.just(aiResponse));
        given(placeRepository.findPlacesWithinRadiusByIds(anyList(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(placesWithDistance);
        given(placeRepository.findByIdsWithKeywords(anyList()))
                .willReturn(places);
        given(momentRepository.countPublicMomentsByPlaceIds(anyList()))
                .willReturn(momentCountResults);
        given(bookmarkQueryService.getBookmarkStatusMap(anyLong(), anyList()))
                .willReturn(bookmarkStatusMap);
        given(placeDtoFactory.createAiSearchDto(any(Place.class), any(Double.class), any(List.class), any(Map.class), any(Map.class)))
                .willReturn(expectedDto);

        // when
        List<PlaceSearchResponse.PlaceDto> result = aiQuerySearchStrategy.search(testContext).block();

        // then
        assertThat(result).hasSize(2);

        verify(placeAiClient).recommendPlacesAsync(testContext.getQuery());
        // 2번 호출되므로 times(2)로 수정
        verify(placeDtoFactory, times(2)).createAiSearchDto(any(Place.class), any(Double.class), any(List.class), any(Map.class), any(Map.class));
    }

    @Test
    @DisplayName("AI 검색 - 카테고리 폴백")
    void search_WithCategoryFallback_ReturnsSuccessfully() {
        // given
        PlaceAiResponse aiResponse = createAiResponseWithCategory();
        List<PlaceWithDistance> placesWithDistance = createPlacesWithDistance();
        List<Place> places = createPlaces();
        List<Object[]> momentCountResults = createMomentCountResults();
        Map<Long, Boolean> bookmarkStatusMap = Map.of(1L, true, 2L, false);
        PlaceSearchResponse.PlaceDto expectedDto = createExpectedDto();

        given(placeAiClient.recommendPlacesAsync(testContext.getQuery()))
                .willReturn(Mono.just(aiResponse));
        given(placeRepository.findPlacesByCategoryWithinRadius(anyString(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(placesWithDistance);
        given(placeRepository.findByIdsWithKeywords(anyList()))
                .willReturn(places);
        given(momentRepository.countPublicMomentsByPlaceIds(anyList()))
                .willReturn(momentCountResults);
        given(bookmarkQueryService.getBookmarkStatusMap(anyLong(), anyList()))
                .willReturn(bookmarkStatusMap);

        // 🚨 여기가 문제! createAiSearchDto가 아니라 createDistanceBasedDto를 사용해야 함
        given(placeDtoFactory.createDistanceBasedDto(any(Place.class), any(Double.class), any(Map.class), any(Map.class)))
                .willReturn(expectedDto);

        // when
        List<PlaceSearchResponse.PlaceDto> result = aiQuerySearchStrategy.search(testContext).block();

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(2);

        verify(placeAiClient).recommendPlacesAsync(testContext.getQuery());
        verify(placeRepository).findPlacesByCategoryWithinRadius(eq("이탈리안"), anyDouble(), anyDouble(), anyDouble());
        verify(placeDtoFactory, times(2)).createDistanceBasedDto(any(Place.class), any(Double.class), any(Map.class), any(Map.class));
    }

    @Test
    @DisplayName("AI 검색 - 빈 결과")
    void search_WithEmptyResult_ReturnsEmptyList() {
        // given
        PlaceAiResponse emptyResponse = PlaceAiResponse.builder()
                .recommendations(Collections.emptyList())
                .placeCategory("")
                .build();

        given(placeAiClient.recommendPlacesAsync(testContext.getQuery()))
                .willReturn(Mono.just(emptyResponse));

        // when
        List<PlaceSearchResponse.PlaceDto> result = aiQuerySearchStrategy.search(testContext).block();

        // then
        assertThat(result).isEmpty();

        verifyNoInteractions(placeRepository, momentRepository, bookmarkQueryService, placeDtoFactory);
    }

    @Test
    @DisplayName("DevToken이 있는 경우 - 토큰과 함께 AI 클라이언트 호출")
    void search_WithDevToken_CallsAiClientWithToken() {
        // given
        String devToken = "test-dev-token";
        PlaceSearchContext contextWithToken = PlaceSearchContext.builder()
                .query("테스트 쿼리")
                .lat(37.5665)
                .lng(126.9780)
                .userId(1L)
                .devToken(devToken)
                .build();

        PlaceAiResponse aiResponse = createAiResponseWithRecommendations();

        given(placeAiClient.recommendPlacesAsync(contextWithToken.getQuery(), devToken))
                .willReturn(Mono.just(aiResponse));
        given(placeRepository.findPlacesWithinRadiusByIds(anyList(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(Collections.emptyList());

        // when
        List<PlaceSearchResponse.PlaceDto> result = aiQuerySearchStrategy.search(contextWithToken).block();

        // then
        assertThat(result).isEmpty();

        verify(placeAiClient).recommendPlacesAsync(contextWithToken.getQuery(), devToken);
    }

    @Test
    @DisplayName("AI 클라이언트 에러 - 에러 전파")
    void search_AiClientError_PropagatesError() {
        // given
        RuntimeException expectedException = new RuntimeException("AI 서비스 오류");
        given(placeAiClient.recommendPlacesAsync(testContext.getQuery()))
                .willReturn(Mono.error(expectedException));

        // when & then
        try {
            aiQuerySearchStrategy.search(testContext).block();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("AI 서비스 오류");
        }
    }

    private PlaceAiResponse createAiResponseWithRecommendations() {
        List<PlaceAiResponse.PlaceRecommendation> recommendations = Arrays.asList(
                PlaceAiResponse.PlaceRecommendation.builder()
                        .id(1L)
                        .similarityScore(0.9)
                        .keyword(Arrays.asList("파스타", "맛집"))
                        .build(),
                PlaceAiResponse.PlaceRecommendation.builder()
                        .id(2L)
                        .similarityScore(0.8)
                        .keyword(Arrays.asList("이탈리안", "레스토랑"))
                        .build()
        );

        return PlaceAiResponse.builder()
                .recommendations(recommendations)
                .placeCategory(null)
                .build();
    }

    private PlaceAiResponse createAiResponseWithCategory() {
        return PlaceAiResponse.builder()
                .recommendations(null)
                .placeCategory("이탈리안")
                .build();
    }

    private List<PlaceWithDistance> createPlacesWithDistance() {
        return Arrays.asList(
                createPlaceWithDistance(1L, "테스트 파스타집", 100.0),
                createPlaceWithDistance(2L, "이탈리안 레스토랑", 200.0)
        );
    }

    private PlaceWithDistance createPlaceWithDistance(Long id, String name, Double distance) {
        return new PlaceWithDistance() {
            @Override public Long getId() { return id; }
            @Override public String getName() { return name; }
            @Override public String getCategory() { return "이탈리안"; }
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
                .name("테스트 파스타집")
                .imageUrl("image1.jpg")
                .build();

        Place place2 = Place.builder()
                .id(2L)
                .name("이탈리안 레스토랑")
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

    private PlaceSearchResponse.PlaceDto createExpectedDto() {
        return PlaceSearchResponse.PlaceDto.builder()
                .id(1L)
                .name("테스트 파스타집")
                .thumbnail("image1.jpg")
                .distance(100.0)
                .momentCount(5L)
                .isBookmarked(true)
                .similarityScore(0.9)
                .build();
    }
}
