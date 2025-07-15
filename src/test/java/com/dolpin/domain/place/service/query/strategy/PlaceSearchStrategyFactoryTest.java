package com.dolpin.domain.place.service.query.strategy;

import com.dolpin.domain.place.service.strategy.PlaceSearchStrategy;
import com.dolpin.domain.place.service.strategy.PlaceSearchStrategyFactory;
import com.dolpin.domain.place.service.strategy.PlaceSearchType;
import com.dolpin.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceSearchStrategyFactory 테스트")
class PlaceSearchStrategyFactoryTest {

    @Mock
    private PlaceSearchStrategy aiQueryStrategy;

    @Mock
    private PlaceSearchStrategy categoryStrategy;

    @Mock
    private PlaceSearchStrategy defaultStrategy;

    private PlaceSearchStrategyFactory strategyFactory;

    @BeforeEach
    void setUp() {
        // 기본 우선순위만 설정 (각 테스트에서 필요한 stubbing은 개별적으로 설정)
        given(aiQueryStrategy.getPriority()).willReturn(1);
        given(categoryStrategy.getPriority()).willReturn(2);
        given(defaultStrategy.getPriority()).willReturn(999);

        List<PlaceSearchStrategy> strategies = Arrays.asList(
                defaultStrategy, // 의도적으로 순서를 섞어서 정렬 테스트
                aiQueryStrategy,
                categoryStrategy
        );

        strategyFactory = new PlaceSearchStrategyFactory(strategies);
    }

    @Test
    @DisplayName("AI 쿼리 타입 - 올바른 전략 반환")
    void getStrategy_AiQueryType_ReturnsCorrectStrategy() {
        // given
        given(aiQueryStrategy.supports(PlaceSearchType.AI_QUERY)).willReturn(true);

        // when
        PlaceSearchStrategy strategy = strategyFactory.getStrategy(PlaceSearchType.AI_QUERY);

        // then
        assertThat(strategy).isEqualTo(aiQueryStrategy);
    }

    @Test
    @DisplayName("카테고리 타입 - 올바른 전략 반환")
    void getStrategy_CategoryType_ReturnsCorrectStrategy() {
        // given
        given(aiQueryStrategy.supports(PlaceSearchType.CATEGORY)).willReturn(false);
        given(categoryStrategy.supports(PlaceSearchType.CATEGORY)).willReturn(true);

        // when
        PlaceSearchStrategy strategy = strategyFactory.getStrategy(PlaceSearchType.CATEGORY);

        // then
        assertThat(strategy).isEqualTo(categoryStrategy);
    }

    @Test
    @DisplayName("지원하지 않는 검색 타입 - 예외 발생")
    void getStrategy_UnsupportedType_ThrowsException() {
        // given - 모든 전략이 지원하지 않는 타입 시뮬레이션
        PlaceSearchType unsupportedType = PlaceSearchType.AI_QUERY;
        given(aiQueryStrategy.supports(unsupportedType)).willReturn(false);
        given(categoryStrategy.supports(unsupportedType)).willReturn(false);
        given(defaultStrategy.supports(unsupportedType)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> strategyFactory.getStrategy(unsupportedType))
                .isInstanceOf(BusinessException.class)
                .hasMessage("지원하지 않는 검색 타입: " + unsupportedType);
    }

    @Test
    @DisplayName("전략 우선순위 정렬 - 낮은 우선순위값이 먼저 선택됨")
    void getStrategy_PrioritySorting_SelectsHigherPriorityStrategy() {
        // given - 두 전략이 모두 같은 타입을 지원하는 경우
        PlaceSearchStrategy highPriorityStrategy = categoryStrategy; // priority = 2
        PlaceSearchStrategy lowPriorityStrategy = defaultStrategy;   // priority = 999

        given(highPriorityStrategy.supports(PlaceSearchType.CATEGORY)).willReturn(true);

        List<PlaceSearchStrategy> strategies = Arrays.asList(
                lowPriorityStrategy,  // 낮은 우선순위를 먼저 추가
                highPriorityStrategy  // 높은 우선순위를 나중에 추가
        );

        PlaceSearchStrategyFactory factory = new PlaceSearchStrategyFactory(strategies);

        // when
        PlaceSearchStrategy selectedStrategy = factory.getStrategy(PlaceSearchType.CATEGORY);

        // then
        assertThat(selectedStrategy).isEqualTo(highPriorityStrategy);
    }

    @Test
    @DisplayName("빈 전략 리스트 - 예외 발생")
    void constructor_EmptyStrategies_ThrowsException() {
        // given
        List<PlaceSearchStrategy> emptyStrategies = Collections.emptyList();

        // when
        PlaceSearchStrategyFactory factory = new PlaceSearchStrategyFactory(emptyStrategies);

        // then
        assertThatThrownBy(() -> factory.getStrategy(PlaceSearchType.AI_QUERY))
                .isInstanceOf(BusinessException.class)
                .hasMessage("지원하지 않는 검색 타입: " + PlaceSearchType.AI_QUERY);
    }

    @Test
    @DisplayName("전략 등록 순서와 무관하게 우선순위로 정렬")
    void constructor_StrategiesOrderedByPriority() {
        // given - 우선순위가 뒤섞인 전략들
        given(aiQueryStrategy.getPriority()).willReturn(10);
        given(categoryStrategy.getPriority()).willReturn(5);
        given(defaultStrategy.getPriority()).willReturn(1);

        given(defaultStrategy.supports(PlaceSearchType.AI_QUERY)).willReturn(true);
        // categoryStrategy와 aiQueryStrategy는 지원하지 않도록 설정

        List<PlaceSearchStrategy> strategies = Arrays.asList(
                aiQueryStrategy,    // priority = 10
                categoryStrategy,   // priority = 5
                defaultStrategy     // priority = 1
        );

        PlaceSearchStrategyFactory factory = new PlaceSearchStrategyFactory(strategies);

        // when - 모든 전략이 지원하는 타입 요청
        PlaceSearchStrategy selectedStrategy = factory.getStrategy(PlaceSearchType.AI_QUERY);

        // then - 가장 높은 우선순위(낮은 숫자) 전략이 선택되어야 함
        assertThat(selectedStrategy).isEqualTo(defaultStrategy);
    }
}
