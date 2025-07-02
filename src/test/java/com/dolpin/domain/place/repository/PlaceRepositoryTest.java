package com.dolpin.domain.place.repository;

import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.*;
import com.dolpin.global.config.TestConfig;
import com.dolpin.global.constants.PlaceTestConstants;
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
@Import({TestConfig.class, PlaceTestHelper.class})
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

    @Autowired
    private PlaceTestHelper testHelper;

    @Nested
    @DisplayName("기본 조회 테스트")
    class BasicQueryTest {

        @Test
        @DisplayName("ID로 기본 장소 정보 조회가 정상 동작한다")
        void findBasicPlaceById_ReturnsPlace() {
            // given
            Place place = PlaceFixture.createBasicCafe();
            Place savedPlace = testHelper.savePlace(entityManager, place);
            testHelper.clearPersistenceContext(entityManager);

            // when
            Optional<Place> result = placeRepository.findBasicPlaceById(savedPlace.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(PlaceTestConstants.TEST_CAFE_NAME);
            assertThat(result.get().getCategory()).isEqualTo(PlaceTestConstants.CAFE_CATEGORY);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 빈 결과를 반환한다")
        void findBasicPlaceById_WithNonExistentId_ReturnsEmpty() {
            // given
            testHelper.clearPersistenceContext(entityManager);

            // when
            Optional<Place> result = placeRepository.findBasicPlaceById(PlaceTestConstants.NON_EXISTENT_PLACE_ID);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("연관 관계 조회 테스트")
    class AssociationQueryTest {

        @Test
        @DisplayName("키워드를 포함한 장소 정보 조회가 정상 동작한다")
        void findByIdWithKeywords_ReturnsPlaceWithKeywords() {
            // given
            List<String> expectedKeywords = List.of(PlaceTestConstants.COZY_KEYWORD, PlaceTestConstants.DELICIOUS_KEYWORD);
            Place place = PlaceFixture.createBasicCafe();
            Place savedPlace = testHelper.savePlaceWithKeywords(entityManager, place, expectedKeywords);
            testHelper.clearPersistenceContext(entityManager);

            // when
            Optional<Place> result = placeRepository.findByIdWithKeywords(savedPlace.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getKeywords()).hasSize(expectedKeywords.size());

            List<String> actualKeywords = result.get().getKeywords().stream()
                    .map(pk -> pk.getKeyword().getKeyword())
                    .toList();
            assertThat(actualKeywords).containsExactlyInAnyOrderElementsOf(expectedKeywords);
        }

        @Test
        @DisplayName("키워드가 없는 장소도 정상 조회된다")
        void findByIdWithKeywords_WithNoKeywords_ReturnsPlace() {
            // given
            Place place = PlaceFixture.createBasicCafe();
            Place savedPlace = testHelper.savePlace(entityManager, place);
            testHelper.clearPersistenceContext(entityManager);

            // when
            Optional<Place> result = placeRepository.findByIdWithKeywords(savedPlace.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getKeywords()).isEmpty();
        }

        @Test
        @DisplayName("메뉴를 포함한 장소 정보 조회가 정상 동작한다")
        void findByIdWithMenus_ReturnsPlaceWithMenus() {
            // given
            Place place = PlaceFixture.createBasicCafe();
            List<PlaceMenu> expectedMenus = List.of(
                    PlaceFixture.createPlaceMenu(place, PlaceTestConstants.AMERICANO_MENU, PlaceTestConstants.AMERICANO_PRICE),
                    PlaceFixture.createPlaceMenu(place, PlaceTestConstants.LATTE_MENU, PlaceTestConstants.LATTE_PRICE)
            );
            Place savedPlace = testHelper.savePlaceWithMenus(entityManager, place, expectedMenus);
            testHelper.clearPersistenceContext(entityManager);

            // when
            Optional<Place> result = placeRepository.findByIdWithMenus(savedPlace.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getMenus()).hasSize(PlaceTestConstants.EXPECTED_MENU_COUNT);

            List<String> actualMenuNames = result.get().getMenus().stream()
                    .map(PlaceMenu::getMenuName)
                    .toList();
            assertThat(actualMenuNames).containsExactlyInAnyOrder(
                    PlaceTestConstants.AMERICANO_MENU, PlaceTestConstants.LATTE_MENU);
        }

        @Test
        @DisplayName("영업시간을 포함한 장소 정보 조회가 정상 동작한다")
        void findByIdWithHours_ReturnsPlaceWithHours() {
            // given
            Place place = PlaceFixture.createBasicCafe();
            List<PlaceHours> expectedHours = List.of(
                    PlaceFixture.createMondayHours(place),
                    PlaceFixture.createTuesdayHours(place)
            );
            Place savedPlace = testHelper.savePlaceWithHours(entityManager, place, expectedHours);
            testHelper.clearPersistenceContext(entityManager);

            // when
            Optional<Place> result = placeRepository.findByIdWithHours(savedPlace.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getHours()).hasSize(2);

            List<String> actualDays = result.get().getHours().stream()
                    .map(PlaceHours::getDayOfWeek)
                    .toList();
            assertThat(actualDays).containsExactlyInAnyOrder(PlaceTestConstants.MONDAY, PlaceTestConstants.TUESDAY);
        }

        @Test
        @DisplayName("여러 ID로 키워드 포함 장소들 조회가 정상 동작한다")
        void findByIdsWithKeywords_ReturnsPlacesWithKeywords() {
            // given
            Place place1 = PlaceFixture.createCafe(PlaceTestConstants.TEST_CAFE_NAME + "1",
                    PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG);
            Place place2 = PlaceFixture.createCafe(PlaceTestConstants.TEST_CAFE_NAME + "2",
                    PlaceTestConstants.NEAR_LAT, PlaceTestConstants.NEAR_LNG);

            Place savedPlace1 = testHelper.savePlaceWithKeywords(entityManager, place1,
                    List.of(PlaceTestConstants.GOOD_KEYWORD));
            Place savedPlace2 = testHelper.savePlace(entityManager, place2);
            testHelper.clearPersistenceContext(entityManager);

            // when
            List<Place> results = placeRepository.findByIdsWithKeywords(
                    List.of(savedPlace1.getId(), savedPlace2.getId()));

            // then
            assertThat(results).hasSize(2);
            assertThat(results.stream().map(Place::getName))
                    .containsExactlyInAnyOrder(PlaceTestConstants.TEST_CAFE_NAME + "1", PlaceTestConstants.TEST_CAFE_NAME + "2");
        }
    }

    @Nested
    @DisplayName("공간 쿼리 테스트")
    class SpatialQueryTest {

        @Test
        @DisplayName("반경 내 특정 장소들 검색이 정상 동작한다")
        void findPlacesWithinRadiusByIds_ReturnsNearbyPlaces() {
            // given
            Place nearPlace = PlaceFixture.createNearbyCafe();
            Place farPlace = PlaceFixture.createFarCafe();

            Place savedNearPlace = testHelper.savePlace(entityManager, nearPlace);
            Place savedFarPlace = testHelper.savePlace(entityManager, farPlace);
            testHelper.clearPersistenceContext(entityManager);

            List<Long> placeIds = List.of(savedNearPlace.getId(), savedFarPlace.getId());

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesWithinRadiusByIds(
                    placeIds, PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG, PlaceTestConstants.SMALL_RADIUS);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo(PlaceTestConstants.NEARBY_PREFIX + PlaceTestConstants.TEST_CAFE_NAME);
            assertThat(results.get(0).getDistance()).isLessThan(PlaceTestConstants.SMALL_RADIUS);
        }

        @Test
        @DisplayName("반경이 매우 클 때 모든 장소가 조회된다")
        void findPlacesWithinRadiusByIds_WithLargeRadius_ReturnsAllPlaces() {
            // given
            Place place1 = PlaceFixture.createCafe(PlaceTestConstants.TEST_CAFE_NAME + "1",
                    PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG);
            Place place2 = PlaceFixture.createCafe(PlaceTestConstants.TEST_CAFE_NAME + "2",
                    PlaceTestConstants.FAR_LAT, PlaceTestConstants.FAR_LNG);

            Place savedPlace1 = testHelper.savePlace(entityManager, place1);
            Place savedPlace2 = testHelper.savePlace(entityManager, place2);
            testHelper.clearPersistenceContext(entityManager);

            List<Long> placeIds = List.of(savedPlace1.getId(), savedPlace2.getId());

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesWithinRadiusByIds(
                    placeIds, PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG, PlaceTestConstants.LARGE_RADIUS);

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("ID 목록으로 검색 시 결과가 거리순으로 정렬된다")
        void findPlacesWithinRadiusByIds_ResultsOrderedByDistance() {
            // given
            String nearCafeName = PlaceTestConstants.NEARBY_PREFIX + PlaceTestConstants.TEST_CAFE_NAME;
            String farCafeName = PlaceTestConstants.FAR_PREFIX + PlaceTestConstants.TEST_CAFE_NAME;

            Place nearPlace = PlaceFixture.createCafe(nearCafeName,
                    PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG);
            Place farPlace = PlaceFixture.createCafe(farCafeName,
                    PlaceTestConstants.SORT_TEST_FAR_LAT, PlaceTestConstants.SORT_TEST_FAR_LNG);

            Place savedNearPlace = testHelper.savePlace(entityManager, nearPlace);
            Place savedFarPlace = testHelper.savePlace(entityManager, farPlace);
            testHelper.clearPersistenceContext(entityManager);

            List<Long> placeIds = List.of(savedNearPlace.getId(), savedFarPlace.getId());

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesWithinRadiusByIds(
                    placeIds, PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG, PlaceTestConstants.LARGE_RADIUS);

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getName()).isEqualTo(nearCafeName);
            assertThat(results.get(1).getName()).isEqualTo(farCafeName);
            assertThat(results.get(0).getDistance()).isLessThan(results.get(1).getDistance());
        }

        @Test
        @DisplayName("카테고리별 반경 내 장소 검색이 정상 동작한다")
        void findPlacesByCategoryWithinRadius_ReturnsPlacesByCategory() {
            // given
            Place cafe = PlaceFixture.createBasicCafe();
            Place restaurant = PlaceFixture.createBasicRestaurant();

            testHelper.savePlace(entityManager, cafe);
            testHelper.savePlace(entityManager, restaurant);
            testHelper.clearPersistenceContext(entityManager);

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesByCategoryWithinRadius(
                    PlaceTestConstants.CAFE_CATEGORY, PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG, PlaceTestConstants.SMALL_RADIUS);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo(PlaceTestConstants.TEST_CAFE_NAME);
            assertThat(results.get(0).getCategory()).isEqualTo(PlaceTestConstants.CAFE_CATEGORY);
        }

        @Test
        @DisplayName("카테고리별 검색 결과가 거리순으로 정렬된다")
        void findPlacesByCategoryWithinRadius_ResultsOrderedByDistance() {
            // given
            String nearCafeName = PlaceTestConstants.NEARBY_PREFIX + PlaceTestConstants.TEST_CAFE_NAME;
            String farCafeName = PlaceTestConstants.FAR_PREFIX + PlaceTestConstants.TEST_CAFE_NAME;

            Place nearCafe = PlaceFixture.createCafe(nearCafeName,
                    PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG);
            Place farCafe = PlaceFixture.createCafe(farCafeName,
                    PlaceTestConstants.SORT_TEST_FAR_LAT, PlaceTestConstants.SORT_TEST_FAR_LNG);

            testHelper.savePlace(entityManager, nearCafe);
            testHelper.savePlace(entityManager, farCafe);
            testHelper.clearPersistenceContext(entityManager);

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesByCategoryWithinRadius(
                    PlaceTestConstants.CAFE_CATEGORY, PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG, PlaceTestConstants.LARGE_RADIUS);

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getName()).isEqualTo(nearCafeName);
            assertThat(results.get(1).getName()).isEqualTo(farCafeName);
            assertThat(results.get(0).getDistance()).isLessThan(results.get(1).getDistance());
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 검색 시 빈 결과를 반환한다")
        void findPlacesByCategoryWithinRadius_WithNonExistentCategory_ReturnsEmpty() {
            // given
            Place cafe = PlaceFixture.createBasicCafe();
            testHelper.savePlace(entityManager, cafe);
            testHelper.clearPersistenceContext(entityManager);

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesByCategoryWithinRadius(
                    PlaceTestConstants.NON_EXISTENT_CATEGORY, PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG, PlaceTestConstants.SMALL_RADIUS);

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("카테고리 조회 테스트")
    class CategoryQueryTest {

        @Test
        @DisplayName("카테고리 목록 조회가 정상 동작한다")
        void findDistinctCategories_ReturnsCategories() {
            // given
            Place cafe = PlaceFixture.createBasicCafe();
            Place restaurant = PlaceFixture.createBasicRestaurant();
            Place bar = PlaceFixture.createBasicBar();

            testHelper.savePlace(entityManager, cafe);
            testHelper.savePlace(entityManager, restaurant);
            testHelper.savePlace(entityManager, bar);
            testHelper.clearPersistenceContext(entityManager);

            // when
            List<String> categories = placeRepository.findDistinctCategories();

            // then
            assertThat(categories).containsExactlyInAnyOrder(
                    PlaceTestConstants.CAFE_CATEGORY,
                    PlaceTestConstants.RESTAURANT_CATEGORY,
                    PlaceTestConstants.BAR_CATEGORY);
        }

        @Test
        @DisplayName("카테고리가 COUNT 기준으로 내림차순 정렬되어 조회된다")
        void findDistinctCategories_OrderedByCountDescending() {
            // given
            testHelper.savePlacesForCategoryTest(entityManager);
            testHelper.clearPersistenceContext(entityManager);

            // when
            List<String> categories = placeRepository.findDistinctCategories();

            // then
            assertThat(categories).hasSize(3);
            assertThat(categories.get(0)).isEqualTo(PlaceTestConstants.CAFE_CATEGORY);  // 3개
            assertThat(categories.get(1)).isEqualTo(PlaceTestConstants.RESTAURANT_CATEGORY);  // 2개
            assertThat(categories.get(2)).isEqualTo(PlaceTestConstants.BAR_CATEGORY);  // 1개
        }

        @Test
        @DisplayName("장소가 없으면 빈 카테고리 목록을 반환한다")
        void findDistinctCategories_WithNoPlaces_ReturnsEmpty() {
            // given
            testHelper.clearPersistenceContext(entityManager);

            // when
            List<String> categories = placeRepository.findDistinctCategories();

            // then
            assertThat(categories).isEmpty();
        }
    }

    @Nested
    @DisplayName("이름 검색 테스트")
    class NameSearchTest {

        @Test
        @DisplayName("이름으로 장소 ID 검색이 정상 동작한다")
        void findPlaceIdsByNameContaining_ReturnsMatchingIds() {
            // given
            Place cafe1 = PlaceFixture.createCafe(PlaceTestConstants.TEST_CAFE_NAME,
                    PlaceTestConstants.CENTER_LAT, PlaceTestConstants.CENTER_LNG);
            Place cafe2 = PlaceFixture.createCafe(PlaceTestConstants.ORDINARY_CAFE_NAME,
                    PlaceTestConstants.NEAR_LAT, PlaceTestConstants.NEAR_LNG);
            Place restaurant = PlaceFixture.createBasicRestaurant();

            testHelper.savePlace(entityManager, cafe1);
            testHelper.savePlace(entityManager, cafe2);
            testHelper.savePlace(entityManager, restaurant);
            testHelper.clearPersistenceContext(entityManager);

            // when
            List<Long> results = placeRepository.findPlaceIdsByNameContaining("카페");

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("일치하는 이름이 없으면 빈 결과를 반환한다")
        void findPlaceIdsByNameContaining_WithNoMatches_ReturnsEmpty() {
            // given
            Place restaurant = PlaceFixture.createBasicRestaurant();
            testHelper.savePlace(entityManager, restaurant);
            testHelper.clearPersistenceContext(entityManager);

            // when
            List<Long> results = placeRepository.findPlaceIdsByNameContaining("존재하지않는이름");

            // then
            assertThat(results).isEmpty();
        }
    }
}
