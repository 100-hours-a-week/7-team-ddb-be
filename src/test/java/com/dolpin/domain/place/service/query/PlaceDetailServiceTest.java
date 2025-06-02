package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.entity.*;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.global.constants.TestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static com.dolpin.global.helper.PlaceTestHelper.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Place 상세 조회 서비스 테스트")
class PlaceDetailServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceQueryServiceImpl placeQueryService;

    @Nested
    @DisplayName("getPlaceDetail 메서드 테스트")
    class GetPlaceDetailTest {

        @Test
        @DisplayName("정상적인 장소 상세 조회가 동작한다")
        void getPlaceDetail_WithValidId_ReturnsCompleteDetail() {

            Long placeId = TestConstants.PLACE_ID_1;
            Place basicPlace = createMockPlaceForDetail(placeId, TestConstants.TEST_CAFE_NAME,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);

            Place placeWithKeywords = createMockPlaceWithKeywordsOnly(
                    List.of(TestConstants.COZY_KEYWORD, TestConstants.DELICIOUS_KEYWORD));

            Place placeWithMenus = createMockPlaceWithMenusOnly(
                    List.of(
                            createMockMenu(TestConstants.AMERICANO_MENU, TestConstants.AMERICANO_PRICE),
                            createMockMenu(TestConstants.LATTE_MENU, TestConstants.LATTE_PRICE)
                    )
            );

            Place placeWithHours = createMockPlaceWithHoursOnly(createCompleteBusinessHours());

            given(placeRepository.findBasicPlaceById(placeId)).willReturn(Optional.of(basicPlace));
            given(placeRepository.findByIdWithKeywords(placeId)).willReturn(Optional.of(placeWithKeywords));
            given(placeRepository.findByIdWithMenus(placeId)).willReturn(Optional.of(placeWithMenus));
            given(placeRepository.findByIdWithHours(placeId)).willReturn(Optional.of(placeWithHours));

            PlaceDetailResponse result = placeQueryService.getPlaceDetail(placeId);

            assertThat(result.getId()).isEqualTo(placeId);
            assertThat(result.getName()).isEqualTo(TestConstants.TEST_CAFE_NAME);
            assertThat(result.getAddress()).isEqualTo(TestConstants.DEFAULT_ROAD_ADDRESS);
            assertThat(result.getPhone()).isEqualTo(TestConstants.DEFAULT_PHONE);
            assertThat(result.getDescription()).isEqualTo(TestConstants.DEFAULT_DESCRIPTION);
            assertThat(result.getThumbnail()).isEqualTo(TestConstants.DEFAULT_IMAGE_URL);

            assertThat(result.getLocation()).containsEntry("type", "Point");
            double[] coordinates = (double[]) result.getLocation().get("coordinates");
            assertThat(coordinates).containsExactly(TestConstants.CENTER_LNG, TestConstants.CENTER_LAT);

            assertThat(result.getKeywords()).containsExactlyInAnyOrder(
                    TestConstants.COZY_KEYWORD, TestConstants.DELICIOUS_KEYWORD);

            assertThat(result.getMenu()).hasSize(2);
            assertThat(result.getMenu()).extracting(PlaceDetailResponse.Menu::getName)
                    .containsExactlyInAnyOrder(TestConstants.AMERICANO_MENU, TestConstants.LATTE_MENU);
            assertThat(result.getMenu()).extracting(PlaceDetailResponse.Menu::getPrice)
                    .containsExactlyInAnyOrder(TestConstants.AMERICANO_PRICE, TestConstants.LATTE_PRICE);

            assertThat(result.getOpeningHours()).isNotNull();
            assertThat(result.getOpeningHours().getSchedules()).hasSize(7);

            verify(placeRepository).findBasicPlaceById(placeId);
            verify(placeRepository).findByIdWithKeywords(placeId);
            verify(placeRepository).findByIdWithMenus(placeId);
            verify(placeRepository).findByIdWithHours(placeId);
        }

        @Test
        @DisplayName("존재하지 않는 장소 ID 조회 시 예외가 발생한다")
        void getPlaceDetail_WithNonExistentId_ThrowsPlaceNotFoundException() {

            Long nonExistentId = TestConstants.NON_EXISTENT_PLACE_ID;

            given(placeRepository.findBasicPlaceById(nonExistentId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> placeQueryService.getPlaceDetail(nonExistentId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.PLACE_NOT_FOUND);
        }
    }
}
