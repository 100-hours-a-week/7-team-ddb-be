package com.dolpin.global.helper;

import com.dolpin.domain.place.dto.response.*;
import com.dolpin.domain.place.entity.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.mockito.BDDMockito.*;

public class PlaceTestHelper {

    public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    public static final double DEFAULT_RADIUS = 1000.0;

    // AI 응답 관련
    public static PlaceAiResponse.PlaceRecommendation createRecommendation(Long id, Double score, List<String> keywords) {
        PlaceAiResponse.PlaceRecommendation recommendation = new PlaceAiResponse.PlaceRecommendation();
        ReflectionTestUtils.setField(recommendation, "id", id);
        ReflectionTestUtils.setField(recommendation, "similarityScore", score);
        ReflectionTestUtils.setField(recommendation, "keyword", keywords);
        return recommendation;
    }

    // PlaceWithDistance Mock 생성
    public static PlaceWithDistance createMockPlaceWithDistance(Long id, String name, double lat, double lng, double distance) {
        return new PlaceWithDistance() {
            @Override public Long getId() { return id; }
            @Override public String getName() { return name; }
            @Override public String getCategory() { return "카페"; }
            @Override public String getRoadAddress() { return "테스트 주소"; }
            @Override public String getLotAddress() { return "테스트 지번"; }
            @Override public Double getDistance() { return distance; }
            @Override public Double getLongitude() { return lng; }
            @Override public Double getLatitude() { return lat; }
            @Override public String getImageUrl() { return "test.jpg"; }
        };
    }

    // Place Mock 생성
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
        given(place.getRoadAddress()).willReturn("테스트 주소");
        given(place.getImageUrl()).willReturn("test.jpg");
        given(place.getDescription()).willReturn("테스트 설명");
        given(place.getPhone()).willReturn("02-1234-5678");
        return place;
    }

    // 키워드가 포함된 Place Mock 생성
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
    // PlaceMenu Mock 생성
    public static PlaceMenu createMockMenu(String name, Integer price) {
        PlaceMenu menu = mock(PlaceMenu.class);
        given(menu.getMenuName()).willReturn(name);
        given(menu.getPrice()).willReturn(price);
        return menu;
    }

    // PlaceHours Mock 생성
    public static PlaceHours createMockHours(String day, String openTime, String closeTime, Boolean isBreakTime) {
        PlaceHours hours = mock(PlaceHours.class);
        given(hours.getDayOfWeek()).willReturn(day);
        given(hours.getOpenTime()).willReturn(openTime);
        given(hours.getCloseTime()).willReturn(closeTime);
        given(hours.getIsBreakTime()).willReturn(isBreakTime);
        return hours;
    }

    // 복잡한 영업시간 생성 헬퍼
    public static List<PlaceHours> createCompleteBusinessHours() {
        List<PlaceHours> hours = new ArrayList<>();

        // 월요일 - 영업시간
        PlaceHours monOpen = mock(PlaceHours.class);
        given(monOpen.getDayOfWeek()).willReturn("월");
        given(monOpen.getOpenTime()).willReturn("09:00");
        given(monOpen.getCloseTime()).willReturn("21:00");
        given(monOpen.getIsBreakTime()).willReturn(false);
        hours.add(monOpen);

        // 월요일 - 브레이크타임
        PlaceHours monBreak = mock(PlaceHours.class);
        given(monBreak.getDayOfWeek()).willReturn("월");
        given(monBreak.getOpenTime()).willReturn("15:00");
        given(monBreak.getCloseTime()).willReturn("16:00");
        given(monBreak.getIsBreakTime()).willReturn(true);
        hours.add(monBreak);

        // 화요일~토요일 기본 영업
        String[] days = {"화", "수", "목", "금", "토"};
        for (String day : days) {
            PlaceHours dayHours = mock(PlaceHours.class);
            given(dayHours.getDayOfWeek()).willReturn(day);
            given(dayHours.getOpenTime()).willReturn("09:00");
            given(dayHours.getCloseTime()).willReturn("21:00");
            given(dayHours.getIsBreakTime()).willReturn(false);
            hours.add(dayHours);
        }

        return hours;
    }
}
