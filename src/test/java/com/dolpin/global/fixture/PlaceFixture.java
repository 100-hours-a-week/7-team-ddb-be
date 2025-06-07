package com.dolpin.global.fixture;

import com.dolpin.domain.place.entity.*;
import com.dolpin.global.constants.TestConstants;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class PlaceFixture {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private PlaceFixture() {
        // Utility class
    }

    // === 기본 장소 생성 메서드 ===
    public static Place createPlace(String name, String category, double lat, double lng) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));

        return Place.builder()
                .name(name)
                .category(category)
                .location(location)
                .roadAddress(TestConstants.DEFAULT_ROAD_ADDRESS)
                .lotAddress(TestConstants.DEFAULT_LOT_ADDRESS)
                .imageUrl(TestConstants.DEFAULT_IMAGE_URL)
                .description(TestConstants.DEFAULT_DESCRIPTION)
                .phone(TestConstants.DEFAULT_PHONE)
                .build();
    }

    // === 카테고리별 장소 생성 메서드 ===
    public static Place createCafe(String name, double lat, double lng) {
        return createPlace(name, TestConstants.CAFE_CATEGORY, lat, lng);
    }

    public static Place createRestaurant(String name, double lat, double lng) {
        return createPlace(name, TestConstants.RESTAURANT_CATEGORY, lat, lng);
    }

    public static Place createBar(String name, double lat, double lng) {
        return createPlace(name, TestConstants.BAR_CATEGORY, lat, lng);
    }

    // === 테스트용 장소 생성 메서드 (상수 활용) ===
    public static Place createBasicCafe() {
        return createCafe(TestConstants.TEST_CAFE_NAME, TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
    }

    public static Place createNearbyCafe() {
        return createCafe(TestConstants.NEARBY_PREFIX + TestConstants.TEST_CAFE_NAME,
                TestConstants.NEAR_LAT, TestConstants.NEAR_LNG);
    }

    public static Place createFarCafe() {
        return createCafe(TestConstants.FAR_PREFIX + TestConstants.TEST_CAFE_NAME,
                TestConstants.FAR_LAT, TestConstants.FAR_LNG);
    }

    public static Place createBasicRestaurant() {
        return createRestaurant(TestConstants.TEST_RESTAURANT_NAME,
                TestConstants.RESTAURANT1_LAT, TestConstants.RESTAURANT1_LNG);
    }

    public static Place createBasicBar() {
        return createBar(TestConstants.TEST_BAR_NAME, TestConstants.BAR_LAT, TestConstants.BAR_LNG);
    }

    // === 연관 엔티티 생성 메서드 ===
    public static Keyword createKeyword(String keyword) {
        return Keyword.builder()
                .keyword(keyword)
                .build();
    }

    public static PlaceKeyword createPlaceKeyword(Place place, Keyword keyword) {
        return PlaceKeyword.builder()
                .place(place)
                .keyword(keyword)
                .build();
    }

    public static PlaceMenu createPlaceMenu(Place place, String menuName, Integer price) {
        return PlaceMenu.builder()
                .place(place)
                .menuName(menuName)
                .price(price)
                .build();
    }

    public static PlaceHours createPlaceHours(Place place, String dayOfWeek, String openTime, String closeTime) {
        return createPlaceHours(place, dayOfWeek, openTime, closeTime, false);
    }

    public static PlaceHours createPlaceHours(Place place, String dayOfWeek, String openTime, String closeTime, boolean isBreakTime) {
        return PlaceHours.builder()
                .place(place)
                .dayOfWeek(dayOfWeek)
                .openTime(openTime)
                .closeTime(closeTime)
                .isBreakTime(isBreakTime)
                .build();
    }

    // === 기본 메뉴 생성 메서드 ===
    public static PlaceMenu createAmericanoMenu(Place place) {
        return createPlaceMenu(place, TestConstants.AMERICANO_MENU, TestConstants.AMERICANO_PRICE);
    }

    public static PlaceMenu createLatteMenu(Place place) {
        return createPlaceMenu(place, TestConstants.LATTE_MENU, TestConstants.LATTE_PRICE);
    }

    // === 기본 영업시간 생성 메서드 ===
    public static PlaceHours createMondayHours(Place place) {
        return createPlaceHours(place, TestConstants.MONDAY, TestConstants.OPEN_TIME, TestConstants.CLOSE_TIME);
    }

    public static PlaceHours createTuesdayHours(Place place) {
        return createPlaceHours(place, TestConstants.TUESDAY, TestConstants.OPEN_TIME, TestConstants.CLOSE_TIME);
    }
}
