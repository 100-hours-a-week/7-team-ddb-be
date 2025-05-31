package com.dolpin.global.fixture;

import com.dolpin.domain.place.entity.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

public class PlaceFixture {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public static Long createAndPersistPlace(TestEntityManager entityManager, String name, double lat, double lng) {
        entityManager.getEntityManager().createNativeQuery("""
            INSERT INTO place (name, location, road_address, category, image_url, description, phone, created_at, updated_at)
            VALUES (?1, ST_SetSRID(ST_Point(?2, ?3), 4326), ?4, ?5, ?6, ?7, ?8, NOW(), NOW())
            """)
                .setParameter(1, name)
                .setParameter(2, lng)
                .setParameter(3, lat)
                .setParameter(4, "테스트 주소")
                .setParameter(5, "카페")
                .setParameter(6, "test.jpg")
                .setParameter(7, "테스트 설명")
                .setParameter(8, "02-1234-5678")
                .executeUpdate();

        entityManager.flush();

        return (Long) entityManager.getEntityManager()
                .createNativeQuery("SELECT id FROM place WHERE name = ? ORDER BY id DESC LIMIT 1")
                .setParameter(1, name)
                .getSingleResult();
    }

    public static Long createPlaceWithCategory(TestEntityManager entityManager, String name, String category, double lat, double lng) {
        entityManager.getEntityManager().createNativeQuery("""
           INSERT INTO place (name, location, road_address, category, image_url, description, phone, created_at, updated_at)
            VALUES (?1, ST_SetSRID(ST_Point(?2, ?3), 4326), ?4, ?5, ?6, ?7, ?8, NOW(), NOW())
            """)
                .setParameter(1, name)
                .setParameter(2, lng)
                .setParameter(3, lat)
                .setParameter(4, "테스트 주소")
                .setParameter(5, category)
                .setParameter(6, "test.jpg")
                .setParameter(7, "테스트 설명")
                .setParameter(8, "02-1234-5678")
                .executeUpdate();

        entityManager.flush();

        return (Long) entityManager.getEntityManager()
                .createNativeQuery("SELECT id FROM place WHERE name = ? AND category = ? ORDER BY id DESC LIMIT 1")
                .setParameter(1, name)
                .setParameter(2, category)
                .getSingleResult();
    }

    public static PlaceHours createAndPersistPlaceHours(TestEntityManager entityManager, Long placeId, String dayOfWeek, String openTime, String closeTime) {
        PlaceHours placeHours = PlaceHours.builder()
                .dayOfWeek(dayOfWeek)
                .openTime(openTime)
                .closeTime(closeTime)
                .isBreakTime(false)
                .build();

        Place place = entityManager.find(Place.class, placeId);

        try {
            var field = PlaceHours.class.getDeclaredField("place");
            field.setAccessible(true);
            field.set(placeHours, place);
        } catch (Exception e) {
            throw new RuntimeException("PlaceHours 생성 실패", e);
        }

        return entityManager.persistAndFlush(placeHours);
    }

    public static PlaceMenu createAndPersistPlaceMenu(TestEntityManager entityManager, Long placeId, String menuName, Integer price) {
        PlaceMenu placeMenu = PlaceMenu.builder()
                .menuName(menuName)
                .price(price)
                .build();

        Place place = entityManager.find(Place.class, placeId);

        try {
            var field = PlaceMenu.class.getDeclaredField("place");
            field.setAccessible(true);
            field.set(placeMenu, place);
        } catch (Exception e) {
            throw new RuntimeException("PlaceMenu 생성 실패", e);
        }

        return entityManager.persistAndFlush(placeMenu);
    }

    public static Keyword createAndPersistKeyword(TestEntityManager entityManager, String keyword) {
        Keyword keywordEntity = Keyword.builder()
                .keyword(keyword)
                .build();
        return entityManager.persistAndFlush(keywordEntity);
    }

    public static PlaceKeyword createAndPersistPlaceKeyword(TestEntityManager entityManager, Long placeId, Long keywordId) {
        Place place = entityManager.find(Place.class, placeId);
        Keyword keyword = entityManager.find(Keyword.class, keywordId);

        PlaceKeyword placeKeyword = PlaceKeyword.builder()
                .place(place)
                .keyword(keyword)
                .build();

        return entityManager.persistAndFlush(placeKeyword);
    }
}
