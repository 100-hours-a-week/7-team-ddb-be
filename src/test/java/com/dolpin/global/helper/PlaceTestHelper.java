package com.dolpin.global.helper;

import com.dolpin.domain.place.dto.response.*;
import com.dolpin.domain.place.entity.*;
import com.dolpin.global.constants.TestConstants;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.*;

import static org.mockito.Mockito.*;

public class PlaceTestHelper {

    public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public static void clearContext(TestEntityManager entityManager) {
        entityManager.flush();
        entityManager.clear();
    }

    public static void clearPersistenceContext(TestEntityManager entityManager) {
        entityManager.flush();
        entityManager.clear();
    }

    public static PlaceAiResponse.PlaceRecommendation createRecommendation(Long id, Double score, List<String> keywords) {
        return new PlaceAiResponse.PlaceRecommendation(id, score, keywords);

    }

    public static PlaceAiResponse createAiResponse(List<PlaceAiResponse.PlaceRecommendation> recommendations) {
        return new PlaceAiResponse(recommendations);
    }

    public static PlaceWithDistance createPlaceWithDistance(Long id, String name, double lat, double lng, double distance) {
        return createPlaceWithDistance(id, name, TestConstants.CAFE_CATEGORY,
                TestConstants.DEFAULT_ROAD_ADDRESS, TestConstants.DEFAULT_LOT_ADDRESS,
                TestConstants.DEFAULT_IMAGE_URL, lat, lng, distance);
    }

    public static PlaceWithDistance createPlaceWithDistance(Long id, String name, String category,
                                                            String roadAddress, String lotAddress, String imageUrl,
                                                            double lat, double lng, double distance) {
        return new PlaceWithDistance() {
            @Override public Long getId() { return id; }
            @Override public String getName() { return name; }
            @Override public String getCategory() { return category; }
            @Override public String getRoadAddress() { return roadAddress; }
            @Override public String getLotAddress() { return lotAddress; }
            @Override public Double getDistance() { return distance; }
            @Override public Double getLongitude() { return lng; }
            @Override public Double getLatitude() { return lat; }
            @Override public String getImageUrl() { return imageUrl; }
        };
    }

    public static PlaceWithDistance createPlaceWithDistance(Long id, String name, String category, double lat, double lng, double distance) {
        return createPlaceWithDistance(id, name, category,
                TestConstants.DEFAULT_ROAD_ADDRESS, TestConstants.DEFAULT_LOT_ADDRESS,
                TestConstants.DEFAULT_IMAGE_URL, lat, lng, distance);
    }

    public static PlaceWithDistance createDefaultCafePlaceWithDistance(Long id, double distance) {
        return createPlaceWithDistance(id, TestConstants.TEST_CAFE_NAME + id, TestConstants.CAFE_CATEGORY,
                TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, distance);
    }

    public static PlaceWithDistance createDefaultRestaurantPlaceWithDistance(Long id, double distance) {
        return createPlaceWithDistance(id, TestConstants.TEST_RESTAURANT_NAME + id, TestConstants.RESTAURANT_CATEGORY,
                TestConstants.RESTAURANT1_LAT, TestConstants.RESTAURANT1_LNG, distance);
    }

