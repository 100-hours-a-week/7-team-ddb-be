package com.dolpin.domain.place.repository;

import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.*;
import com.dolpin.global.config.TestConfig;
import com.dolpin.global.constants.TestConstants;
import com.dolpin.global.fixture.PlaceFixture;
import com.dolpin.global.helper.PlaceTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@ContextConfiguration(initializers = PlaceRepositoryTest.TestContainerInitializer.class)
@DisplayName("PlaceRepository 테스트")
class PlaceRepositoryTest {

    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:15-3.3-alpine")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    static class TestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.datasource.driver-class-name=org.postgresql.Driver"
            ).applyTo(context.getEnvironment());
        }
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlaceRepository placeRepository;

    @Nested
    @DisplayName("기본 조회 테스트")
    class BasicQueryTest {

        @Test
        @DisplayName("ID로 기본 장소 정보 조회가 정상 동작한다")
        void findBasicPlaceById_ReturnsPlace() {
            Place place = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place savedPlace = entityManager.persistAndFlush(place);
            clearPersistenceContext();

            Optional<Place> result = placeRepository.findBasicPlaceById(savedPlace.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(TestConstants.TEST_CAFE_NAME);
            assertThat(result.get().getCategory()).isEqualTo(TestConstants.CAFE_CATEGORY);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 빈 결과를 반환한다")
        void findBasicPlaceById_WithNonExistentId_ReturnsEmpty() {
            Long nonExistentId = 999L;
            clearPersistenceContext();

            Optional<Place> result = placeRepository.findBasicPlaceById(nonExistentId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("연관 관계 조회 테스트")
    class AssociationQueryTest {

        @Test
        @DisplayName("키워드를 포함한 장소 정보 조회가 정상 동작한다")
        void findByIdWithKeywords_ReturnsPlaceWithKeywords() {
            Place place = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Keyword keyword1 = PlaceFixture.createKeyword(TestConstants.COZY_KEYWORD);
            Keyword keyword2 = PlaceFixture.createKeyword(TestConstants.DELICIOUS_KEYWORD);

            Place savedPlace = entityManager.persistAndFlush(place);
            Keyword savedKeyword1 = entityManager.persistAndFlush(keyword1);
            Keyword savedKeyword2 = entityManager.persistAndFlush(keyword2);

            PlaceKeyword placeKeyword1 = PlaceFixture.createPlaceKeyword(savedPlace, savedKeyword1);
            PlaceKeyword placeKeyword2 = PlaceFixture.createPlaceKeyword(savedPlace, savedKeyword2);
            entityManager.persistAndFlush(placeKeyword1);
            entityManager.persistAndFlush(placeKeyword2);

            clearPersistenceContext();

            Optional<Place> result = placeRepository.findByIdWithKeywords(savedPlace.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getKeywords()).hasSize(2);

            List<String> keywords = result.get().getKeywords().stream()
                    .map(pk -> pk.getKeyword().getKeyword())
                    .toList();
            assertThat(keywords).containsExactlyInAnyOrder(TestConstants.COZY_KEYWORD, TestConstants.DELICIOUS_KEYWORD);
        }

        @Test
        @DisplayName("키워드가 없는 장소도 정상 조회된다")
        void findByIdWithKeywords_WithNoKeywords_ReturnsPlace() {
            Place place = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place savedPlace = entityManager.persistAndFlush(place);
            clearPersistenceContext();

            Optional<Place> result = placeRepository.findByIdWithKeywords(savedPlace.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getKeywords()).isEmpty();
        }

        @Test
        @DisplayName("메뉴를 포함한 장소 정보 조회가 정상 동작한다")
        void findByIdWithMenus_ReturnsPlaceWithMenus() {
            Place place = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);

            Place savedPlace = entityManager.persistAndFlush(place);

            PlaceMenu menu1 = PlaceFixture.createPlaceMenu(savedPlace, TestConstants.AMERICANO_MENU, TestConstants.AMERICANO_PRICE);
            PlaceMenu menu2 = PlaceFixture.createPlaceMenu(savedPlace, TestConstants.LATTE_MENU, TestConstants.LATTE_PRICE);
            entityManager.persistAndFlush(menu1);
            entityManager.persistAndFlush(menu2);

            clearPersistenceContext();

            Optional<Place> result = placeRepository.findByIdWithMenus(savedPlace.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getMenus()).hasSize(2);

            List<String> menuNames = result.get().getMenus().stream()
                    .map(menu -> menu.getMenuName())
                    .toList();
            assertThat(menuNames).containsExactlyInAnyOrder(TestConstants.AMERICANO_MENU, TestConstants.LATTE_MENU);
        }

        @Test
        @DisplayName("영업시간을 포함한 장소 정보 조회가 정상 동작한다")
        void findByIdWithHours_ReturnsPlaceWithHours() {
            Place place = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);

            Place savedPlace = entityManager.persistAndFlush(place);

            PlaceHours hours1 = PlaceFixture.createPlaceHours(savedPlace, TestConstants.MONDAY, TestConstants.OPEN_TIME, TestConstants.CLOSE_TIME);
            PlaceHours hours2 = PlaceFixture.createPlaceHours(savedPlace, TestConstants.TUESDAY, TestConstants.OPEN_TIME, TestConstants.CLOSE_TIME);
            entityManager.persistAndFlush(hours1);
            entityManager.persistAndFlush(hours2);

            clearPersistenceContext();

            Optional<Place> result = placeRepository.findByIdWithHours(savedPlace.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getHours()).hasSize(2);

            List<String> days = result.get().getHours().stream()
                    .map(hours -> hours.getDayOfWeek())
                    .toList();
            assertThat(days).containsExactlyInAnyOrder(TestConstants.MONDAY, TestConstants.TUESDAY);
        }

        @Test
        @DisplayName("여러 ID로 키워드 포함 장소들 조회가 정상 동작한다")
        void findByIdsWithKeywords_ReturnsPlacesWithKeywords() {
            Place place1 = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME + "1", TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place place2 = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME + "2", TestConstants.CAFE_CATEGORY,
                    TestConstants.NEAR_LAT, TestConstants.NEAR_LNG);
            Keyword keyword = PlaceFixture.createKeyword(TestConstants.GOOD_KEYWORD);

            Place savedPlace1 = entityManager.persistAndFlush(place1);
            Place savedPlace2 = entityManager.persistAndFlush(place2);
            Keyword savedKeyword = entityManager.persistAndFlush(keyword);

            PlaceKeyword placeKeyword = PlaceFixture.createPlaceKeyword(savedPlace1, savedKeyword);
            entityManager.persistAndFlush(placeKeyword);

            clearPersistenceContext();

            List<Place> results = placeRepository.findByIdsWithKeywords(List.of(savedPlace1.getId(), savedPlace2.getId()));

            assertThat(results).hasSize(2);
            assertThat(results.stream().map(Place::getName))
                    .containsExactlyInAnyOrder(TestConstants.TEST_CAFE_NAME + "1", TestConstants.TEST_CAFE_NAME + "2");
        }
    }

    @Nested
    @DisplayName("공간 쿼리 테스트")
    class SpatialQueryTest {

        @Test
        @DisplayName("반경 내 특정 장소들 검색이 정상 동작한다")
        void findPlacesWithinRadiusByIds_ReturnsNearbyPlaces() {
            Place nearPlace = PlaceFixture.createPlace("가까운 " + TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place farPlace = PlaceFixture.createPlace("먼 " + TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.FAR_LAT, TestConstants.FAR_LNG);

            Place savedNearPlace = entityManager.persistAndFlush(nearPlace);
            Place savedFarPlace = entityManager.persistAndFlush(farPlace);
            clearPersistenceContext();

            List<Long> placeIds = List.of(savedNearPlace.getId(), savedFarPlace.getId());

            List<PlaceWithDistance> results = placeRepository.findPlacesWithinRadiusByIds(
                    placeIds, TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.SMALL_RADIUS);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("가까운 " + TestConstants.TEST_CAFE_NAME);
            assertThat(results.get(0).getDistance()).isLessThan(TestConstants.SMALL_RADIUS);
        }

        @Test
        @DisplayName("반경이 매우 클 때 모든 장소가 조회된다")
        void findPlacesWithinRadiusByIds_WithLargeRadius_ReturnsAllPlaces() {
            Place place1 = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME + "1", TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place place2 = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME + "2", TestConstants.CAFE_CATEGORY,
                    TestConstants.FAR_LAT, TestConstants.FAR_LNG);

            Place savedPlace1 = entityManager.persistAndFlush(place1);
            Place savedPlace2 = entityManager.persistAndFlush(place2);
            clearPersistenceContext();

            List<Long> placeIds = List.of(savedPlace1.getId(), savedPlace2.getId());

            List<PlaceWithDistance> results = placeRepository.findPlacesWithinRadiusByIds(
                    placeIds, TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.LARGE_RADIUS);

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("ID 목록으로 검색 시 결과가 거리순으로 정렬된다")
        void findPlacesWithinRadiusByIds_ResultsOrderedByDistance() {
            String nearCafeName = "가까운 " + TestConstants.TEST_CAFE_NAME;
            String farCafeName = "먼 " + TestConstants.TEST_CAFE_NAME;

            Place nearPlace = PlaceFixture.createPlace(nearCafeName, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place farPlace = PlaceFixture.createPlace(farCafeName, TestConstants.CAFE_CATEGORY,
                    TestConstants.SORT_TEST_FAR_LAT, TestConstants.SORT_TEST_FAR_LNG);

            Place savedNearPlace = entityManager.persistAndFlush(nearPlace);
            Place savedFarPlace = entityManager.persistAndFlush(farPlace);
            clearPersistenceContext();

            List<Long> placeIds = List.of(savedNearPlace.getId(), savedFarPlace.getId());

            List<PlaceWithDistance> results = placeRepository.findPlacesWithinRadiusByIds(
                    placeIds, TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.LARGE_RADIUS);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getName()).isEqualTo(nearCafeName);
            assertThat(results.get(1).getName()).isEqualTo(farCafeName);
            assertThat(results.get(0).getDistance()).isLessThan(results.get(1).getDistance());
        }

        @Test
        @DisplayName("카테고리별 반경 내 장소 검색이 정상 동작한다")
        void findPlacesByCategoryWithinRadius_ReturnsPlacesByCategory() {
            Place cafe = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place restaurant = PlaceFixture.createPlace(TestConstants.TEST_RESTAURANT_NAME, TestConstants.RESTAURANT_CATEGORY,
                    TestConstants.NEAR_LAT, TestConstants.NEAR_LNG);

            entityManager.persistAndFlush(cafe);
            entityManager.persistAndFlush(restaurant);
            clearPersistenceContext();

            List<PlaceWithDistance> results = placeRepository.findPlacesByCategoryWithinRadius(
                    TestConstants.CAFE_CATEGORY, TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.SMALL_RADIUS);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo(TestConstants.TEST_CAFE_NAME);
            assertThat(results.get(0).getCategory()).isEqualTo(TestConstants.CAFE_CATEGORY);
        }

        @Test
        @DisplayName("카테고리별 검색 결과가 거리순으로 정렬된다")
        void findPlacesByCategoryWithinRadius_ResultsOrderedByDistance() {
            String nearCafeName = "가까운 " + TestConstants.TEST_CAFE_NAME;
            String farCafeName = "먼 " + TestConstants.TEST_CAFE_NAME;

            Place nearCafe = PlaceFixture.createPlace(nearCafeName, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place farCafe = PlaceFixture.createPlace(farCafeName, TestConstants.CAFE_CATEGORY,
                    TestConstants.SORT_TEST_FAR_LAT, TestConstants.SORT_TEST_FAR_LNG);

            entityManager.persistAndFlush(nearCafe);
            entityManager.persistAndFlush(farCafe);
            clearPersistenceContext();

            List<PlaceWithDistance> results = placeRepository.findPlacesByCategoryWithinRadius(
                    TestConstants.CAFE_CATEGORY, TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.LARGE_RADIUS);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getName()).isEqualTo(nearCafeName);
            assertThat(results.get(1).getName()).isEqualTo(farCafeName);
            assertThat(results.get(0).getDistance()).isLessThan(results.get(1).getDistance());
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 검색 시 빈 결과를 반환한다")
        void findPlacesByCategoryWithinRadius_WithNonExistentCategory_ReturnsEmpty() {
            Place cafe = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);

            entityManager.persistAndFlush(cafe);
            clearPersistenceContext();

            List<PlaceWithDistance> results = placeRepository.findPlacesByCategoryWithinRadius(
                    TestConstants.NON_EXISTENT_CATEGORY, TestConstants.CENTER_LAT, TestConstants.CENTER_LNG, TestConstants.SMALL_RADIUS);

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("카테고리 조회 테스트")
    class CategoryQueryTest {

        @Test
        @DisplayName("카테고리 목록 조회가 정상 동작한다")
        void findDistinctCategories_ReturnsCategories() {
            Place cafe = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME, TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place restaurant = PlaceFixture.createPlace(TestConstants.TEST_RESTAURANT_NAME, TestConstants.RESTAURANT_CATEGORY,
                    TestConstants.NEAR_LAT, TestConstants.NEAR_LNG);
            Place bar = PlaceFixture.createPlace(TestConstants.TEST_BAR_NAME, TestConstants.BAR_CATEGORY,
                    TestConstants.BAR_LAT, TestConstants.BAR_LNG);

            entityManager.persistAndFlush(cafe);
            entityManager.persistAndFlush(restaurant);
            entityManager.persistAndFlush(bar);
            clearPersistenceContext();

            List<String> categories = placeRepository.findDistinctCategories();

            assertThat(categories).containsExactlyInAnyOrder(TestConstants.CAFE_CATEGORY, TestConstants.RESTAURANT_CATEGORY, TestConstants.BAR_CATEGORY);
        }

        @Test
        @DisplayName("카테고리가 COUNT 기준으로 내림차순 정렬되어 조회된다")
        void findDistinctCategories_OrderedByCountDescending() {
            Place cafe1 = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME + "1", TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place cafe2 = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME + "2", TestConstants.CAFE_CATEGORY,
                    TestConstants.NEAR_LAT, TestConstants.NEAR_LNG);
            Place cafe3 = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME + "3", TestConstants.CAFE_CATEGORY,
                    TestConstants.BAR_LAT, TestConstants.BAR_LNG);

            Place restaurant1 = PlaceFixture.createPlace(TestConstants.TEST_RESTAURANT_NAME + "1", TestConstants.RESTAURANT_CATEGORY,
                    TestConstants.RESTAURANT1_LAT, TestConstants.RESTAURANT1_LNG);
            Place restaurant2 = PlaceFixture.createPlace(TestConstants.TEST_RESTAURANT_NAME + "2", TestConstants.RESTAURANT_CATEGORY,
                    TestConstants.RESTAURANT2_LAT, TestConstants.RESTAURANT2_LNG);

            Place bar1 = PlaceFixture.createPlace(TestConstants.TEST_BAR_NAME + "1", TestConstants.BAR_CATEGORY,
                    TestConstants.SORT_TEST_FAR_LAT, TestConstants.SORT_TEST_FAR_LNG);

            entityManager.persistAndFlush(cafe1);
            entityManager.persistAndFlush(cafe2);
            entityManager.persistAndFlush(cafe3);
            entityManager.persistAndFlush(restaurant1);
            entityManager.persistAndFlush(restaurant2);
            entityManager.persistAndFlush(bar1);
            clearPersistenceContext();

            List<String> categories = placeRepository.findDistinctCategories();

            assertThat(categories).hasSize(3);
            assertThat(categories.get(0)).isEqualTo(TestConstants.CAFE_CATEGORY);
            assertThat(categories.get(1)).isEqualTo(TestConstants.RESTAURANT_CATEGORY);
            assertThat(categories.get(2)).isEqualTo(TestConstants.BAR_CATEGORY);
        }

        @Test
        @DisplayName("동일한 개수의 카테고리는 가나다순으로 정렬된다")
        void findDistinctCategories_WithSameCount_OrderedAlphabetically() {
            Place cafe1 = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME + "1", TestConstants.CAFE_CATEGORY,
                    TestConstants.CENTER_LAT, TestConstants.CENTER_LNG);
            Place cafe2 = PlaceFixture.createPlace(TestConstants.TEST_CAFE_NAME + "2", TestConstants.CAFE_CATEGORY,
                    TestConstants.NEAR_LAT, TestConstants.NEAR_LNG);

            Place restaurant1 = PlaceFixture.createPlace(TestConstants.TEST_RESTAURANT_NAME + "1", TestConstants.RESTAURANT_CATEGORY,
                    TestConstants.RESTAURANT1_LAT, TestConstants.RESTAURANT1_LNG);
            Place restaurant2 = PlaceFixture.createPlace(TestConstants.TEST_RESTAURANT_NAME + "2", TestConstants.RESTAURANT_CATEGORY,
                    TestConstants.RESTAURANT2_LAT, TestConstants.RESTAURANT2_LNG);

            entityManager.persistAndFlush(cafe1);
            entityManager.persistAndFlush(cafe2);
            entityManager.persistAndFlush(restaurant1);
            entityManager.persistAndFlush(restaurant2);
            clearPersistenceContext();

            List<String> categories = placeRepository.findDistinctCategories();

            assertThat(categories).hasSize(2);
            assertThat(categories.get(0)).isEqualTo(TestConstants.RESTAURANT_CATEGORY);
            assertThat(categories.get(1)).isEqualTo(TestConstants.CAFE_CATEGORY);
        }

        @Test
        @DisplayName("장소가 없으면 빈 카테고리 목록을 반환한다")
        void findDistinctCategories_WithNoPlaces_ReturnsEmpty() {
            clearPersistenceContext();

            List<String> categories = placeRepository.findDistinctCategories();

            assertThat(categories).isEmpty();
        }
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
