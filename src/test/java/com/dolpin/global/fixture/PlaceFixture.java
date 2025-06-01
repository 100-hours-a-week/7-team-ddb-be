package com.dolpin.global.fixture;

import com.dolpin.domain.place.entity.*;
import com.dolpin.global.constants.TestConstants;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class PlaceFixture {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

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

    public static Place createCafe(String name, double lat, double lng) {
        return createPlace(name, TestConstants.CAFE_CATEGORY, lat, lng);
    }

    public static Place createRestaurant(String name, double lat, double lng) {
        return createPlace(name, TestConstants.RESTAURANT_CATEGORY, lat, lng);
    }

    public static Place createBar(String name, double lat, double lng) {
        return createPlace(name, TestConstants.BAR_CATEGORY, lat, lng);
    }

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
}
