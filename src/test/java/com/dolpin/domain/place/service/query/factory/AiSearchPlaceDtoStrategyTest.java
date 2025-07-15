// AiSearchPlaceDtoStrategy 테스트
package com.dolpin.domain.place.service.query.factory;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.entity.Keyword;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.entity.PlaceKeyword;
import com.dolpin.domain.place.service.factory.AiSearchPlaceDtoStrategy;
import com.dolpin.domain.place.service.factory.PlaceDtoContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiSearchPlaceDtoStrategy 테스트")
class AiSearchPlaceDtoStrategyTest {

    private AiSearchPlaceDtoStrategy strategy;
    private Place testPlace;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        strategy = new AiSearchPlaceDtoStrategy();
        geometryFactory = new GeometryFactory();

        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        testPlace = Place.builder()
                .id(1L)
                .name("테스트 카페")
                .imageUrl("test.jpg")
                .location(location)
                .keywords(createPlaceKeywords())
                .build();
    }

    @Test
    @DisplayName("지원 조건 확인 - 유사도 점수가 있는 경우")
    void supports_WithSimilarityScore_ReturnsTrue() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .similarityScore(0.9)
                .build();

        // when
        boolean supports = strategy.supports(context);

        // then
        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("지원 조건 확인 - AI 키워드가 있는 경우")
    void supports_WithAiKeywords_ReturnsTrue() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .aiKeywords(Arrays.asList("AI", "추천"))
                .build();

        // when
        boolean supports = strategy.supports(context);

        // then
        assertThat(supports).isTrue();
    }

    @Test
    @DisplayName("지원 조건 확인 - 둘 다 없는 경우")
    void supports_WithoutAiData_ReturnsFalse() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .distance(100.0)
                .build();

        // when
        boolean supports = strategy.supports(context);

        // then
        assertThat(supports).isFalse();
    }

    @Test
    @DisplayName("우선순위 확인")
    void getPriority_ReturnsHighPriority() {
        // when
        int priority = strategy.getPriority();

        // then
        assertThat(priority).isEqualTo(1);
    }

    @Test
    @DisplayName("DTO 생성 - AI 키워드 우선 사용")
    void createPlaceDto_WithAiKeywords_PrioritizesAiKeywords() {
        // given
        List<String> aiKeywords = Arrays.asList("AI추천", "맛집");
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .similarityScore(0.9)
                .aiKeywords(aiKeywords)
                .momentCount(5L)
                .isBookmarked(true)
                .build();

        // when
        PlaceSearchResponse.PlaceDto result = strategy.createPlaceDto(context);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("테스트 카페");
        assertThat(result.getThumbnail()).isEqualTo("test.jpg");
        assertThat(result.getMomentCount()).isEqualTo(5L);
        assertThat(result.getIsBookmarked()).isTrue();
        assertThat(result.getSimilarityScore()).isEqualTo(0.9);
        assertThat(result.getKeywords()).isEqualTo(aiKeywords);
        assertThat(result.getDistance()).isNull();
    }

    @Test
    @DisplayName("DTO 생성 - AI 키워드 없을 때 기존 키워드 사용")
    void createPlaceDto_WithoutAiKeywords_UseOriginalKeywords() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .similarityScore(0.8)
                .momentCount(3L)
                .isBookmarked(false)
                .build();

        // when
        PlaceSearchResponse.PlaceDto result = strategy.createPlaceDto(context);

        // then
        assertThat(result.getKeywords()).hasSize(2);
        assertThat(result.getKeywords()).contains("카페", "음료");
        assertThat(result.getSimilarityScore()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("위치 정보 매핑 확인")
    void createPlaceDto_LocationMapping() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .similarityScore(0.9)
                .build();

        // when
        PlaceSearchResponse.PlaceDto result = strategy.createPlaceDto(context);

        // then
        Map<String, Object> location = result.getLocation();
        assertThat(location.get("type")).isEqualTo("Point");
        double[] coordinates = (double[]) location.get("coordinates");
        assertThat(coordinates[0]).isEqualTo(126.9780);
        assertThat(coordinates[1]).isEqualTo(37.5665);
    }

    private List<PlaceKeyword> createPlaceKeywords() {
        Keyword keyword1 = Keyword.builder().keyword("카페").build();
        Keyword keyword2 = Keyword.builder().keyword("음료").build();

        PlaceKeyword pk1 = PlaceKeyword.builder().keyword(keyword1).build();
        PlaceKeyword pk2 = PlaceKeyword.builder().keyword(keyword2).build();

        return Arrays.asList(pk1, pk2);
    }
}
