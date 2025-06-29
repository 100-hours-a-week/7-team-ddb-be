package com.dolpin.global.constants;

public final class TestConstants {

    private TestConstants() {
    }

    // === 사용자 관련 상수 ===
    public static final Long USER_ID_1 = 1L;

    // === API 응답 메시지 ===
    public static final class Api {
        public static final String SUCCESS_MESSAGE_SEARCH = "get_place_success";
        public static final String SUCCESS_MESSAGE_DETAIL = "get_place_detail_success";
        public static final String SUCCESS_MESSAGE_CATEGORIES = "get_categories_success";
    }

    // === 장소명 관련 상수 ===
    public static final String TEST_CAFE_NAME = "테스트 카페";
    public static final String TEST_RESTAURANT_NAME = "테스트 식당";
    public static final String TEST_BAR_NAME = "테스트 술집";

    // === 장소명 접두사 ===
    public static final String NEARBY_PREFIX = "가까운 ";
    public static final String FAR_PREFIX = "먼 ";
    public static final String BEST_PREFIX = "최고의 ";
    public static final String GOOD_PREFIX = "좋은 ";
    public static final String ORDINARY_PREFIX = "평범한 ";

    // === 검색 테스트용 장소명 ===
    public static final String ORDINARY_CAFE_NAME = "평범한 카페";
    public static final String BEST_CAFE_NAME = "최고의 카페";
    public static final String GOOD_CAFE_NAME = "좋은 카페";
    public static final String ITALIAN_RESTAURANT_NAME = "이탈리안 레스토랑";
    public static final String ROMANTIC_PASTA_NAME = "로맨틱 파스타";
    public static final String STARBUCKS_NAME = "스타벅스";
    public static final String TWOSOME_NAME = "투썸플레이스";

    // === 카테고리 상수 ===
    public static final String CAFE_CATEGORY = "카페";
    public static final String RESTAURANT_CATEGORY = "식당";
    public static final String BAR_CATEGORY = "술집";
    public static final String BAKERY_CATEGORY = "베이커리";
    public static final String FASTFOOD_CATEGORY = "패스트푸드";
    public static final String NON_EXISTENT_CATEGORY = "존재하지않는카테고리";

    // === 기본 정보 상수 ===
    public static final String DEFAULT_ROAD_ADDRESS = "서울특별시 강남구 테헤란로 123";
    public static final String DEFAULT_LOT_ADDRESS = "서울특별시 강남구 역삼동 123-45";
    public static final String DEFAULT_IMAGE_URL = "https://test-images.com/test.jpg";
    public static final String DEFAULT_DESCRIPTION = "테스트용 장소 설명입니다.";
    public static final String DEFAULT_PHONE = "02-1234-5678";

    // === 추가 테스트 정보 ===
    public static final String TEST_ADDRESS = "서울시 강남구 테스트로 123";
    public static final String TEST_DESCRIPTION = "테스트 카페입니다";
    public static final String STARBUCKS_THUMBNAIL = "starbucks.jpg";
    public static final String TEST_THUMBNAIL = "test.jpg";

    // === 위치 상수 (서울 시청 기준) ===
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
    public static final Double SORT_TEST_PLACE2_LAT = 37.5666;
    public static final Double SORT_TEST_PLACE2_LNG = 126.9781;
    public static final Double SORT_TEST_PLACE3_LAT = 37.5667;
    public static final Double SORT_TEST_PLACE3_LNG = 126.9782;

    // === 반경 상수 ===
    public static final Double SMALL_RADIUS = 1000.0;
    public static final Double LARGE_RADIUS = 50000.0;
    public static final double DEFAULT_RADIUS = 1000.0;

    // === 키워드 상수 ===
    public static final String COZY_KEYWORD = "아늑한";
    public static final String DELICIOUS_KEYWORD = "맛있는";
    public static final String GOOD_KEYWORD = "좋은";
    public static final String CHAIN_STORE_KEYWORD = "체인점";
    public static final String SPACIOUS_KEYWORD = "넓은";
    public static final String DESSERT_KEYWORD = "디저트";
    public static final String CAKE_KEYWORD = "케이크";
    public static final String ORDINARY_KEYWORD = "평범한";
    public static final String BEST_KEYWORD = "최고";
    public static final String ITALIAN_KEYWORD = "이탈리안";
    public static final String ROMANTIC_KEYWORD = "분위기좋은";
    public static final String DATE_KEYWORD = "데이트";

    // === 메뉴 상수 ===
    public static final String AMERICANO_MENU = "아메리카노";
    public static final String LATTE_MENU = "라떼";
    public static final Integer AMERICANO_PRICE = 4000;
    public static final Integer LATTE_PRICE = 4500;

    // === 영업시간 상수 ===
    public static final String MONDAY = "월";
    public static final String TUESDAY = "화";
    public static final String WEDNESDAY = "수";
    public static final String THURSDAY = "목";
    public static final String FRIDAY = "금";
    public static final String SATURDAY = "토";
    public static final String SUNDAY = "일";
    public static final String OPEN_TIME = "09:00";
    public static final String CLOSE_TIME = "21:00";

