package com.dolpin.global.constants;

public final class TestConstants {

    private TestConstants() {
    }

    // === 장소 관련 상수 ===
    public static final String TEST_CAFE_NAME = "테스트 카페";
    public static final String TEST_RESTAURANT_NAME = "테스트 식당";
    public static final String TEST_BAR_NAME = "테스트 술집";

    // === 카테고리 상수 ===
    public static final String CAFE_CATEGORY = "카페";
    public static final String RESTAURANT_CATEGORY = "식당";
    public static final String BAR_CATEGORY = "술집";
    public static final String NON_EXISTENT_CATEGORY = "존재하지않는카테고리";

    // === 기본 정보 상수 ===
    public static final String DEFAULT_ROAD_ADDRESS = "테스트 주소";
    public static final String DEFAULT_LOT_ADDRESS = "테스트 지번";
    public static final String DEFAULT_IMAGE_URL = "test.jpg";
    public static final String DEFAULT_DESCRIPTION = "테스트 설명";
    public static final String DEFAULT_PHONE = "02-1234-5678";

    // === 위치 상수 ===
    public static final Double CENTER_LAT = 37.5665;
    public static final Double CENTER_LNG = 126.9780;
    public static final Double NEAR_LAT = 37.5666;
    public static final Double NEAR_LNG = 126.9781;
    public static final Double FAR_LAT = 37.6665;
    public static final Double FAR_LNG = 127.0780;
    public static final Double SORT_TEST_FAR_LAT = 37.5670;
    public static final Double SORT_TEST_FAR_LNG = 126.9785;
    public static final Double BAR_LAT = 37.5667;
    public static final Double BAR_LNG = 126.9782;
    public static final Double RESTAURANT1_LAT = 37.5668;
    public static final Double RESTAURANT1_LNG = 126.9783;
    public static final Double RESTAURANT2_LAT = 37.5669;
    public static final Double RESTAURANT2_LNG = 126.9784;

    // === 반경 상수 ===
    public static final Double SMALL_RADIUS = 1000.0;
    public static final Double LARGE_RADIUS = 50000.0;
    public static final double DEFAULT_RADIUS = 1000.0;

    // === 키워드 상수 ===
    public static final String COZY_KEYWORD = "아늑한";
    public static final String DELICIOUS_KEYWORD = "맛있는";
    public static final String GOOD_KEYWORD = "좋은";

    // === 메뉴 상수 ===
    public static final String AMERICANO_MENU = "아메리카노";
    public static final String LATTE_MENU = "라떼";
    public static final Integer AMERICANO_PRICE = 4000;
    public static final Integer LATTE_PRICE = 4500;

    // === 영업시간 상수 ===
    public static final String MONDAY = "월";
    public static final String TUESDAY = "화";
    public static final String OPEN_TIME = "09:00";
    public static final String CLOSE_TIME = "21:00";
}
