package com.dolpin.global.helper;

import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.*;
import com.dolpin.global.constants.PlaceTestConstants;
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

    // === 기본 Place Stub 생성 - 테스트별 필요한 정보만 stubbing ===
    public static Place createPlaceStub(Long id, String name, double lat, double lng) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        when(place.getLocation()).thenReturn(location);
        when(place.getImageUrl()).thenReturn(PlaceTestConstants.DEFAULT_IMAGE_URL);
        return place;
    }

    // 성공 케이스용 - 모든 정보 포함
    public static Place createBasicPlaceForDetail() {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(PlaceTestConstants.CENTER_LNG, PlaceTestConstants.CENTER_LAT));
        Place place = mock(Place.class);

        when(place.getId()).thenReturn(PlaceTestConstants.PLACE_ID_1);
        when(place.getName()).thenReturn(PlaceTestConstants.TEST_CAFE_NAME);
        when(place.getLocation()).thenReturn(location);
        when(place.getRoadAddress()).thenReturn(PlaceTestConstants.DEFAULT_ROAD_ADDRESS);
        when(place.getImageUrl()).thenReturn(PlaceTestConstants.DEFAULT_IMAGE_URL);
        when(place.getDescription()).thenReturn(PlaceTestConstants.DEFAULT_DESCRIPTION);
        when(place.getPhone()).thenReturn(PlaceTestConstants.DEFAULT_PHONE);

        return place;
    }

    // 실패 케이스용 - 아무 stubbing도 하지 않음 (메서드가 호출되지 않기 때문)
    public static Place createMinimalPlaceForFailure() {
        return mock(Place.class);
    }

    // 키워드 조회용 Place
    public static Place createPlaceWithKeywords(List<String> keywordStrings) {
        Place place = mock(Place.class);
        List<PlaceKeyword> keywordStubs = createKeywordStubs(keywordStrings);
        when(place.getKeywords()).thenReturn(keywordStubs);
        return place;
    }

    // 메뉴 조회용 Place
    public static Place createPlaceWithMenus(List<PlaceMenu> menus) {
        Place place = mock(Place.class);
        when(place.getMenus()).thenReturn(menus);
        return place;
    }

    // 영업시간 조회용 Place
    public static Place createPlaceWithHours(List<PlaceHours> hours) {
        Place place = mock(Place.class);
        when(place.getHours()).thenReturn(hours);
        return place;
    }

    // === 키워드 관련 Place Stub ===
    public static Place createPlaceStubWithKeywords(Long id, String name, List<String> keywordStrings) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(PlaceTestConstants.CENTER_LNG, PlaceTestConstants.CENTER_LAT));
        Place place = mock(Place.class);

        when(place.getId()).thenReturn(id);
        when(place.getName()).thenReturn(name);
        when(place.getLocation()).thenReturn(location);
        when(place.getImageUrl()).thenReturn(PlaceTestConstants.DEFAULT_IMAGE_URL);

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

    // === 연관 엔티티 Stub 생성 ===
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
                createMenuStub(PlaceTestConstants.AMERICANO_MENU, PlaceTestConstants.AMERICANO_PRICE),
                createMenuStub(PlaceTestConstants.LATTE_MENU, PlaceTestConstants.LATTE_PRICE)
        );
    }

    public static List<String> getDefaultCafeKeywords() {
        return List.of(PlaceTestConstants.COZY_KEYWORD, PlaceTestConstants.DELICIOUS_KEYWORD);
    }

    public static List<String> getChainStoreKeywords() {
        return List.of(PlaceTestConstants.CHAIN_STORE_KEYWORD, PlaceTestConstants.SPACIOUS_KEYWORD);
    }

    public static List<String> getDessertKeywords() {
        return List.of(PlaceTestConstants.DESSERT_KEYWORD, PlaceTestConstants.CAKE_KEYWORD);
    }

    public static List<PlaceHours> createCompleteBusinessHoursStubs() {
        List<PlaceHours> hours = new ArrayList<>();

        hours.add(createHoursStub(PlaceTestConstants.MONDAY, PlaceTestConstants.OPEN_TIME, PlaceTestConstants.CLOSE_TIME, false));
        hours.add(createHoursStub(PlaceTestConstants.MONDAY, PlaceTestConstants.BREAK_START_TIME, PlaceTestConstants.BREAK_END_TIME, true));

        String[] workingDays = {PlaceTestConstants.TUESDAY, PlaceTestConstants.WEDNESDAY, PlaceTestConstants.THURSDAY, PlaceTestConstants.FRIDAY, PlaceTestConstants.SATURDAY};
        for (String day : workingDays) {
            hours.add(createHoursStub(day, PlaceTestConstants.OPEN_TIME, PlaceTestConstants.CLOSE_TIME, false));
        }

        hours.add(createHoursStub(PlaceTestConstants.SUNDAY, null, null, false));

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
        return createPlaceWithDistance(id, name, PlaceTestConstants.CAFE_CATEGORY,
                PlaceTestConstants.DEFAULT_ROAD_ADDRESS, PlaceTestConstants.DEFAULT_LOT_ADDRESS,
                PlaceTestConstants.DEFAULT_IMAGE_URL, lat, lng, distance);
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
                createPlaceWithDistance(PlaceTestConstants.PLACE_ID_1, PlaceTestConstants.ORDINARY_CAFE_NAME,
                        PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG, PlaceTestConstants.DISTANCE_100M),
                createPlaceWithDistance(PlaceTestConstants.PLACE_ID_2, PlaceTestConstants.BEST_CAFE_NAME,
                        PlaceTestConstants.SORT_TEST_PLACE2_LAT, PlaceTestConstants.SORT_TEST_PLACE2_LNG, PlaceTestConstants.DISTANCE_200M),
                createPlaceWithDistance(PlaceTestConstants.PLACE_ID_3, PlaceTestConstants.GOOD_CAFE_NAME,
                        PlaceTestConstants.SORT_TEST_PLACE3_LAT, PlaceTestConstants.SORT_TEST_PLACE3_LNG, PlaceTestConstants.DISTANCE_300M)
        );
    }

    public static PlaceAiResponse createSortTestAiResponse() {
        return new PlaceAiResponse(List.of(
                createRecommendation(PlaceTestConstants.PLACE_ID_1, PlaceTestConstants.SIMILARITY_SCORE_LOW, List.of(PlaceTestConstants.ORDINARY_KEYWORD)),
                createRecommendation(PlaceTestConstants.PLACE_ID_2, PlaceTestConstants.SIMILARITY_SCORE_HIGH, List.of(PlaceTestConstants.BEST_KEYWORD)),
                createRecommendation(PlaceTestConstants.PLACE_ID_3, PlaceTestConstants.SIMILARITY_SCORE_MEDIUM, List.of(PlaceTestConstants.GOOD_KEYWORD))
        ), null);
    }

    public static List<Place> createSortTestPlaceStubs() {
        return List.of(
                createPlaceStubWithKeywords(PlaceTestConstants.PLACE_ID_1, PlaceTestConstants.ORDINARY_CAFE_NAME, List.of(PlaceTestConstants.ORDINARY_KEYWORD)),
                createPlaceStubWithKeywords(PlaceTestConstants.PLACE_ID_2, PlaceTestConstants.BEST_CAFE_NAME, List.of(PlaceTestConstants.BEST_KEYWORD)),
                createPlaceStubWithKeywords(PlaceTestConstants.PLACE_ID_3, PlaceTestConstants.GOOD_CAFE_NAME, List.of(PlaceTestConstants.GOOD_KEYWORD))
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
