package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.entity.*;
import com.dolpin.domain.place.repository.PlaceRepository;
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
            // given
            Long placeId = 1L;
            Place basicPlace = createMockPlaceForDetail(placeId, "테스트 카페", 37.5665, 126.9780);

            // 키워드가 포함된 장소
            Place placeWithKeywords = createMockPlaceWithKeywords(placeId, "테스트 카페", List.of("아늑한", "맛있는"));

            // 메뉴가 포함된 장소
            Place placeWithMenus = mock(Place.class);
            PlaceMenu menu1 = createMockMenu("아메리카노", 4000);
            PlaceMenu menu2 = createMockMenu("라떼", 4500);
            given(placeWithMenus.getMenus()).willReturn(List.of(menu1, menu2));

            // 영업시간이 포함된 장소
            Place placeWithHours = mock(Place.class);
            List<PlaceHours> hours = createCompleteBusinessHours();
            given(placeWithHours.getHours()).willReturn(hours);

            given(placeRepository.findBasicPlaceById(placeId)).willReturn(Optional.of(basicPlace));
            given(placeRepository.findByIdWithKeywords(placeId)).willReturn(Optional.of(placeWithKeywords));
            given(placeRepository.findByIdWithMenus(placeId)).willReturn(Optional.of(placeWithMenus));
            given(placeRepository.findByIdWithHours(placeId)).willReturn(Optional.of(placeWithHours));

            // when
            PlaceDetailResponse result = placeQueryService.getPlaceDetail(placeId);

            // then
            assertThat(result.getId()).isEqualTo(placeId);
            assertThat(result.getName()).isEqualTo("테스트 카페");
            assertThat(result.getAddress()).isEqualTo("테스트 주소");
            assertThat(result.getPhone()).isEqualTo("02-1234-5678");
            assertThat(result.getDescription()).isEqualTo("테스트 설명");
            assertThat(result.getThumbnail()).isEqualTo("test.jpg");

            // 위치 정보 확인
            assertThat(result.getLocation()).containsEntry("type", "Point");
            double[] coordinates = (double[]) result.getLocation().get("coordinates");
            assertThat(coordinates).containsExactly(126.9780, 37.5665);

            // 키워드 확인
            assertThat(result.getKeywords()).containsExactlyInAnyOrder("아늑한", "맛있는");

            // 메뉴 확인
            assertThat(result.getMenu()).hasSize(2);
            assertThat(result.getMenu()).extracting(PlaceDetailResponse.Menu::getName)
                    .containsExactlyInAnyOrder("아메리카노", "라떼");

            // 영업시간 확인
            assertThat(result.getOpeningHours()).isNotNull();
            assertThat(result.getOpeningHours().getSchedules()).hasSize(7);

            // 각 Repository 메서드가 호출되었는지 확인
            verify(placeRepository).findBasicPlaceById(placeId);
            verify(placeRepository).findByIdWithKeywords(placeId);
            verify(placeRepository).findByIdWithMenus(placeId);
            verify(placeRepository).findByIdWithHours(placeId);
        }

        @Test
        @DisplayName("존재하지 않는 장소 ID 조회 시 예외가 발생한다")
        void getPlaceDetail_WithNonExistentId_ThrowsPlaceNotFoundException() {
            // given
            Long nonExistentId = 999L;
            given(placeRepository.findBasicPlaceById(nonExistentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> placeQueryService.getPlaceDetail(nonExistentId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.PLACE_NOT_FOUND);
        }
    }
}
