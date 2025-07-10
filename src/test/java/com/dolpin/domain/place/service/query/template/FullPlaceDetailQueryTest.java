package com.dolpin.domain.place.service.query.template;

import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.entity.*;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import com.dolpin.domain.place.service.template.FullPlaceDetailQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FullPlaceDetailQuery 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FullPlaceDetailQuery 테스트")
class FullPlaceDetailQueryTest {

    @Mock private PlaceRepository placeRepository;
    @Mock private PlaceBookmarkQueryService bookmarkQueryService;

    @InjectMocks
    private FullPlaceDetailQuery fullPlaceDetailQuery;

    /* ──────────────────────── 테스트 ──────────────────────── */

    @Test
    @DisplayName("완전한 상세 정보 조회 - 모든 정보 포함")
    void getPlaceDetail_ReturnsCompleteInformation() {
        // given
        Long placeId = 1L;
        Long userId  = 1L;

        setupMockPlaceQueries(placeId);
        given(bookmarkQueryService.isBookmarked(userId, placeId)).willReturn(true);

        // when
        PlaceDetailResponse result = fullPlaceDetailQuery.getPlaceDetail(placeId, userId);

        // then
        assertThat(result.getId()).isEqualTo(placeId);
        assertThat(result.getName()).isEqualTo("테스트 카페");
        assertThat(result.getIsBookmarked()).isTrue();
        assertThat(result.getKeywords()).isNotEmpty();
        assertThat(result.getMenu()).isNotEmpty();
        assertThat(result.getOpeningHours()).isNotNull();

        verify(placeRepository).findBasicPlaceById(placeId);
        verify(placeRepository).findByIdWithKeywords(placeId);
        verify(placeRepository).findByIdWithMenus(placeId);
        verify(placeRepository).findByIdWithHours(placeId);
        verify(bookmarkQueryService).isBookmarked(userId, placeId);
    }

    /* ──────────────────────── Mock 세팅 ──────────────────────── */

    private void setupMockPlaceQueries(Long placeId) {
        given(placeRepository.findBasicPlaceById(placeId))
                .willReturn(Optional.of(createMockPlace(placeId, "테스트 카페")));

        given(placeRepository.findByIdWithKeywords(placeId))
                .willReturn(Optional.of(createMockPlaceWithKeywords(placeId)));

        given(placeRepository.findByIdWithMenus(placeId))
                .willReturn(Optional.of(createMockPlaceWithMenus(placeId)));

        given(placeRepository.findByIdWithHours(placeId))
                .willReturn(Optional.of(createMockPlaceWithHours(placeId)));
    }

    /* ──────────────────────── Place 생성 헬퍼 ──────────────────────── */

    private Place createMockPlace(Long id, String name) {
        Point point = createPoint();

        return Place.builder()
                .id(id)
                .name(name)
                .category("카페")
                .roadAddress("서울시 강남구")
                .imageUrl("image.jpg")
                .description("멋진 카페")
                .phone("02-1234-5678")
                .location(point)
                .keywords(new ArrayList<>())
                .menus(new ArrayList<>())
                .hours(new ArrayList<>())
                .build();
    }

    private Place createMockPlaceWithKeywords(Long id) {
        // Keyword와 PlaceKeyword를 먼저 만들기
        Keyword k1 = createKeyword("맛있는");
        Keyword k2 = createKeyword("분위기 좋은");

        PlaceKeyword pk1 = createPlaceKeyword(k1);
        PlaceKeyword pk2 = createPlaceKeyword(k2);

        Point point = createPoint();

        return Place.builder()
                .id(id)
                .name("테스트 카페")
                .category("카페")
                .roadAddress("서울시 강남구")
                .imageUrl("image.jpg")
                .description("멋진 카페")
                .phone("02-1234-5678")
                .location(point)
                .keywords(List.of(pk1, pk2))
                .menus(new ArrayList<>())
                .hours(new ArrayList<>())
                .build();
    }

    private Place createMockPlaceWithMenus(Long id) {
        PlaceMenu menu1 = createPlaceMenu("아메리카노", 4000);
        PlaceMenu menu2 = createPlaceMenu("라떼", 4500);

        Point point = createPoint();

        return Place.builder()
                .id(id)
                .name("테스트 카페")
                .category("카페")
                .roadAddress("서울시 강남구")
                .imageUrl("image.jpg")
                .description("멋진 카페")
                .phone("02-1234-5678")
                .location(point)
                .keywords(new ArrayList<>())
                .menus(List.of(menu1, menu2))
                .hours(new ArrayList<>())
                .build();
    }

    private Place createMockPlaceWithHours(Long id) {
        PlaceHours hours1 = createPlaceHours("월", "09:00", "22:00", false);
        PlaceHours hours2 = createPlaceHours("화", "09:00", "22:00", false);

        Point point = createPoint();

        return Place.builder()
                .id(id)
                .name("테스트 카페")
                .category("카페")
                .roadAddress("서울시 강남구")
                .imageUrl("image.jpg")
                .description("멋진 카페")
                .phone("02-1234-5678")
                .location(point)
                .keywords(new ArrayList<>())
                .menus(new ArrayList<>())
                .hours(List.of(hours1, hours2))
                .build();
    }

    /* ──────────────────────── 엔티티 생성 헬퍼 ──────────────────────── */

    private Keyword createKeyword(String keywordText) {
        return Keyword.builder()
                .id(1L)
                .keyword(keywordText)
                .build();
    }

    private PlaceKeyword createPlaceKeyword(Keyword keyword) {
        return PlaceKeyword.builder()
                .id(1L)
                .keyword(keyword)
                .build();
    }

    private PlaceMenu createPlaceMenu(String menuName, Integer price) {
        return PlaceMenu.builder()
                .id(1L)
                .menuName(menuName)
                .price(price)
                .build();
    }

    private PlaceHours createPlaceHours(String dayOfWeek, String openTime, String closeTime, Boolean isBreakTime) {
        return PlaceHours.builder()
                .id(1L)
                .dayOfWeek(dayOfWeek)
                .openTime(openTime)
                .closeTime(closeTime)
                .isBreakTime(isBreakTime)
                .build();
    }

    private Point createPoint() {
        GeometryFactory gf = new GeometryFactory();
        return gf.createPoint(new Coordinate(126.9780, 37.5665));
    }
}
