package com.dolpin.domain.place.service.query.factory;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.service.factory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceDtoFactory 테스트")
class PlaceDtoFactoryTest {

    @Mock
    private PlaceDtoStrategy aiSearchStrategy;

    @Mock
    private PlaceDtoStrategy distanceBasedStrategy;

    @Mock
    private PlaceDtoStrategy standardStrategy;

    private PlaceDtoFactory placeDtoFactory;
    private Place testPlace;

    @BeforeEach
    void setUp() {
        // Mock 전략들의 동작 설정
        given(aiSearchStrategy.getPriority()).willReturn(1);
        given(distanceBasedStrategy.getPriority()).willReturn(2);
        given(standardStrategy.getPriority()).willReturn(999);

        List<PlaceDtoStrategy> strategies = Arrays.asList(
                standardStrategy,      // 의도적으로 순서를 섞어서 정렬 테스트
                aiSearchStrategy,
                distanceBasedStrategy
        );

        placeDtoFactory = new PlaceDtoFactory(strategies);

        testPlace = Place.builder()
                .id(1L)
                .name("테스트 장소")
                .imageUrl("test.jpg")
                .build();
    }

    @Test
    @DisplayName("AI 검색 전략 선택 - 유사도 점수가 있는 경우")
    void createPlaceDto_WithSimilarityScore_UsesAiSearchStrategy() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .similarityScore(0.9)
                .momentCount(5L)
                .isBookmarked(true)
                .build();

        PlaceSearchResponse.PlaceDto expectedDto = createExpectedDto();

        given(aiSearchStrategy.supports(context)).willReturn(true);
        given(aiSearchStrategy.createPlaceDto(context)).willReturn(expectedDto);

        // when
        PlaceSearchResponse.PlaceDto result = placeDtoFactory.createPlaceDto(context);