    // === 추가 시간 상수 ===
    public static final String BREAK_START_TIME = "15:00";
    public static final String BREAK_END_TIME = "16:00";
    public static final String EARLY_CLOSE_TIME = "20:00";
    public static final String TWENTY_FOUR_HOUR_START = "00:00";
    public static final String TWENTY_FOUR_HOUR_END = "00:00";
    public static final String LATE_NIGHT_CLOSE = "02:00";
    public static final String LUNCH_BREAK_START = "12:00";
    public static final String LUNCH_BREAK_END = "13:00";
    public static final String FULL_DAY_START = "00:00";
    public static final String FULL_DAY_END = "23:59";
    public static final String TEST_CLOSE_TIME_18 = "18:00";

    // === 거리 관련 상수 ===
    public static final Double DISTANCE_ZERO_VALUE = 0.0;
    public static final String DISTANCE_ZERO_TEXT = "0";
    public static final String DISTANCE_UNIT_METER = "m";
    public static final String DISTANCE_UNIT_KILOMETER = "km";

    // === 테스트용 거리 값 ===
    public static final Double DISTANCE_100M = 100.0;
    public static final Double DISTANCE_150M = 150.0;
    public static final Double DISTANCE_200M = 200.0;
    public static final Double DISTANCE_300M = 300.0;
    public static final Double DISTANCE_500M = 500.0;

    // === 유사도 점수 상수 ===
    public static final Double SIMILARITY_SCORE_HIGH = 0.95;
    public static final Double SIMILARITY_SCORE_MEDIUM = 0.88;
    public static final Double SIMILARITY_SCORE_LOW = 0.75;

    // === 영업 상태 상수 ===
    public static final String BUSINESS_STATUS_OPEN = "영업 중";
    public static final String BUSINESS_STATUS_CLOSED = "영업 종료";
    public static final String BUSINESS_STATUS_BREAK = "브레이크 타임";
    public static final String BUSINESS_STATUS_UNKNOWN = "영업 여부 확인 필요";
    public static final String BUSINESS_STATUS_HOLIDAY = "휴무일";
    public static final String BUSINESS_STATUS_NO_INFO = "영업 정보 없음";

    // === 테스트 시간 상수 ===
    public static final String INVALID_TIME_FORMAT = "invalid";
    public static final String INVALID_TIME_HOUR = "25:00";
    public static final String EMPTY_STRING = "";
    public static final String WHITESPACE_STRING = "   ";

    // === 테스트 ID 상수 ===
    public static final Long PLACE_ID_1 = 1L;
    public static final Long PLACE_ID_2 = 2L;
    public static final Long PLACE_ID_3 = 3L;
    public static final Long NON_EXISTENT_PLACE_ID = 999L;

    // === 검색 쿼리 상수 ===
    public static final String PASTA_SEARCH_QUERY = "맛있는 파스타";
    public static final String CAFE_SEARCH_QUERY = "맛있는 카페";
    public static final String NON_EXISTENT_SEARCH_QUERY = "존재하지 않는 검색어";
    public static final String SEARCH_QUERY_NONEXISTENT = "존재하지않는검색어";

    // === 거리 테스트 상수 ===
    public static final class Distance {
        public static final Double DISTANCE_500M_DOUBLE = 500.0;
        public static final Double DISTANCE_999M_DOUBLE = 999.9;
        public static final Double DISTANCE_1000M_DOUBLE = 1000.0;
        public static final Double DISTANCE_1500M_DOUBLE = 1500.0;
        public static final Double DISTANCE_2340M_DOUBLE = 2340.7;
        public static final Double DISTANCE_10000M_DOUBLE = 10000.0;

        public static final String DISTANCE_500M_TEXT = "500m";
        public static final String DISTANCE_1000M_TEXT = "1000m";
        public static final String DISTANCE_1KM_TEXT = "1.0km";
        public static final String DISTANCE_1_5KM_TEXT = "1.5km";
        public static final String DISTANCE_2_3KM_TEXT = "2.3km";
        public static final String DISTANCE_10KM_TEXT = "10.0km";
    }

    // === 시간 테스트 상수 ===
    public static final class Time {
        public static final String TIME_09_30 = "09:30";
        public static final String TIME_12_00 = "12:00";
        public static final String TIME_18_45 = "18:45";
        public static final String TIME_23_59 = "23:59";
        public static final String TIME_00_00 = "00:00";

        public static final int MINUTES_570 = 570;
        public static final int MINUTES_720 = 720;
        public static final int MINUTES_1125 = 1125;
        public static final int MINUTES_1439 = 1439;
        public static final int MINUTES_0 = 0;
    }

    // === 테스트 컬렉션 크기 상수 ===
    public static final int EXPECTED_TOTAL_CATEGORIES = 5;
    public static final int EXPECTED_WEEKDAYS_COUNT = 7;
    public static final int EXPECTED_MENU_COUNT = 2;
    public static final int EXPECTED_SORT_TEST_SIZE = 3;

    // === Moment 개수 테스트 상수 ===
    public static final Long MOMENT_COUNT_HIGH = 10L;
    public static final Long MOMENT_COUNT_MEDIUM = 5L;
    public static final Long MOMENT_COUNT_LOW = 2L;
    public static final Long MOMENT_COUNT_ZERO = 0L;
}