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

            Place basicPlace = createBasicPlaceForSuccess();
            Place keywordPlace = createKeywordPlace(getDefaultCafeKeywords());
            Place menuPlace = createMenuPlace(createDefaultCafeMenus());
            Place hoursPlace = createHoursPlace(createCompleteBusinessHoursStubs());

            given(placeRepository.findBasicPlaceById(placeId)).willReturn(Optional.of(basicPlace));
            given(placeRepository.findByIdWithKeywords(placeId)).willReturn(Optional.of(keywordPlace));
            given(placeRepository.findByIdWithMenus(placeId)).willReturn(Optional.of(menuPlace));
            given(placeRepository.findByIdWithHours(placeId)).willReturn(Optional.of(hoursPlace));

            PlaceDetailResponse result = placeQueryService.getPlaceDetail(placeId);

            verifyBasicPlaceInfo(result, placeId);
            verifyLocationInfo(result);
            verifyKeywordsInfo(result);
            verifyMenuInfo(result);
            verifyOpeningHoursInfo(result);
            verifyRepositoryInteractions(placeId);
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

            verify(placeRepository).findBasicPlaceById(nonExistentId);
            verifyNoMoreInteractions(placeRepository);
        }

        @Test
        @DisplayName("키워드 조회 실패 시 예외가 발생한다")
        void getPlaceDetail_WhenKeywordQueryFails_ThrowsException() {
            Long placeId = TestConstants.PLACE_ID_1;
            Place basicPlace = createBasicPlaceForFailure(); // 상세 메서드 호출 안됨

            given(placeRepository.findBasicPlaceById(placeId)).willReturn(Optional.of(basicPlace));
            given(placeRepository.findByIdWithKeywords(placeId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> placeQueryService.getPlaceDetail(placeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.PLACE_NOT_FOUND);

            verify(placeRepository).findBasicPlaceById(placeId);
            verify(placeRepository).findByIdWithKeywords(placeId);
            verifyNoMoreInteractions(placeRepository);
        }

        @Test
        @DisplayName("메뉴 조회 실패 시 예외가 발생한다")
        void getPlaceDetail_WhenMenuQueryFails_ThrowsException() {
            Long placeId = TestConstants.PLACE_ID_1;
            Place basicPlace = createBasicPlaceForFailure(); // 상세 메서드 호출 안됨
            Place keywordPlace = createKeywordPlace(getDefaultCafeKeywords());

            given(placeRepository.findBasicPlaceById(placeId)).willReturn(Optional.of(basicPlace));
            given(placeRepository.findByIdWithKeywords(placeId)).willReturn(Optional.of(keywordPlace));
            given(placeRepository.findByIdWithMenus(placeId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> placeQueryService.getPlaceDetail(placeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.PLACE_NOT_FOUND);

            verify(placeRepository).findBasicPlaceById(placeId);
            verify(placeRepository).findByIdWithKeywords(placeId);
            verify(placeRepository).findByIdWithMenus(placeId);
            verifyNoMoreInteractions(placeRepository);
        }

        @Test
        @DisplayName("영업시간 조회 실패 시 예외가 발생한다")
        void getPlaceDetail_WhenHoursQueryFails_ThrowsException() {
            Long placeId = TestConstants.PLACE_ID_1;
            Place basicPlace = createBasicPlaceForFailure(); // 상세 메서드 호출 안됨
            Place keywordPlace = createKeywordPlace(getDefaultCafeKeywords());
            Place menuPlace = createMenuPlace(createDefaultCafeMenus());

            given(placeRepository.findBasicPlaceById(placeId)).willReturn(Optional.of(basicPlace));
            given(placeRepository.findByIdWithKeywords(placeId)).willReturn(Optional.of(keywordPlace));
            given(placeRepository.findByIdWithMenus(placeId)).willReturn(Optional.of(menuPlace));
            given(placeRepository.findByIdWithHours(placeId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> placeQueryService.getPlaceDetail(placeId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.PLACE_NOT_FOUND);

            verify(placeRepository).findBasicPlaceById(placeId);
            verify(placeRepository).findByIdWithKeywords(placeId);
            verify(placeRepository).findByIdWithMenus(placeId);
            verify(placeRepository).findByIdWithHours(placeId);
            verifyNoMoreInteractions(placeRepository);
        }
    }

    private void verifyBasicPlaceInfo(PlaceDetailResponse result, Long expectedId) {
        assertThat(result.getId()).isEqualTo(expectedId);
        assertThat(result.getName()).isEqualTo(TestConstants.TEST_CAFE_NAME);
        assertThat(result.getAddress()).isEqualTo(TestConstants.DEFAULT_ROAD_ADDRESS);
        assertThat(result.getPhone()).isEqualTo(TestConstants.DEFAULT_PHONE);
        assertThat(result.getDescription()).isEqualTo(TestConstants.DEFAULT_DESCRIPTION);
        assertThat(result.getThumbnail()).isEqualTo(TestConstants.DEFAULT_IMAGE_URL);
    }

    private void verifyLocationInfo(PlaceDetailResponse result) {
        assertThat(result.getLocation()).containsEntry("type", "Point");
        double[] coordinates = (double[]) result.getLocation().get("coordinates");
        assertThat(coordinates).containsExactly(TestConstants.CENTER_LNG, TestConstants.CENTER_LAT);
    }

    private void verifyKeywordsInfo(PlaceDetailResponse result) {
        assertThat(result.getKeywords()).containsExactlyInAnyOrder(
                TestConstants.COZY_KEYWORD, TestConstants.DELICIOUS_KEYWORD);
    }

    private void verifyMenuInfo(PlaceDetailResponse result) {
        assertThat(result.getMenu()).hasSize(TestConstants.EXPECTED_MENU_COUNT);
        assertThat(result.getMenu()).extracting(PlaceDetailResponse.Menu::getName)
                .containsExactlyInAnyOrder(TestConstants.AMERICANO_MENU, TestConstants.LATTE_MENU);
        assertThat(result.getMenu()).extracting(PlaceDetailResponse.Menu::getPrice)
                .containsExactlyInAnyOrder(TestConstants.AMERICANO_PRICE, TestConstants.LATTE_PRICE);
    }

    private void verifyOpeningHoursInfo(PlaceDetailResponse result) {
        assertThat(result.getOpeningHours()).isNotNull();
        assertThat(result.getOpeningHours().getSchedules()).hasSize(TestConstants.EXPECTED_WEEKDAYS_COUNT);
    }

    private void verifyRepositoryInteractions(Long placeId) {
        verify(placeRepository).findBasicPlaceById(placeId);
        verify(placeRepository).findByIdWithKeywords(placeId);
        verify(placeRepository).findByIdWithMenus(placeId);
        verify(placeRepository).findByIdWithHours(placeId);
        verifyNoMoreInteractions(placeRepository);
    }
}
