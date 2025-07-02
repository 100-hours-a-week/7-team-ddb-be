package com.dolpin.global.helper;

import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.*;
import com.dolpin.global.constants.PlaceTestConstants;
import com.dolpin.global.fixture.PlaceFixture;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class PlaceTestHelper {

    // === 영속화 메서드 ===
    public Place savePlace(TestEntityManager entityManager, Place place) {
        return entityManager.persistAndFlush(place);
    }

    public Keyword saveKeyword(TestEntityManager entityManager, Keyword keyword) {
        return entityManager.persistAndFlush(keyword);
    }

    // === 복합 영속화 메서드 ===
    public Place savePlaceWithKeywords(TestEntityManager entityManager, Place place, List<String> keywords) {
        Place savedPlace = entityManager.persistAndFlush(place);

        for (String keywordText : keywords) {
            Keyword keyword = saveKeyword(entityManager, PlaceFixture.createKeyword(keywordText));
            entityManager.persistAndFlush(PlaceFixture.createPlaceKeyword(savedPlace, keyword));
        }

        return savedPlace;
    }

    public Place savePlaceWithMenus(TestEntityManager entityManager, Place place, List<PlaceMenu> menus) {
        Place savedPlace = entityManager.persistAndFlush(place);

        for (PlaceMenu menu : menus) {
            PlaceMenu menuWithPlace = PlaceMenu.builder()
                    .place(savedPlace)
                    .menuName(menu.getMenuName())
                    .price(menu.getPrice())
                    .build();
            entityManager.persistAndFlush(menuWithPlace);
        }

        return savedPlace;
    }

    public Place savePlaceWithHours(TestEntityManager entityManager, Place place, List<PlaceHours> hours) {
        Place savedPlace = entityManager.persistAndFlush(place);

        for (PlaceHours hour : hours) {
            PlaceHours hourWithPlace = PlaceHours.builder()
                    .place(savedPlace)
                    .dayOfWeek(hour.getDayOfWeek())
                    .openTime(hour.getOpenTime())
                    .closeTime(hour.getCloseTime())
                    .isBreakTime(hour.getIsBreakTime())
                    .build();
            entityManager.persistAndFlush(hourWithPlace);
        }

        return savedPlace;
    }

    // === 완전한 장소 저장 ===
    public Place saveCompletePlace(TestEntityManager entityManager, Place place,
                                   List<String> keywords, List<PlaceMenu> menus, List<PlaceHours> hours) {
        Place savedPlace = savePlaceWithKeywords(entityManager, place, keywords);

        // 메뉴 추가
        for (PlaceMenu menu : menus) {
            PlaceMenu menuWithPlace = PlaceMenu.builder()
                    .place(savedPlace)
                    .menuName(menu.getMenuName())
                    .price(menu.getPrice())
                    .build();
            entityManager.persistAndFlush(menuWithPlace);
        }

        // 영업시간 추가
        for (PlaceHours hour : hours) {
            PlaceHours hourWithPlace = PlaceHours.builder()
                    .place(savedPlace)
                    .dayOfWeek(hour.getDayOfWeek())
                    .openTime(hour.getOpenTime())
                    .closeTime(hour.getCloseTime())
                    .isBreakTime(hour.getIsBreakTime())
                    .build();
            entityManager.persistAndFlush(hourWithPlace);
        }

        return savedPlace;
    }

    // === 테스트 데이터 세트 생성 ===
    public List<Place> savePlacesForCategoryTest(TestEntityManager entityManager) {
        Place cafe1 = savePlace(entityManager, PlaceFixture.createCafe(
                PlaceTestConstants.TEST_CAFE_NAME + "1", PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG));
        Place cafe2 = savePlace(entityManager, PlaceFixture.createCafe(
                PlaceTestConstants.TEST_CAFE_NAME + "2", PlaceTestConstants.NEAR_LAT, PlaceTestConstants.NEAR_LNG));
        Place cafe3 = savePlace(entityManager, PlaceFixture.createCafe(
                PlaceTestConstants.TEST_CAFE_NAME + "3", PlaceTestConstants.BAR_LAT, PlaceTestConstants.BAR_LNG));

        Place restaurant1 = savePlace(entityManager, PlaceFixture.createRestaurant(
                PlaceTestConstants.TEST_RESTAURANT_NAME + "1", PlaceTestConstants.RESTAURANT1_LAT, PlaceTestConstants.RESTAURANT1_LNG));
        Place restaurant2 = savePlace(entityManager, PlaceFixture.createRestaurant(
                PlaceTestConstants.TEST_RESTAURANT_NAME + "2", PlaceTestConstants.RESTAURANT2_LAT, PlaceTestConstants.RESTAURANT2_LNG));

        Place bar = savePlace(entityManager, PlaceFixture.createBar(
                PlaceTestConstants.TEST_BAR_NAME + "1", PlaceTestConstants.SORT_TEST_FAR_LAT, PlaceTestConstants.SORT_TEST_FAR_LNG));

        return List.of(cafe1, cafe2, cafe3, restaurant1, restaurant2, bar);
    }

    public List<Place> setupSortTestData(TestEntityManager entityManager) {
        Place place1 = PlaceFixture.createCafe(PlaceTestConstants.BEST_CAFE_NAME,
                PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG);
        Place place2 = PlaceFixture.createCafe(PlaceTestConstants.GOOD_CAFE_NAME,
                PlaceTestConstants.SORT_TEST_PLACE2_LAT, PlaceTestConstants.SORT_TEST_PLACE2_LNG);
        Place place3 = PlaceFixture.createCafe(PlaceTestConstants.ORDINARY_CAFE_NAME,
                PlaceTestConstants.SORT_TEST_PLACE3_LAT, PlaceTestConstants.SORT_TEST_PLACE3_LNG);

        Place saved1 = savePlace(entityManager, place1);
        Place saved2 = savePlace(entityManager, place2);
        Place saved3 = savePlace(entityManager, place3);

        return List.of(saved1, saved2, saved3);
    }

    // === 키워드별 장소 생성 ===
    public Place saveCafeWithKeywords(TestEntityManager entityManager, String name,
                                      double lat, double lng, List<String> keywords) {
        Place cafe = PlaceFixture.createCafe(name, lat, lng);
        return savePlaceWithKeywords(entityManager, cafe, keywords);
    }

    // === 기본 데이터 생성 ===
    public Place saveBasicCafeWithAllData(TestEntityManager entityManager) {
        Place cafe = PlaceFixture.createBasicCafe();

        List<String> keywords = List.of(PlaceTestConstants.COZY_KEYWORD, PlaceTestConstants.DELICIOUS_KEYWORD);
        List<PlaceMenu> menus = List.of(
                PlaceFixture.createAmericanoMenu(cafe),
                PlaceFixture.createLatteMenu(cafe)
        );
        List<PlaceHours> hours = List.of(
                PlaceFixture.createMondayHours(cafe),
                PlaceFixture.createTuesdayHours(cafe)
        );

        return saveCompletePlace(entityManager, cafe, keywords, menus, hours);
    }

    // === 영속성 컨텍스트 관리 ===
    public void clearPersistenceContext(TestEntityManager entityManager) {
        entityManager.flush();
        entityManager.clear();
    }

    // === 검증 헬퍼 메서드들 ===
    public void assertPlaceBasicInfo(Place place, String expectedName, String expectedCategory) {
        assertThat(place.getName()).isEqualTo(expectedName);
        assertThat(place.getCategory()).isEqualTo(expectedCategory);
        assertThat(place.getRoadAddress()).isEqualTo(PlaceTestConstants.DEFAULT_ROAD_ADDRESS);
        assertThat(place.getImageUrl()).isEqualTo(PlaceTestConstants.DEFAULT_IMAGE_URL);
    }

    public void assertPlaceLocation(Place place, double expectedLat, double expectedLng) {
        assertThat(place.getLocation().getY()).isEqualTo(expectedLat);
        assertThat(place.getLocation().getX()).isEqualTo(expectedLng);
    }

    public void assertDistanceOrdering(List<PlaceWithDistance> results) {
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getDistance())
                    .isLessThanOrEqualTo(results.get(i + 1).getDistance());
        }
    }

    public void assertKeywordsMatch(List<PlaceKeyword> actualKeywords, List<String> expectedKeywords) {
        List<String> actualKeywordStrings = actualKeywords.stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .toList();
        assertThat(actualKeywordStrings).containsExactlyInAnyOrderElementsOf(expectedKeywords);
    }

    public void assertMenusMatch(List<PlaceMenu> actualMenus, List<String> expectedMenuNames) {
        List<String> actualMenuNames = actualMenus.stream()
                .map(PlaceMenu::getMenuName)
                .toList();
        assertThat(actualMenuNames).containsExactlyInAnyOrderElementsOf(expectedMenuNames);
    }

    // === 테스트 데이터 생성 헬퍼 ===
    public Map<Long, Long> createMomentCountMap(List<Long> placeIds, List<Long> counts) {
        if (placeIds.size() != counts.size()) {
            throw new IllegalArgumentException("PlaceIds와 counts의 크기가 일치하지 않습니다.");
        }

        return placeIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        placeIds::indexOf,
                        (existing, replacement) -> existing))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> counts.get(entry.getValue())));
    }
}
