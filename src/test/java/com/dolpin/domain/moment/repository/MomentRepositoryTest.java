package com.dolpin.domain.moment.repository;

import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.entity.MomentImage;
import com.dolpin.global.constants.MomentTestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@DisplayName("MomentRepository 테스트")
class MomentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("dolpin_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.properties.hibernate.spatial.enabled", () -> "true");
    }

    @Autowired
    private MomentRepository momentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Moment testMoment;
    private Moment otherUserMoment;
    private Moment privateMoment;

    @BeforeEach
    void setUp() {
        // 테스트용 공개 Moment 생성
        testMoment = createMoment(
                MomentTestConstants.TEST_USER_ID,
                MomentTestConstants.TEST_MOMENT_TITLE,
                MomentTestConstants.TEST_MOMENT_CONTENT,
                MomentTestConstants.TEST_PLACE_ID,
                MomentTestConstants.TEST_PLACE_NAME,
                MomentTestConstants.DEFAULT_IS_PUBLIC
        );

        // 다른 사용자의 공개 Moment 생성
        otherUserMoment = createMoment(
                MomentTestConstants.OTHER_USER_ID,
                "다른 사용자의 제목",
                "다른 사용자의 내용",
                MomentTestConstants.TEST_PLACE_ID,
                MomentTestConstants.TEST_PLACE_NAME,
                MomentTestConstants.DEFAULT_IS_PUBLIC
        );

        // 비공개 Moment 생성
        privateMoment = createMoment(
                MomentTestConstants.TEST_USER_ID,
                "비공개 제목",
                "비공개 내용",
                MomentTestConstants.TEST_PLACE_ID,
                MomentTestConstants.TEST_PLACE_NAME,
                false
        );

        entityManager.persistAndFlush(testMoment);
        entityManager.persistAndFlush(otherUserMoment);
        entityManager.persistAndFlush(privateMoment);
        entityManager.clear();
    }

    @Nested
    @DisplayName("기본 조회 테스트")
    class BasicQueryTest {

        @Test
        @DisplayName("ID로 기본 Moment 정보만 조회")
        void findBasicMomentById() {
            // when
            Optional<Moment> found = momentRepository.findBasicMomentById(testMoment.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(testMoment.getId());
            assertThat(found.get().getTitle()).isEqualTo(MomentTestConstants.TEST_MOMENT_TITLE);
            assertThat(found.get().getContent()).isEqualTo(MomentTestConstants.TEST_MOMENT_CONTENT);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional 반환")
        void findBasicMomentById_NotFound() {
            // when
            Optional<Moment> found = momentRepository.findBasicMomentById(999L);

            // then
            assertThat(found).isNotPresent();
        }

        @Test
        @DisplayName("ID로 Moment와 이미지 정보 함께 조회")
        void findByIdWithImages() {
            // given
            MomentImage image1 = createMomentImage(testMoment, MomentTestConstants.TEST_IMAGE_1, 0);
            MomentImage image2 = createMomentImage(testMoment, MomentTestConstants.TEST_IMAGE_2, 1);
            entityManager.persist(image1);
            entityManager.persist(image2);
            entityManager.flush();
            entityManager.clear();

            // when
            Optional<Moment> found = momentRepository.findByIdWithImages(testMoment.getId());

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getImages()).hasSize(MomentTestConstants.TEST_IMAGES_COUNT);
            assertThat(found.get().getImages().get(0).getImageUrl()).isEqualTo(MomentTestConstants.TEST_IMAGE_1);
            assertThat(found.get().getImages().get(1).getImageUrl()).isEqualTo(MomentTestConstants.TEST_IMAGE_2);
        }
    }

    @Nested
    @DisplayName("네이티브 쿼리 조회 테스트")
    class NativeQueryTest {

        @Test
        @DisplayName("공개 Moment와 본인 비공개 Moment 조회 - 커서 없음")
        void findPublicMomentsWithUserPrivateNative_NoCursor() {
            // when
            List<Moment> moments = momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, null, MomentTestConstants.DEFAULT_PAGE_LIMIT);

            // then
            assertThat(moments).hasSize(3); // 공개 2개 + 비공개 1개
            assertThat(moments).extracting(Moment::getUserId)
                    .containsExactlyInAnyOrder(
                            MomentTestConstants.TEST_USER_ID,
                            MomentTestConstants.OTHER_USER_ID,
                            MomentTestConstants.TEST_USER_ID
                    );
        }

        @Test
        @DisplayName("공개 Moment와 본인 비공개 Moment 조회 - 익명 사용자")
        void findPublicMomentsWithUserPrivateNative_AnonymousUser() {
            // when
            List<Moment> moments = momentRepository.findPublicMomentsWithUserPrivateNative(
                    null, null, MomentTestConstants.DEFAULT_PAGE_LIMIT);

            // then
            assertThat(moments).hasSize(2); // 공개 Moment만
            assertThat(moments).allMatch(Moment::getIsPublic);
        }

        @Test
        @DisplayName("사용자별 Moment 조회 - 비공개 포함")
        void findByUserIdWithVisibilityNative_IncludePrivate() {
            // when
            List<Moment> moments = momentRepository.findByUserIdWithVisibilityNative(
                    MomentTestConstants.TEST_USER_ID, true, null, MomentTestConstants.DEFAULT_PAGE_LIMIT);

            // then
            assertThat(moments).hasSize(2); // 공개 1개 + 비공개 1개
            assertThat(moments).allMatch(moment -> moment.getUserId().equals(MomentTestConstants.TEST_USER_ID));
        }

        @Test
        @DisplayName("사용자별 Moment 조회 - 공개만")
        void findByUserIdWithVisibilityNative_PublicOnly() {
            // when
            List<Moment> moments = momentRepository.findByUserIdWithVisibilityNative(
                    MomentTestConstants.TEST_USER_ID, false, null, MomentTestConstants.DEFAULT_PAGE_LIMIT);

            // then
            assertThat(moments).hasSize(1); // 공개 1개만
            assertThat(moments.get(0).getIsPublic()).isTrue();
            assertThat(moments.get(0).getUserId()).isEqualTo(MomentTestConstants.TEST_USER_ID);
        }

        @Test
        @DisplayName("장소별 공개 Moment 조회")
        void findPublicMomentsByPlaceIdNative() {
            // when
            List<Moment> moments = momentRepository.findPublicMomentsByPlaceIdNative(
                    MomentTestConstants.TEST_PLACE_ID, null, MomentTestConstants.DEFAULT_PAGE_LIMIT);

            // then
            assertThat(moments).hasSize(2); // 공개 Moment 2개
            assertThat(moments).allMatch(Moment::getIsPublic);
            assertThat(moments).allMatch(moment -> moment.getPlaceId().equals(MomentTestConstants.TEST_PLACE_ID));
        }
    }

    @Nested
    @DisplayName("조회수 관련 테스트")
    class ViewCountTest {

        @Test
        @DisplayName("조회수 증가")
        void incrementViewCount() {
            // given
            Long momentId = testMoment.getId();
            Long initialViewCount = testMoment.getViewCount();

            // when
            int updatedRows = momentRepository.incrementViewCount(momentId);

            // then
            assertThat(updatedRows).isEqualTo(1);

            // 데이터베이스에서 다시 조회하여 확인
            Optional<Moment> updatedMoment = momentRepository.findBasicMomentById(momentId);
            assertThat(updatedMoment).isPresent();
            assertThat(updatedMoment.get().getViewCount()).isEqualTo(initialViewCount + 1);
        }

        @Test
        @DisplayName("존재하지 않는 Moment 조회수 증가 시 0 반환")
        void incrementViewCount_NotFound() {
            // when
            int updatedRows = momentRepository.incrementViewCount(999L);

            // then
            assertThat(updatedRows).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("카운트 조회 테스트")
    class CountQueryTest {

        @Test
        @DisplayName("사용자별 Moment 개수 조회 - 비공개 포함")
        void countByUserIdWithVisibility_IncludePrivate() {
            // when
            long count = momentRepository.countByUserIdWithVisibility(MomentTestConstants.TEST_USER_ID, true);

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("사용자별 Moment 개수 조회 - 공개만")
        void countByUserIdWithVisibility_PublicOnly() {
            // when
            long count = momentRepository.countByUserIdWithVisibility(MomentTestConstants.TEST_USER_ID, false);

            // then
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("장소별 공개 Moment 개수 조회")
        void countPublicMomentsByPlaceId() {
            // when
            long count = momentRepository.countPublicMomentsByPlaceId(MomentTestConstants.TEST_PLACE_ID);

            // then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("여러 장소의 공개 Moment 개수 일괄 조회")
        void countPublicMomentsByPlaceIds() {
            // given
            List<Long> placeIds = List.of(MomentTestConstants.TEST_PLACE_ID, 999L);

            // when
            List<Object[]> results = momentRepository.countPublicMomentsByPlaceIds(placeIds);

            // then
            assertThat(results).hasSize(1); // 999L은 결과 없음
            Object[] result = results.get(0);
            assertThat(result[0]).isEqualTo(MomentTestConstants.TEST_PLACE_ID);
            assertThat(result[1]).isEqualTo(2L);
        }

        @Test
        @DisplayName("특정 기간 내 사용자 Moment 개수 조회")
        void countByUserIdAndCreatedAtBetween() {
            // given
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now().plusDays(1);

            // when
            long count = momentRepository.countByUserIdAndCreatedAtBetween(
                    MomentTestConstants.TEST_USER_ID, startDate, endDate);

            // then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("유틸리티 쿼리 테스트")
    class UtilityQueryTest {

        @Test
        @DisplayName("Moment ID로 사용자 ID 조회")
        void findUserIdByMomentId() {
            // when
            Optional<Long> userId = momentRepository.findUserIdByMomentId(testMoment.getId());

            // then
            assertThat(userId).isPresent();
            assertThat(userId.get()).isEqualTo(MomentTestConstants.TEST_USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 Moment ID로 사용자 ID 조회")
        void findUserIdByMomentId_NotFound() {
            // when
            Optional<Long> userId = momentRepository.findUserIdByMomentId(999L);

            // then
            assertThat(userId).isNotPresent();
        }

        @Test
        @DisplayName("최근 공개 Moment 조회")
        void findRecentPublicMoments() {
            // given
            LocalDateTime since = LocalDateTime.now().minusHours(1);
            Pageable pageable = PageRequest.of(0, MomentTestConstants.DEFAULT_PAGE_LIMIT);

            // when
            List<Moment> recentMoments = momentRepository.findRecentPublicMoments(since, pageable);

            // then
            assertThat(recentMoments).hasSize(2);
            assertThat(recentMoments).allMatch(Moment::getIsPublic);
        }

        @Test
        @DisplayName("특정 사용자들의 Moment 조회")
        void findMomentsByUserIds() {
            // given
            List<Long> userIds = List.of(MomentTestConstants.TEST_USER_ID, MomentTestConstants.OTHER_USER_ID);
            Pageable pageable = PageRequest.of(0, MomentTestConstants.DEFAULT_PAGE_LIMIT);

            // when
            var momentsPage = momentRepository.findMomentsByUserIds(userIds, pageable);

            // then
            assertThat(momentsPage.getContent()).hasSize(2);
            assertThat(momentsPage.getContent()).allMatch(Moment::getIsPublic);
            assertThat(momentsPage.getContent())
                    .extracting(Moment::getUserId)
                    .containsExactlyInAnyOrder(MomentTestConstants.TEST_USER_ID, MomentTestConstants.OTHER_USER_ID);
        }
    }

    @Nested
    @DisplayName("커서 기반 페이지네이션 테스트")
    class CursorPaginationTest {

        @Test
        @DisplayName("커서 기반 조회 - 커서 이후 데이터만 반환")
        void findWithCursor() {
            // given
            String cursor = testMoment.getCreatedAt().toString();

            // when
            List<Moment> moments = momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, cursor, MomentTestConstants.DEFAULT_PAGE_LIMIT);

            // then
            assertThat(moments).allMatch(moment ->
                    moment.getCreatedAt().isBefore(testMoment.getCreatedAt()));
        }

        @Test
        @DisplayName("제한된 개수만큼 조회")
        void findWithLimit() {
            // given
            int limit = 1;

            // when
            List<Moment> moments = momentRepository.findPublicMomentsWithUserPrivateNative(
                    MomentTestConstants.TEST_USER_ID, null, limit);

            // then
            assertThat(moments).hasSize(limit);
        }
    }

    // Helper methods
    private Moment createMoment(Long userId, String title, String content, Long placeId, String placeName, Boolean isPublic) {
        return Moment.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .placeId(placeId)
                .placeName(placeName)
                .isPublic(isPublic)
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .build();
    }

    private MomentImage createMomentImage(Moment moment, String imageUrl, Integer sequence) {
        return MomentImage.builder()
                .moment(moment)
                .imageUrl(imageUrl)
                .imageSequence(sequence)
                .build();
    }
}
