package com.dolpin.global.fixture;

import com.dolpin.domain.place.entity.*;
import com.dolpin.global.constants.PlaceTestConstants;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class PlaceFixture {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private PlaceFixture() {
        // Utility class
    }

    // === 완전한 파라미터 지원 메서드 ===
    public static Place createPlace(String name, String category, double lat, double lng,
                                    String roadAddress, String lotAddress, String imageUrl,
                                    String description, String phone) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));

        return Place.builder()
                .name(name)
                .category(category)
                .location(location)
                .roadAddress(roadAddress)
                .lotAddress(lotAddress)
                .imageUrl(imageUrl)
                .description(description)
                .phone(phone)
                .build();
    }

    // === 기본값을 사용하는 편의 메서드 ===
    public static Place createPlace(String name, String category, double lat, double lng) {
        return createPlace(name, category, lat, lng,
                PlaceTestConstants.DEFAULT_ROAD_ADDRESS,
                PlaceTestConstants.DEFAULT_LOT_ADDRESS,
                PlaceTestConstants.DEFAULT_IMAGE_URL,
                PlaceTestConstants.DEFAULT_DESCRIPTION,
                PlaceTestConstants.DEFAULT_PHONE);
    }

    // === 카테고리별 장소 생성 메서드 (기본값) ===
    public static Place createCafe(String name, double lat, double lng) {
        return createPlace(name, PlaceTestConstants.CAFE_CATEGORY, lat, lng);
    }

    public static Place createRestaurant(String name, double lat, double lng) {
        return createPlace(name, PlaceTestConstants.RESTAURANT_CATEGORY, lat, lng);
    }

    public static Place createBar(String name, double lat, double lng) {
        return createPlace(name, PlaceTestConstants.BAR_CATEGORY, lat, lng);
    }

    // === 카테고리별 장소 생성 메서드 (커스터마이징 가능) ===
    public static Place createCafe(String name, double lat, double lng, String roadAddress,
                                   String imageUrl, String description, String phone) {
        return createPlace(name, PlaceTestConstants.CAFE_CATEGORY, lat, lng,
                roadAddress, PlaceTestConstants.DEFAULT_LOT_ADDRESS, imageUrl, description, phone);
    }

    public static Place createRestaurant(String name, double lat, double lng, String roadAddress,
                                         String imageUrl, String description, String phone) {
        return createPlace(name, PlaceTestConstants.RESTAURANT_CATEGORY, lat, lng,
                roadAddress, PlaceTestConstants.DEFAULT_LOT_ADDRESS, imageUrl, description, phone);
    }

    public static Place createBar(String name, double lat, double lng, String roadAddress,
                                  String imageUrl, String description, String phone) {
        return createPlace(name, PlaceTestConstants.BAR_CATEGORY, lat, lng,
                roadAddress, PlaceTestConstants.DEFAULT_LOT_ADDRESS, imageUrl, description, phone);
    }

    // === 테스트용 장소 생성 메서드 (상수 활용) ===
    public static Place createBasicCafe() {
        return createCafe(PlaceTestConstants.TEST_CAFE_NAME, PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG);
    }

    public static Place createNearbyCafe() {
        return createCafe(PlaceTestConstants.NEARBY_PREFIX + PlaceTestConstants.TEST_CAFE_NAME,
                PlaceTestConstants.NEAR_LAT, PlaceTestConstants.NEAR_LNG);
    }

    public static Place createFarCafe() {
        return createCafe(PlaceTestConstants.FAR_PREFIX + PlaceTestConstants.TEST_CAFE_NAME,
                PlaceTestConstants.FAR_LAT, PlaceTestConstants.FAR_LNG);
    }

    public static Place createBasicRestaurant() {
        return createRestaurant(PlaceTestConstants.TEST_RESTAURANT_NAME,
                PlaceTestConstants.RESTAURANT1_LAT, PlaceTestConstants.RESTAURANT1_LNG);
    }

    public static Place createBasicBar() {
        return createBar(PlaceTestConstants.TEST_BAR_NAME, PlaceTestConstants.BAR_LAT, PlaceTestConstants.BAR_LNG);
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
        return createPlaceMenu(place, PlaceTestConstants.AMERICANO_MENU, PlaceTestConstants.AMERICANO_PRICE);
    }

    public static PlaceMenu createLatteMenu(Place place) {
        return createPlaceMenu(place, PlaceTestConstants.LATTE_MENU, PlaceTestConstants.LATTE_PRICE);
    }

    // === 커스터마이징 가능한 메뉴 생성 ===
    public static PlaceMenu createCustomMenu(Place place, String menuName, Integer price) {
        return createPlaceMenu(place, menuName, price);
    }

    // === 기본 영업시간 생성 메서드 ===
    public static PlaceHours createMondayHours(Place place) {
        return createPlaceHours(place, PlaceTestConstants.MONDAY, PlaceTestConstants.OPEN_TIME, PlaceTestConstants.CLOSE_TIME);
    }

    public static PlaceHours createTuesdayHours(Place place) {
        return createPlaceHours(place, PlaceTestConstants.TUESDAY, PlaceTestConstants.OPEN_TIME, PlaceTestConstants.CLOSE_TIME);
    }

    // === 커스터마이징 가능한 영업시간 ===
    public static PlaceHours createCustomHours(Place place, String dayOfWeek, String openTime, String closeTime) {
        return createPlaceHours(place, dayOfWeek, openTime, closeTime, false);
    }

    public static PlaceHours createCustomBreakTime(Place place, String dayOfWeek, String startTime, String endTime) {
        return createPlaceHours(place, dayOfWeek, startTime, endTime, true);
    }
}
