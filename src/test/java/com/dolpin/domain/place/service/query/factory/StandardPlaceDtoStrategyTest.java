// StandardPlaceDtoStrategy 테스트
package com.dolpin.domain.place.service.query.factory;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.entity.PlaceKeyword;
import com.dolpin.domain.place.entity.Keyword;
import com.dolpin.domain.place.service.factory.StandardPlaceDtoStrategy;
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
@DisplayName("StandardPlaceDtoStrategy 테스트")
class StandardPlaceDtoStrategyTest {

    private StandardPlaceDtoStrategy strategy;
    private Place testPlace;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        strategy = new StandardPlaceDtoStrategy();
        geometryFactory = new GeometryFactory();

        Point location = geometryFactory.createPoint(new Coordinate(126.9780, 37.5665));

        testPlace = Place.builder()
                .id(1L)
                .name("표준 카페")
                .imageUrl("standard.jpg")
                .location(location)
                .keywords(createPlaceKeywords())
                .build();
    }

    @Test
    @DisplayName("지원 조건 확인 - 모든 컨텍스트 지원")
    void supports_AllContexts_ReturnsTrue() {
        // given
        PlaceDtoContext context1 = PlaceDtoContext.builder().place(testPlace).build();
        PlaceDtoContext context2 = PlaceDtoContext.builder().place(testPlace).distance(100.0).build();
        PlaceDtoContext context3 = PlaceDtoContext.builder().place(testPlace).similarityScore(0.9).build();

        // when & then
        assertThat(strategy.supports(context1)).isTrue();
        assertThat(strategy.supports(context2)).isTrue();
        assertThat(strategy.supports(context3)).isTrue();
    }

    @Test
    @DisplayName("우선순위 확인 - 가장 낮은 우선순위")
    void getPriority_ReturnsLowestPriority() {
        // when
        int priority = strategy.getPriority();

        // then
        assertThat(priority).isEqualTo(999);
    }

    @Test
    @DisplayName("거리 변환 - 미터 단위")
    void createPlaceDto_DistanceInMeters() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .distance(500.0) // 500미터
                .momentCount(3L)
                .isBookmarked(false)
                .build();

        // when
        PlaceSearchResponse.PlaceDto result = strategy.createPlaceDto(context);

        // then
        assertThat(result.getDistance()).isEqualTo(500.0);
    }

    @Test
    @DisplayName("거리 변환 - 킬로미터 단위 (소수점 첫째자리)")
    void createPlaceDto_DistanceInKilometers() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .distance(1500.0) // 1.5km
                .momentCount(3L)
                .isBookmarked(false)
                .build();

        // when
        PlaceSearchResponse.PlaceDto result = strategy.createPlaceDto(context);

        // then
        assertThat(result.getDistance()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("거리 변환 - null 거리")
    void createPlaceDto_NullDistance() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .distance(null)
                .build();

        // when
        PlaceSearchResponse.PlaceDto result = strategy.createPlaceDto(context);

        // then
        assertThat(result.getDistance()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("AI 키워드 우선 사용")
    void createPlaceDto_WithAiKeywords_PrioritizesAiKeywords() {
        // given
        List<String> aiKeywords = Arrays.asList("AI추천", "특별메뉴");
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .aiKeywords(aiKeywords)
                .build();

        // when
        PlaceSearchResponse.PlaceDto result = strategy.createPlaceDto(context);

        // then
        assertThat(result.getKeywords()).isEqualTo(aiKeywords);
    }

    @Test
    @DisplayName("기본 키워드 사용")
    void createPlaceDto_WithoutAiKeywords_UsesOriginalKeywords() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .build();

        // when
        PlaceSearchResponse.PlaceDto result = strategy.createPlaceDto(context);

        // then
        assertThat(result.getKeywords()).hasSize(2);
        assertThat(result.getKeywords()).contains("카페", "음료");
    }

    @Test
    @DisplayName("위치 정보 매핑")
    void createPlaceDto_LocationMapping() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
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
