package com.dolpin.domain.user.repository;

import com.dolpin.domain.user.entity.User;
import com.dolpin.global.config.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Nested
    @DisplayName("findByProviderAndProviderId 메서드 테스트")
    class FindByProviderAndProviderIdTest {

        @Test
        @DisplayName("Provider와 ProviderId로 사용자를 정상적으로 찾는다")
        void findByProviderAndProviderId_WithValidData_ReturnsUser() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .imageUrl("profile.jpg")
                    .introduction("안녕하세요!")
                    .build();

            testEntityManager.persistAndFlush(user);

            // when
            Optional<User> result = userRepository.findByProviderAndProviderId("kakao", 12345L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getProviderId()).isEqualTo(12345L);
            assertThat(result.get().getProvider()).isEqualTo("kakao");
            assertThat(result.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 빈 Optional을 반환한다")
        void findByProviderAndProviderId_WithNonExistentUser_ReturnsEmpty() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .build();

            testEntityManager.persistAndFlush(user);

            // when
            Optional<User> result = userRepository.findByProviderAndProviderId("google", 99999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUsername 메서드 테스트")
    class ExistsByUsernameTest {

        @Test
        @DisplayName("존재하는 사용자명으로 조회 시 true를 반환한다")
        void existsByUsername_WithExistingUsername_ReturnsTrue() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("existuser")
                    .build();

            testEntityManager.persistAndFlush(user);

            // when
            boolean result = userRepository.existsByUsername("existuser");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 사용자명으로 조회 시 false를 반환한다")
        void existsByUsername_WithNonExistentUsername_ReturnsFalse() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("existuser")
                    .build();

            testEntityManager.persistAndFlush(user);

            // when
            boolean result = userRepository.existsByUsername("nouser");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("username unique 제약조건이 정상 동작한다")
        void uniqueUsernameConstraint_WorksCorrectly() {
            // given
            User user1 = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("sameuser")
                    .build();

            User user2 = User.builder()
                    .providerId(67890L)
                    .provider("google")
                    .username("sameuser")  // 같은 username
                    .build();

            testEntityManager.persistAndFlush(user1);

            // when & then - 두 번째 사용자 저장 시 예외 발생해야 함
            try {
                testEntityManager.persistAndFlush(user2);
                testEntityManager.flush(); // 강제로 DB에 반영
                assertThat(false).as("Unique constraint violation should occur").isTrue();
            } catch (Exception e) {
                // 예상되는 제약조건 위반 예외
                assertThat(e.getMessage()).containsAnyOf("constraint", "unique", "duplicate");
            }
        }
    }

    @Nested
    @DisplayName("기본 JPA 동작 검증")
    class BasicJpaTest {

        @Test
        @DisplayName("User 엔티티의 저장과 조회가 정상 동작한다")
        void userEntitySaveAndFind_WorksCorrectly() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .imageUrl("profile.jpg")
                    .introduction("안녕하세요!")
                    .build();

            // when
            User savedUser = userRepository.save(user);
            Optional<User> foundUser = userRepository.findById(savedUser.getId());

            // then
            assertThat(foundUser).isPresent();
            assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
            assertThat(foundUser.get().getImageUrl()).isEqualTo("profile.jpg");
            assertThat(foundUser.get().getIntroduction()).isEqualTo("안녕하세요!");
        }

        @Test
        @DisplayName("User 삭제가 정상 동작한다")
        void userDelete_WorksCorrectly() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("deleteuser")
                    .build();

            User savedUser = userRepository.save(user);
            Long userId = savedUser.getId();

            // when
            userRepository.delete(savedUser);

            // then
            Optional<User> result = userRepository.findById(userId);
            assertThat(result).isEmpty();
        }
    }
}