    public static Place createMockPlace(Long id, String name, double lat, double lng) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        when(place.getLocation()).thenReturn(location);
        when(place.getKeywords()).thenReturn(Collections.emptyList());
        return place;
    }

    public static Place createMockPlaceForDetail(Long id, String name, double lat, double lng) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        when(place.getLocation()).thenReturn(location);
        when(place.getRoadAddress()).thenReturn(TestConstants.DEFAULT_ROAD_ADDRESS);
        when(place.getImageUrl()).thenReturn(TestConstants.DEFAULT_IMAGE_URL);
        when(place.getDescription()).thenReturn(TestConstants.DEFAULT_DESCRIPTION);
        when(place.getPhone()).thenReturn(TestConstants.DEFAULT_PHONE);
        return place;
    }

    public static Place createDefaultMockCafe(Long id) {
        return createMockPlaceForDetail(id, TestConstants.TEST_CAFE_NAME,
                TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
    }

    public static Place createDefaultMockRestaurant(Long id) {
        return createMockPlaceForDetail(id, TestConstants.TEST_RESTAURANT_NAME,
                TestConstants.RESTAURANT1_LAT, TestConstants.RESTAURANT1_LNG);
    }

    public static List<PlaceKeyword> createKeywordMocks(List<String> keywordStrings) {
        if (keywordStrings == null) {
            return Collections.emptyList();
        }

        List<PlaceKeyword> result = new ArrayList<>();
        for (String kw : keywordStrings) {

            Keyword keyword = mock(Keyword.class);

            when(keyword.getKeyword()).thenReturn(kw);

            PlaceKeyword placeKeyword = mock(PlaceKeyword.class);
            when(placeKeyword.getKeyword()).thenReturn(keyword);

            result.add(placeKeyword);
        }
        return result;
    }

    public static Place createMockPlaceWithKeywords(Long id, String name, List<String> keywordStrings) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        List<PlaceKeyword> keywordMocks = createKeywordMocks(keywordStrings);

        doReturn(keywordMocks).when(place).getKeywords();
        return place;
    }

    public static Place createMockPlaceWithKeywordsOnly(List<String> keywordStrings) {
        Place place = mock(Place.class);
        List<PlaceKeyword> keywordMocks = createKeywordMocks(keywordStrings);
        doReturn(keywordMocks).when(place).getKeywords();
        return place;
    }

    public static Place createMockPlaceWithIdAndKeywords(Long id, List<String> keywordStrings) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        List<PlaceKeyword> keywordMocks = createKeywordMocks(keywordStrings);
        doReturn(keywordMocks).when(place).getKeywords();
        return place;
    }

    public static Place createMockPlaceWithFullInfo(Long id, String name, double lat, double lng, List<String> keywordStrings) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        when(place.getLocation()).thenReturn(location);
        List<PlaceKeyword> keywordMocks = createKeywordMocks(keywordStrings);
        doReturn(keywordMocks).when(place).getKeywords();
        return place;
    }

    public static List<Place> createSortTestPlaces() {
        return List.of(
                createMockPlaceWithFullInfo(TestConstants.PLACE_ID_1, TestConstants.ORDINARY_CAFE_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, List.of(TestConstants.ORDINARY_KEYWORD)),
                createMockPlaceWithFullInfo(TestConstants.PLACE_ID_2, TestConstants.BEST_CAFE_NAME,
                        TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG, List.of(TestConstants.BEST_KEYWORD)),
                createMockPlaceWithFullInfo(TestConstants.PLACE_ID_3, TestConstants.GOOD_CAFE_NAME,
                        TestConstants.SORT_TEST_PLACE3_LAT, TestConstants.SORT_TEST_PLACE3_LNG, List.of(TestConstants.GOOD_KEYWORD))
        );
    }

    public static PlaceAiResponse createSortTestAiResponse() {
        return new PlaceAiResponse(List.of(
                createRecommendation(TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_LOW, List.of(TestConstants.ORDINARY_KEYWORD)),
                createRecommendation(TestConstants.PLACE_ID_2, TestConstants.SIMILARITY_SCORE_HIGH, List.of(TestConstants.BEST_KEYWORD)),
                createRecommendation(TestConstants.PLACE_ID_3, TestConstants.SIMILARITY_SCORE_MEDIUM, List.of(TestConstants.GOOD_KEYWORD))
        ));
    }

    public static List<PlaceWithDistance> createSortTestPlaceWithDistances() {
        return List.of(
                createPlaceWithDistance(TestConstants.PLACE_ID_1, TestConstants.ORDINARY_CAFE_NAME,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                createPlaceWithDistance(TestConstants.PLACE_ID_2, TestConstants.BEST_CAFE_NAME,
                        TestConstants.SORT_TEST_PLACE2_LAT, TestConstants.SORT_TEST_PLACE2_LNG, TestConstants.DISTANCE_200M),
                createPlaceWithDistance(TestConstants.PLACE_ID_3, TestConstants.GOOD_CAFE_NAME,
                        TestConstants.SORT_TEST_PLACE3_LAT, TestConstants.SORT_TEST_PLACE3_LNG, TestConstants.DISTANCE_300M)
        );
    }

    public static List<String> getDefaultCafeKeywords() {
        return List.of(TestConstants.COZY_KEYWORD, TestConstants.GOOD_KEYWORD);
    }

    public static List<String> getDefaultRestaurantKeywords() {
        return List.of(TestConstants.DELICIOUS_KEYWORD, TestConstants.GOOD_KEYWORD);
    }

    public static List<String> getChainStoreKeywords() {
        return List.of(TestConstants.CHAIN_STORE_KEYWORD, TestConstants.SPACIOUS_KEYWORD);
    }

    public static List<String> getDessertKeywords() {
        return List.of(TestConstants.DESSERT_KEYWORD, TestConstants.CAKE_KEYWORD);
    }

    public static Place createMockCafeWithDefaultKeywords(Long id, String name) {
        return createMockPlaceWithKeywords(id, name, getDefaultCafeKeywords());
    }

    public static Place createMockRestaurantWithDefaultKeywords(Long id, String name) {
        return createMockPlaceWithKeywords(id, name, getDefaultRestaurantKeywords());
    }

    // ========== 8. 메뉴 및 영업시간 관련 메서드들 ==========
    public static Place createMockPlaceWithMenusOnly(List<PlaceMenu> menus) {
        Place place = mock(Place.class);
        when(place.getMenus()).thenReturn(menus);
        return place;
    }

    public static Place createMockPlaceWithHoursOnly(List<PlaceHours> hours) {
        Place place = mock(Place.class);
        when(place.getHours()).thenReturn(hours);
        return place;
    }

    public static PlaceMenu createMockMenu(String name, Integer price) {
        PlaceMenu menu = mock(PlaceMenu.class);
        when(menu.getMenuName()).thenReturn(name);
        when(menu.getPrice()).thenReturn(price);
        return menu;
    }

    public static PlaceMenu createDefaultAmericano() {
        return createMockMenu(TestConstants.AMERICANO_MENU, TestConstants.AMERICANO_PRICE);
    }

    public static PlaceMenu createDefaultLatte() {
        return createMockMenu(TestConstants.LATTE_MENU, TestConstants.LATTE_PRICE);
    }

    public static List<PlaceMenu> createDefaultCafeMenus() {
        return List.of(createDefaultAmericano(), createDefaultLatte());
    }

    public static PlaceHours createMockHours(String day, String openTime, String closeTime, Boolean isBreakTime) {
        PlaceHours hours = mock(PlaceHours.class);
        when(hours.getDayOfWeek()).thenReturn(day);
        when(hours.getOpenTime()).thenReturn(openTime);
        when(hours.getCloseTime()).thenReturn(closeTime);
        when(hours.getIsBreakTime()).thenReturn(isBreakTime);
        return hours;
    }

    public static PlaceHours createDefaultBusinessHours(String day) {
        return createMockHours(day, TestConstants.OPEN_TIME, TestConstants.CLOSE_TIME, false);
    }

    public static PlaceHours createDefaultBreakTime(String day) {
        return createMockHours(day, TestConstants.BREAK_START_TIME, TestConstants.BREAK_END_TIME, true);
    }

    public static List<PlaceHours> createCompleteBusinessHours() {
        List<PlaceHours> hours = new ArrayList<>();

        hours.add(createDefaultBusinessHours(TestConstants.MONDAY));
        hours.add(createDefaultBreakTime(TestConstants.MONDAY));

        String[] days = {TestConstants.TUESDAY, TestConstants.WEDNESDAY, TestConstants.THURSDAY, TestConstants.FRIDAY, TestConstants.SATURDAY};
        for (String day : days) {
            hours.add(createDefaultBusinessHours(day));
        }

        return hours;
    }

    public static PlaceHours createEarlyCloseHours(String day) {
        return createMockHours(day, TestConstants.OPEN_TIME, TestConstants.EARLY_CLOSE_TIME, false);
    }

    public static PlaceHours createTwentyFourHours(String day) {
        return createMockHours(day, TestConstants.TWENTY_FOUR_HOUR_START, TestConstants.TWENTY_FOUR_HOUR_END, false);
    }

    public static PlaceHours createClosedDay(String day) {
        return createMockHours(day, null, null, false);
    }

    public static Map<String, Object> createCompleteCafeMocks(Long id) {
        Map<String, Object> mocks = new HashMap<>();

        Place basicPlace = createDefaultMockCafe(id);
        Place placeWithKeywords = createMockCafeWithDefaultKeywords(id, TestConstants.TEST_CAFE_NAME);

        Place placeWithMenus = mock(Place.class);
        when(placeWithMenus.getMenus()).thenReturn(createDefaultCafeMenus());

        Place placeWithHours = mock(Place.class);
        when(placeWithHours.getHours()).thenReturn(createCompleteBusinessHours());

        mocks.put("basic", basicPlace);
        mocks.put("withKeywords", placeWithKeywords);
        mocks.put("withMenus", placeWithMenus);
        mocks.put("withHours", placeWithHours);

        return mocks;
    }

    public static List<PlaceWithDistance> createDistanceSortedPlaces() {
        return List.of(
                createPlaceWithDistance(TestConstants.PLACE_ID_1, "가까운 " + TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                        TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                createPlaceWithDistance(TestConstants.PLACE_ID_2, "먼 " + TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                        TestConstants.SORT_TEST_FAR_LAT, TestConstants.SORT_TEST_FAR_LNG, TestConstants.DISTANCE_500M)
        );
    }

    public static PlaceAiResponse createSimilaritySortedAiResponse() {
        List<PlaceAiResponse.PlaceRecommendation> recommendations = List.of(
                createRecommendation(TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_LOW, List.of(TestConstants.ORDINARY_KEYWORD)),
                createRecommendation(TestConstants.PLACE_ID_2, TestConstants.SIMILARITY_SCORE_HIGH, List.of(TestConstants.BEST_KEYWORD)),
                createRecommendation(TestConstants.PLACE_ID_3, TestConstants.SIMILARITY_SCORE_MEDIUM, List.of(TestConstants.GOOD_KEYWORD))
        );
        return createAiResponse(recommendations);
    }

    public static List<PlaceWithDistance> createCategoryPlaces(String category) {
        if (TestConstants.CAFE_CATEGORY.equals(category)) {
            return List.of(
                    createPlaceWithDistance(TestConstants.PLACE_ID_1, TestConstants.TEST_CAFE_NAME + "1", category,
                            TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.DISTANCE_100M),
                    createPlaceWithDistance(TestConstants.PLACE_ID_2, TestConstants.TEST_CAFE_NAME + "2", category,
                            TestConstants.NEAR_LAT, TestConstants.NEAR_LNG, TestConstants.DISTANCE_200M)
            );
        } else if (TestConstants.RESTAURANT_CATEGORY.equals(category)) {
            return List.of(
                    createPlaceWithDistance(TestConstants.PLACE_ID_1, TestConstants.TEST_RESTAURANT_NAME + "1", category,
                            TestConstants.RESTAURANT1_LAT, TestConstants.RESTAURANT1_LNG, TestConstants.DISTANCE_150M)
            );
        }
        return Collections.emptyList();
    }
}