        // then
        assertThat(result).isEqualTo(expectedDto);
        verify(aiSearchStrategy).createPlaceDto(context);
    }

    @Test
    @DisplayName("거리 기반 전략 선택 - 거리가 있고 유사도 점수가 없는 경우")
    void createPlaceDto_WithDistanceOnly_UsesDistanceBasedStrategy() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .distance(100.0)
                .momentCount(3L)
                .isBookmarked(false)
                .build();

        PlaceSearchResponse.PlaceDto expectedDto = createExpectedDto();

        given(aiSearchStrategy.supports(context)).willReturn(false);
        given(distanceBasedStrategy.supports(context)).willReturn(true);
        given(distanceBasedStrategy.createPlaceDto(context)).willReturn(expectedDto);

        // when
        PlaceSearchResponse.PlaceDto result = placeDtoFactory.createPlaceDto(context);

        // then
        assertThat(result).isEqualTo(expectedDto);
        verify(distanceBasedStrategy).createPlaceDto(context);
    }

    @Test
    @DisplayName("표준 전략 선택 - 특별한 조건이 없는 경우")
    void createPlaceDto_BasicContext_UsesStandardStrategy() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .momentCount(2L)
                .isBookmarked(false)
                .build();

        PlaceSearchResponse.PlaceDto expectedDto = createExpectedDto();

        given(aiSearchStrategy.supports(context)).willReturn(false);
        given(distanceBasedStrategy.supports(context)).willReturn(false);
        given(standardStrategy.supports(context)).willReturn(true);
        given(standardStrategy.createPlaceDto(context)).willReturn(expectedDto);

        // when
        PlaceSearchResponse.PlaceDto result = placeDtoFactory.createPlaceDto(context);

        // then
        assertThat(result).isEqualTo(expectedDto);
        verify(standardStrategy).createPlaceDto(context);
    }

    @Test
    @DisplayName("우선순위가 높은 전략 먼저 선택")
    void createPlaceDto_MultipleSupportingStrategies_SelectsHigherPriority() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .build();

        PlaceSearchResponse.PlaceDto expectedDto = createExpectedDto();

        // 모든 전략이 지원한다고 가정
        given(aiSearchStrategy.supports(context)).willReturn(true);
        given(aiSearchStrategy.createPlaceDto(context)).willReturn(expectedDto);

        // when
        PlaceSearchResponse.PlaceDto result = placeDtoFactory.createPlaceDto(context);

        // then - 가장 높은 우선순위(낮은 숫자)인 AI 전략이 선택되어야 함
        assertThat(result).isEqualTo(expectedDto);
        verify(aiSearchStrategy).createPlaceDto(context);
    }

    @Test
    @DisplayName("지원하는 전략이 없는 경우 - 예외 발생")
    void createPlaceDto_NoSupportingStrategy_ThrowsException() {
        // given
        PlaceDtoContext context = PlaceDtoContext.builder()
                .place(testPlace)
                .build();

        given(aiSearchStrategy.supports(context)).willReturn(false);
        given(distanceBasedStrategy.supports(context)).willReturn(false);
        given(standardStrategy.supports(context)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> placeDtoFactory.createPlaceDto(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("지원하는 DTO 생성 전략을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("편의 메서드 - createPlaceDto(Place, Map, Map)")
    void createPlaceDto_ConvenienceMethod_CreatesContextAndCallsStrategy() {
        // given
        Map<Long, Long> momentCountMap = Map.of(1L, 5L);
        Map<Long, Boolean> bookmarkStatusMap = Map.of(1L, true);
        PlaceSearchResponse.PlaceDto expectedDto = createExpectedDto();

        given(standardStrategy.supports(any(PlaceDtoContext.class))).willReturn(true);
        given(aiSearchStrategy.supports(any(PlaceDtoContext.class))).willReturn(false);
        given(distanceBasedStrategy.supports(any(PlaceDtoContext.class))).willReturn(false);
        given(standardStrategy.createPlaceDto(any(PlaceDtoContext.class))).willReturn(expectedDto);

        // when
        PlaceSearchResponse.PlaceDto result = placeDtoFactory.createPlaceDto(
                testPlace, momentCountMap, bookmarkStatusMap);

        // then
        assertThat(result).isEqualTo(expectedDto);
        verify(standardStrategy).createPlaceDto(any(PlaceDtoContext.class));
    }

    @Test
    @DisplayName("편의 메서드 - createAiSearchDto")
    void createAiSearchDto_CreatesContextWithAiData() {
        // given
        Double similarityScore = 0.9;
        List<String> aiKeywords = Arrays.asList("AI", "검색");
        Map<Long, Long> momentCountMap = Map.of(1L, 5L);
        Map<Long, Boolean> bookmarkStatusMap = Map.of(1L, true);
        PlaceSearchResponse.PlaceDto expectedDto = createExpectedDto();

        given(aiSearchStrategy.supports(any(PlaceDtoContext.class))).willReturn(true);
        given(aiSearchStrategy.createPlaceDto(any(PlaceDtoContext.class))).willReturn(expectedDto);

        // when
        PlaceSearchResponse.PlaceDto result = placeDtoFactory.createAiSearchDto(
                testPlace, similarityScore, aiKeywords, momentCountMap, bookmarkStatusMap);

        // then
        assertThat(result).isEqualTo(expectedDto);
        verify(aiSearchStrategy).createPlaceDto(any(PlaceDtoContext.class));
    }

    @Test
    @DisplayName("편의 메서드 - createDistanceBasedDto")
    void createDistanceBasedDto_CreatesContextWithDistanceData() {
        // given
        Double distance = 150.0;
        Map<Long, Long> momentCountMap = Map.of(1L, 3L);
        Map<Long, Boolean> bookmarkStatusMap = Map.of(1L, false);
        PlaceSearchResponse.PlaceDto expectedDto = createExpectedDto();

        given(distanceBasedStrategy.supports(any(PlaceDtoContext.class))).willReturn(true);
        given(distanceBasedStrategy.createPlaceDto(any(PlaceDtoContext.class))).willReturn(expectedDto);

        // when
        PlaceSearchResponse.PlaceDto result = placeDtoFactory.createDistanceBasedDto(
                testPlace, distance, momentCountMap, bookmarkStatusMap);

        // then
        assertThat(result).isEqualTo(expectedDto);
        verify(distanceBasedStrategy).createPlaceDto(any(PlaceDtoContext.class));
    }

    @Test
    @DisplayName("빈 전략 리스트로 생성 - 정상 동작")
    void constructor_EmptyStrategies_WorksCorrectly() {
        // given
        List<PlaceDtoStrategy> emptyStrategies = Collections.emptyList();
        PlaceDtoFactory factory = new PlaceDtoFactory(emptyStrategies);
        PlaceDtoContext context = PlaceDtoContext.builder().place(testPlace).build();

        // when & then
        assertThatThrownBy(() -> factory.createPlaceDto(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("지원하는 DTO 생성 전략을 찾을 수 없습니다");
    }

    private PlaceSearchResponse.PlaceDto createExpectedDto() {
        return PlaceSearchResponse.PlaceDto.builder()
                .id(1L)
                .name("테스트 장소")
                .thumbnail("test.jpg")
                .distance(100.0)
                .momentCount(5L)
                .isBookmarked(true)
                .build();
    }
}
