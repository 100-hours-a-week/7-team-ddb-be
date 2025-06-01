package com.dolpin.global.helper;

import com.dolpin.domain.place.dto.response.*;
import com.dolpin.domain.place.entity.*;
import com.dolpin.global.constants.TestConstants;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.mockito.BDDMockito.*;

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
        PlaceAiResponse.PlaceRecommendation recommendation = new PlaceAiResponse.PlaceRecommendation();
        ReflectionTestUtils.setField(recommendation, "id", id);
        ReflectionTestUtils.setField(recommendation, "similarityScore", score);
        ReflectionTestUtils.setField(recommendation, "keyword", keywords);
        return recommendation;
    }

    public static PlaceWithDistance createMockPlaceWithDistance(Long id, String name, double lat, double lng, double distance) {
        return createMockPlaceWithDistance(id, name, TestConstants.CAFE_CATEGORY, lat, lng, distance);
    }

    public static PlaceWithDistance createMockPlaceWithDistance(Long id, String name, String category, double lat, double lng, double distance) {
        return new PlaceWithDistance() {
            @Override public Long getId() { return id; }
            @Override public String getName() { return name; }
            @Override public String getCategory() { return category; }
            @Override public String getRoadAddress() { return TestConstants.DEFAULT_ROAD_ADDRESS; }
            @Override public String getLotAddress() { return TestConstants.DEFAULT_LOT_ADDRESS; }
            @Override public Double getDistance() { return distance; }
            @Override public Double getLongitude() { return lng; }
            @Override public Double getLatitude() { return lat; }
            @Override public String getImageUrl() { return TestConstants.DEFAULT_IMAGE_URL; }
        };
    }

    public static Place createMockPlace(Long id, String name, double lat, double lng) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        Place place = mock(Place.class);
        given(place.getId()).willReturn(id);
        given(place.getName()).willReturn(name);
        given(place.getLocation()).willReturn(location);
        given(place.getKeywords()).willReturn(Collections.emptyList());
        return place;
    }

    public static Place createMockPlaceForDetail(Long id, String name, double lat, double lng) {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        Place place = mock(Place.class);
        given(place.getId()).willReturn(id);
        given(place.getName()).willReturn(name);
        given(place.getLocation()).willReturn(location);
        given(place.getRoadAddress()).willReturn(TestConstants.DEFAULT_ROAD_ADDRESS);
        given(place.getImageUrl()).willReturn(TestConstants.DEFAULT_IMAGE_URL);
        given(place.getDescription()).willReturn(TestConstants.DEFAULT_DESCRIPTION);
        given(place.getPhone()).willReturn(TestConstants.DEFAULT_PHONE);
        return place;
    }

    public static Place createMockPlaceWithKeywords(Long id, String name, List<String> keywordStrings) {
        Place place = mock(Place.class);

        List<PlaceKeyword> keywords = keywordStrings.stream()
                .map(kw -> {
                    Keyword keyword = mock(Keyword.class);
                    given(keyword.getKeyword()).willReturn(kw);
                    PlaceKeyword placeKeyword = mock(PlaceKeyword.class);
                    given(placeKeyword.getKeyword()).willReturn(keyword);
                    return placeKeyword;
                })
                .toList();
        given(place.getKeywords()).willReturn(keywords);
        return place;
    }

    public static PlaceMenu createMockMenu(String name, Integer price) {
        PlaceMenu menu = mock(PlaceMenu.class);
        given(menu.getMenuName()).willReturn(name);
        given(menu.getPrice()).willReturn(price);
        return menu;
    }

    public static PlaceHours createMockHours(String day, String openTime, String closeTime, Boolean isBreakTime) {
        PlaceHours hours = mock(PlaceHours.class);
        given(hours.getDayOfWeek()).willReturn(day);
        given(hours.getOpenTime()).willReturn(openTime);
        given(hours.getCloseTime()).willReturn(closeTime);
        given(hours.getIsBreakTime()).willReturn(isBreakTime);
        return hours;
    }

    public static List<PlaceHours> createCompleteBusinessHours() {
        List<PlaceHours> hours = new ArrayList<>();

        PlaceHours monOpen = mock(PlaceHours.class);
        given(monOpen.getDayOfWeek()).willReturn(TestConstants.MONDAY);
        given(monOpen.getOpenTime()).willReturn(TestConstants.OPEN_TIME);
        given(monOpen.getCloseTime()).willReturn(TestConstants.CLOSE_TIME);
        given(monOpen.getIsBreakTime()).willReturn(false);
        hours.add(monOpen);

        PlaceHours monBreak = mock(PlaceHours.class);
        given(monBreak.getDayOfWeek()).willReturn(TestConstants.MONDAY);
        given(monBreak.getOpenTime()).willReturn("15:00");
        given(monBreak.getCloseTime()).willReturn("16:00");
        given(monBreak.getIsBreakTime()).willReturn(true);
        hours.add(monBreak);

        String[] days = {TestConstants.TUESDAY, "수", "목", "금", "토"};
        for (String day : days) {
            PlaceHours dayHours = mock(PlaceHours.class);
            given(dayHours.getDayOfWeek()).willReturn(day);
            given(dayHours.getOpenTime()).willReturn(TestConstants.OPEN_TIME);
            given(dayHours.getCloseTime()).willReturn(TestConstants.CLOSE_TIME);
            given(dayHours.getIsBreakTime()).willReturn(false);
            hours.add(dayHours);
        }

        return hours;
    }
}
