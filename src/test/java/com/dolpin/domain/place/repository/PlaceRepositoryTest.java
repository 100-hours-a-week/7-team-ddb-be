package com.dolpin.domain.place.repository;

import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Keyword;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.global.config.TestConfig;
import com.dolpin.global.fixture.PlaceFixture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
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

    // PostGIS가 포함된 이미지 사용 (PostgreSQL 호환성 명시)
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
            // given
            Long placeId = PlaceFixture.createPlaceWithCategory(entityManager, "테스트 카페", "카페", 37.5665, 126.9780);

            // when
            Optional<Place> result = placeRepository.findBasicPlaceById(placeId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("테스트 카페");
            assertThat(result.get().getCategory()).isEqualTo("카페");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 빈 결과를 반환한다")
        void findBasicPlaceById_WithNonExistentId_ReturnsEmpty() {
            // given
            Long nonExistentId = 999L;

            // when
            Optional<Place> result = placeRepository.findBasicPlaceById(nonExistentId);

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
            Long placeId = PlaceFixture.createPlaceWithCategory(entityManager, "테스트 카페", "카페", 37.5665, 126.9780);
            Keyword keyword1 = PlaceFixture.createAndPersistKeyword(entityManager, "아늑한");
            Keyword keyword2 = PlaceFixture.createAndPersistKeyword(entityManager, "맛있는");

            PlaceFixture.createAndPersistPlaceKeyword(entityManager, placeId, keyword1.getId());
            PlaceFixture.createAndPersistPlaceKeyword(entityManager, placeId, keyword2.getId());

            entityManager.flush();
            entityManager.clear();

            // when
            Optional<Place> result = placeRepository.findByIdWithKeywords(placeId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getKeywords()).hasSize(2);

            List<String> keywords = result.get().getKeywords().stream()
                    .map(pk -> pk.getKeyword().getKeyword())
                    .toList();
            assertThat(keywords).containsExactlyInAnyOrder("아늑한", "맛있는");
        }

        @Test
        @DisplayName("키워드가 없는 장소도 정상 조회된다")
        void findByIdWithKeywords_WithNoKeywords_ReturnsPlace() {
            // given
            Long placeId = PlaceFixture.createPlaceWithCategory(entityManager, "키워드 없는 카페", "카페", 37.5665, 126.9780);

            // when
            Optional<Place> result = placeRepository.findByIdWithKeywords(placeId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getKeywords()).isEmpty();
        }

        @Test
        @DisplayName("메뉴를 포함한 장소 정보 조회가 정상 동작한다")
        void findByIdWithMenus_ReturnsPlaceWithMenus() {
            // given
            Long placeId = PlaceFixture.createPlaceWithCategory(entityManager, "테스트 카페", "카페", 37.5665, 126.9780);
            PlaceFixture.createAndPersistPlaceMenu(entityManager, placeId, "아메리카노", 4000);
            PlaceFixture.createAndPersistPlaceMenu(entityManager, placeId, "라떼", 4500);

            entityManager.flush();
            entityManager.clear();

            // when
            Optional<Place> result = placeRepository.findByIdWithMenus(placeId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getMenus()).hasSize(2);

            List<String> menuNames = result.get().getMenus().stream()
                    .map(menu -> menu.getMenuName())
                    .toList();
            assertThat(menuNames).containsExactlyInAnyOrder("아메리카노", "라떼");
        }

        @Test
        @DisplayName("영업시간을 포함한 장소 정보 조회가 정상 동작한다")
        void findByIdWithHours_ReturnsPlaceWithHours() {
            // given
            Long placeId = PlaceFixture.createPlaceWithCategory(entityManager, "테스트 카페", "카페", 37.5665, 126.9780);
            PlaceFixture.createAndPersistPlaceHours(entityManager, placeId, "월", "09:00", "21:00");
            PlaceFixture.createAndPersistPlaceHours(entityManager, placeId, "화", "09:00", "21:00");

            entityManager.flush();
            entityManager.clear();

            // when
            Optional<Place> result = placeRepository.findByIdWithHours(placeId);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getHours()).hasSize(2);

            List<String> days = result.get().getHours().stream()
                    .map(hours -> hours.getDayOfWeek())
                    .toList();
            assertThat(days).containsExactlyInAnyOrder("월", "화");
        }

        @Test
        @DisplayName("여러 ID로 키워드 포함 장소들 조회가 정상 동작한다")
        void findByIdsWithKeywords_ReturnsPlacesWithKeywords() {
            // given
            Long place1Id = PlaceFixture.createPlaceWithCategory(entityManager, "카페1", "카페", 37.5665, 126.9780);
            Long place2Id = PlaceFixture.createPlaceWithCategory(entityManager, "카페2", "카페", 37.5666, 126.9781);

            Keyword keyword = PlaceFixture.createAndPersistKeyword(entityManager, "좋은");
            PlaceFixture.createAndPersistPlaceKeyword(entityManager, place1Id, keyword.getId());

            entityManager.flush();
            entityManager.clear();

            // when
            List<Place> results = placeRepository.findByIdsWithKeywords(List.of(place1Id, place2Id));

            // then
            assertThat(results).hasSize(2);
            assertThat(results.stream().map(Place::getName)).containsExactlyInAnyOrder("카페1", "카페2");
        }
    }

    @Nested
    @DisplayName("공간 쿼리 테스트")
    class SpatialQueryTest {

        @Test
        @DisplayName("반경 내 특정 장소들 검색이 정상 동작한다")
        void findPlacesWithinRadiusByIds_ReturnsNearbyPlaces() {
            // given
            Long nearPlaceId = PlaceFixture.createPlaceWithCategory(entityManager, "가까운 카페", "카페", 37.5665, 126.9780);
            Long farPlaceId = PlaceFixture.createPlaceWithCategory(entityManager, "먼 카페", "카페", 37.6665, 127.0780);

            entityManager.flush();
            entityManager.clear();

            List<Long> placeIds = List.of(nearPlaceId, farPlaceId);
            Double centerLat = 37.5665;
            Double centerLng = 126.9780;
            Double radius = 1000.0; // 1km

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesWithinRadiusByIds(
                    placeIds, centerLat, centerLng, radius);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("가까운 카페");
            assertThat(results.get(0).getDistance()).isLessThan(radius);
        }

        @Test
        @DisplayName("반경이 매우 클 때 모든 장소가 조회된다")
        void findPlacesWithinRadiusByIds_WithLargeRadius_ReturnsAllPlaces() {
            // given
            Long place1Id = PlaceFixture.createPlaceWithCategory(entityManager, "카페1", "카페", 37.5665, 126.9780);
            Long place2Id = PlaceFixture.createPlaceWithCategory(entityManager, "카페2", "카페", 37.6665, 127.0780);

            entityManager.flush();
            entityManager.clear();

            List<Long> placeIds = List.of(place1Id, place2Id);
            Double centerLat = 37.5665;
            Double centerLng = 126.9780;
            Double radius = 50000.0; // 50km

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesWithinRadiusByIds(
                    placeIds, centerLat, centerLng, radius);

            // then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("카테고리별 반경 내 장소 검색이 정상 동작한다")
        void findPlacesByCategoryWithinRadius_ReturnsPlacesByCategory() {
            // given
            PlaceFixture.createPlaceWithCategory(entityManager, "테스트 카페", "카페", 37.5665, 126.9780);
            PlaceFixture.createPlaceWithCategory(entityManager, "테스트 식당", "식당", 37.5666, 126.9781);

            entityManager.flush();
            entityManager.clear();

            String category = "카페";
            Double centerLat = 37.5665;
            Double centerLng = 126.9780;
            Double radius = 1000.0;

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesByCategoryWithinRadius(
                    category, centerLat, centerLng, radius);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("테스트 카페");
            assertThat(results.get(0).getCategory()).isEqualTo("카페");
        }

        @Test
        @DisplayName("카테고리별 검색 결과가 거리순으로 정렬된다")
        void findPlacesByCategoryWithinRadius_ResultsOrderedByDistance() {
            // given
            PlaceFixture.createPlaceWithCategory(entityManager, "가까운 카페", "카페", 37.5665, 126.9780);
            PlaceFixture.createPlaceWithCategory(entityManager, "먼 카페", "카페", 37.5670, 126.9785);

            entityManager.flush();
            entityManager.clear();

            String category = "카페";
            Double centerLat = 37.5665;
            Double centerLng = 126.9780;
            Double radius = 10000.0; // 충분히 큰 반경

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesByCategoryWithinRadius(
                    category, centerLat, centerLng, radius);

            // then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getName()).isEqualTo("가까운 카페");
            assertThat(results.get(1).getName()).isEqualTo("먼 카페");
            assertThat(results.get(0).getDistance()).isLessThan(results.get(1).getDistance());
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 검색 시 빈 결과를 반환한다")
        void findPlacesByCategoryWithinRadius_WithNonExistentCategory_ReturnsEmpty() {
            // given
            PlaceFixture.createPlaceWithCategory(entityManager, "테스트 카페", "카페", 37.5665, 126.9780);

            entityManager.flush();
            entityManager.clear();

            String nonExistentCategory = "존재하지않는카테고리";
            Double centerLat = 37.5665;
            Double centerLng = 126.9780;
            Double radius = 1000.0;

            // when
            List<PlaceWithDistance> results = placeRepository.findPlacesByCategoryWithinRadius(
                    nonExistentCategory, centerLat, centerLng, radius);

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
            PlaceFixture.createPlaceWithCategory(entityManager, "카페1", "카페", 37.5665, 126.9780);
            PlaceFixture.createPlaceWithCategory(entityManager, "식당1", "식당", 37.5666, 126.9781);
            PlaceFixture.createPlaceWithCategory(entityManager, "술집1", "술집", 37.5667, 126.9782);

            entityManager.flush();
            entityManager.clear();

            // when
            List<String> categories = placeRepository.findDistinctCategories();

            // then
            assertThat(categories).containsExactlyInAnyOrder("카페", "식당", "술집");
        }

        @Test
        @DisplayName("카테고리가 많은 순서대로 정렬되어 조회된다")
        void findDistinctCategories_OrderedByCount() {
            // given - 카페 3개, 식당 2개, 술집 1개
            PlaceFixture.createPlaceWithCategory(entityManager, "카페1", "카페", 37.5665, 126.9780);
            PlaceFixture.createPlaceWithCategory(entityManager, "카페2", "카페", 37.5666, 126.9781);
            PlaceFixture.createPlaceWithCategory(entityManager, "카페3", "카페", 37.5667, 126.9782);
            PlaceFixture.createPlaceWithCategory(entityManager, "식당1", "식당", 37.5668, 126.9783);
            PlaceFixture.createPlaceWithCategory(entityManager, "식당2", "식당", 37.5669, 126.9784);
            PlaceFixture.createPlaceWithCategory(entityManager, "술집1", "술집", 37.5670, 126.9785);

            entityManager.flush();
            entityManager.clear();

            // when
            List<String> categories = placeRepository.findDistinctCategories();

            // then
            assertThat(categories).hasSize(3);
            assertThat(categories.get(0)).isEqualTo("카페"); // 가장 많음
            assertThat(categories.get(1)).isEqualTo("식당"); // 두 번째로 많음
            assertThat(categories.get(2)).isEqualTo("술집"); // 가장 적음
        }

        @Test
        @DisplayName("장소가 없으면 빈 카테고리 목록을 반환한다")
        void findDistinctCategories_WithNoPlaces_ReturnsEmpty() {
            // given - 장소 없음

            // when
            List<String> categories = placeRepository.findDistinctCategories();

            // then
            assertThat(categories).isEmpty();
        }
    }
}