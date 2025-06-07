package com.dolpin.global.helper;

import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.*;
import com.dolpin.global.constants.TestConstants;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.*;

import static org.mockito.Mockito.*;

public class PlaceServiceTestHelper {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private PlaceServiceTestHelper() {
    }

    public static Place createPlaceStub(Long id, String name, double lat, double lng) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        when(place.getLocation()).thenReturn(location);
        when(place.getImageUrl()).thenReturn(TestConstants.DEFAULT_IMAGE_URL);
        return place;
    }

    public static Place createBasicPlaceStub() {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(TestConstants.CENTER_LNG, TestConstants.CENTER_LAT));
        Place place = mock(Place.class);

        when(place.getId()).thenReturn(TestConstants.PLACE_ID_1);
        when(place.getName()).thenReturn(TestConstants.TEST_CAFE_NAME);
        when(place.getLocation()).thenReturn(location);
        when(place.getRoadAddress()).thenReturn(TestConstants.DEFAULT_ROAD_ADDRESS);
        when(place.getImageUrl()).thenReturn(TestConstants.DEFAULT_IMAGE_URL);
        when(place.getDescription()).thenReturn(TestConstants.DEFAULT_DESCRIPTION);
        when(place.getPhone()).thenReturn(TestConstants.DEFAULT_PHONE);

        return place;
    }

    public static Place createPlaceStubWithKeywords(Long id, String name, List<String> keywordStrings) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(TestConstants.CENTER_LNG, TestConstants.CENTER_LAT));
        Place place = mock(Place.class);

        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        when(place.getLocation()).thenReturn(location);
        when(place.getImageUrl()).thenReturn(TestConstants.DEFAULT_IMAGE_URL);

        List<PlaceKeyword> keywordStubs = createKeywordStubs(keywordStrings);
        when(place.getKeywords()).thenReturn(keywordStubs);

        return place;
    }

    public static Place createPlaceStubWithKeywords(List<String> keywordStrings) {
        Place place = mock(Place.class);
        List<PlaceKeyword> keywordStubs = createKeywordStubs(keywordStrings);
        when(place.getKeywords()).thenReturn(keywordStubs);
        return place;
    }

    public static Place createPlaceStubWithMenus(List<PlaceMenu> menus) {
        Place place = mock(Place.class);
        when(place.getMenus()).thenReturn(menus);
        return place;
    }

    public static Place createPlaceStubWithHours(List<PlaceHours> hours) {
        Place place = mock(Place.class);
        when(place.getHours()).thenReturn(hours);
        return place;
    }

    // === 연관 엔티티 Mock 생성 ===
    public static List<PlaceKeyword> createKeywordStubs(List<String> keywordStrings) {
        if (keywordStrings == null || keywordStrings.isEmpty()) {
            return Collections.emptyList();
        }

        return keywordStrings.stream()
                .map(PlaceServiceTestHelper::createKeywordStub)
                .toList();
    }

    private static PlaceKeyword createKeywordStub(String keywordString) {
        Keyword keyword = mock(Keyword.class);
        when(keyword.getKeyword()).thenReturn(keywordString);

        PlaceKeyword placeKeyword = mock(PlaceKeyword.class);
        when(placeKeyword.getKeyword()).thenReturn(keyword);

        return placeKeyword;
    }

    public static PlaceMenu createMenuStub(String name, Integer price) {
        PlaceMenu menu = mock(PlaceMenu.class);
        when(menu.getMenuName()).thenReturn(name);
        when(menu.getPrice()).thenReturn(price);
        return menu;
    }

    public static PlaceHours createHoursStub(String day, String openTime, String closeTime, Boolean isBreakTime) {
        PlaceHours hours = mock(PlaceHours.class);
        when(hours.getDayOfWeek()).thenReturn(day);
        when(hours.getIsBreakTime()).thenReturn(isBreakTime);

        if (openTime != null) {
            when(hours.getOpenTime()).thenReturn(openTime);
        }
        if (closeTime != null) {
            when(hours.getCloseTime()).thenReturn(closeTime);
        }

        return hours;
    }

    // === 기본 테스트 데이터 ===
    public static List<PlaceMenu> createDefaultCafeMenus() {
        return List.of(
                createMenuStub(TestConstants.AMERICANO_MENU, TestConstants.AMERICANO_PRICE),
                createMenuStub(TestConstants.LATTE_MENU, TestConstants.LATTE_PRICE)
        );
    }

    public static List<String> getDefaultCafeKeywords() {
        return List.of(TestConstants.COZY_KEYWORD, TestConstants.DELICIOUS_KEYWORD);
    }

    public static List<String> getChainStoreKeywords() {
        return List.of(TestConstants.CHAIN_STORE_KEYWORD, TestConstants.SPACIOUS_KEYWORD);
    }

    public static List<String> getDessertKeywords() {
        return List.of(TestConstants.DESSERT_KEYWORD, TestConstants.CAKE_KEYWORD);
    }

    public static List<PlaceHours> createCompleteBusinessHoursStubs() {
        List<PlaceHours> hours = new ArrayList<>();

        hours.add(createHoursStub(TestConstants.MONDAY, TestConstants.OPEN_TIME, TestConstants.CLOSE_TIME, false));
        hours.add(createHoursStub(TestConstants.MONDAY, TestConstants.BREAK_START_TIME, TestConstants.BREAK_END_TIME, true));

        String[] workingDays = {TestConstants.TUESDAY, TestConstants.WEDNESDAY, TestConstants.THURSDAY, TestConstants.FRIDAY, TestConstants.SATURDAY};
        for (String day : workingDays) {
            hours.add(createHoursStub(day, TestConstants.OPEN_TIME, TestConstants.CLOSE_TIME, false));
        }

        hours.add(createHoursStub(TestConstants.SUNDAY, null, null, false));

        return hours;
    }

    // === AI 응답 생성 ===
    public static PlaceAiResponse.PlaceRecommendation createRecommendation(Long id, Double score, List<String> keywords) {
        return new PlaceAiResponse.PlaceRecommendation(id, score, keywords);
    }

    public static PlaceAiResponse createAiResponse(List<PlaceAiResponse.PlaceRecommendation> recommendations) {
        return createAiResponse(recommendations, null);
    }

    public static PlaceAiResponse createAiResponse(List<PlaceAiResponse.PlaceRecommendation> recommendations, String category) {
        return new PlaceAiResponse(recommendations, category);
    }

    public static PlaceAiResponse createEmptyAiResponse() {
        return new PlaceAiResponse(Collections.emptyList(), null);
    }

    // === PlaceWithDistance 구현체 생성 ===
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

    // === 테스트용 데이터 세트 ===
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

    public static PlaceAiResponse createSortTestAiResponse() {
        return new PlaceAiResponse(List.of(
                createRecommendation(TestConstants.PLACE_ID_1, TestConstants.SIMILARITY_SCORE_LOW, List.of(TestConstants.ORDINARY_KEYWORD)),
                createRecommendation(TestConstants.PLACE_ID_2, TestConstants.SIMILARITY_SCORE_HIGH, List.of(TestConstants.BEST_KEYWORD)),
                createRecommendation(TestConstants.PLACE_ID_3, TestConstants.SIMILARITY_SCORE_MEDIUM, List.of(TestConstants.GOOD_KEYWORD))
        ), null);
    }

    public static List<Place> createSortTestPlaceStubs() {
        return List.of(
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_1, TestConstants.ORDINARY_CAFE_NAME, List.of(TestConstants.ORDINARY_KEYWORD)),
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_2, TestConstants.BEST_CAFE_NAME, List.of(TestConstants.BEST_KEYWORD)),
                createPlaceStubWithKeywords(TestConstants.PLACE_ID_3, TestConstants.GOOD_CAFE_NAME, List.of(TestConstants.GOOD_KEYWORD))
        );
    }

    // === Moment 개수 테스트 데이터 ===
    public static List<Object[]> createMomentCountTestData(List<Long> placeIds, List<Long> counts) {
        if (placeIds.size() != counts.size()) {
            throw new IllegalArgumentException("PlaceIds와 counts의 크기가 일치하지 않습니다.");
        }

        List<Object[]> result = new ArrayList<>();
        for (int i = 0; i < placeIds.size(); i++) {
            result.add(new Object[]{placeIds.get(i), counts.get(i)});
        }
        return result;
    }
}
