package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.client.PlaceAiClient;
import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.global.constants.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceQueryServiceImpl 통합 테스트")
class PlaceQueryServiceImplTest {

    @Mock private PlaceRepository placeRepository;
    @Mock private PlaceAiClient placeAiClient;
    @InjectMocks private PlaceQueryServiceImpl placeQueryService;

    @Nested
    @DisplayName("getAllCategories 메서드 테스트")
    class GetAllCategoriesTest {

        @Test
        @DisplayName("카테고리 목록 정상 조회가 동작한다")
        void getAllCategories_ReturnsAllAvailableCategories() {
            // given
            List<String> categories = Arrays.asList(
                    TestConstants.CAFE_CATEGORY,
                    TestConstants.RESTAURANT_CATEGORY,
                    TestConstants.BAR_CATEGORY,
                    TestConstants.BAKERY_CATEGORY,
                    TestConstants.FASTFOOD_CATEGORY
            );
            given(placeRepository.findDistinctCategories()).willReturn(categories);

            // when
            PlaceCategoryResponse result = placeQueryService.getAllCategories();

            // then
            assertThat(result.getCategories()).hasSize(5);
            assertThat(result.getCategories()).containsExactlyInAnyOrder(
                    TestConstants.CAFE_CATEGORY,
                    TestConstants.RESTAURANT_CATEGORY,
                    TestConstants.BAR_CATEGORY,
                    TestConstants.BAKERY_CATEGORY,
                    TestConstants.FASTFOOD_CATEGORY
            );
        }

        @Test
        @DisplayName("카테고리가 없을 때 빈 목록을 반환한다")
        void getAllCategories_WithNoCategories_ReturnsEmptyList() {
            // given
            given(placeRepository.findDistinctCategories()).willReturn(Collections.emptyList());

            // when
            PlaceCategoryResponse result = placeQueryService.getAllCategories();

            // then
            assertThat(result.getCategories()).isEmpty();
        }
    }
}
