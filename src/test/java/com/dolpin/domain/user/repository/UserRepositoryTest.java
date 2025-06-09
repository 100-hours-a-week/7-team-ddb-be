package com.dolpin.domain.user.repository;

import com.dolpin.domain.user.entity.User;
import com.dolpin.global.config.TestConfig;
import com.dolpin.global.constants.UserTestConstants;
import com.dolpin.global.helper.UserTestHelper;
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
@Import({TestConfig.class, UserTestHelper.class})
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

    @Autowired
    private UserTestHelper userTestHelper;

    @Nested
    @DisplayName("findByProviderAndProviderId 메서드 테스트")
    class FindByProviderAndProviderIdTest {

        @Test
        @DisplayName("Provider와 ProviderId로 사용자를 정상적으로 찾는다")
        void findByProviderAndProviderId_WithValidData_ReturnsUser() {
            // given
            User user = userTestHelper.createAndSaveUser(testEntityManager,
                    UserTestConstants.PROVIDER_ID_VALID, UserTestConstants.KAKAO_PROVIDER,
                    UserTestConstants.USERNAME_TEST, UserTestConstants.IMAGE_URL_PROFILE,
                    UserTestConstants.INTRODUCTION_HELLO);

            // when
            Optional<User> result = userRepository.findByProviderAndProviderId(
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.PROVIDER_ID_VALID);

            // then
            assertThat(result).isPresent();
            userTestHelper.assertUserBasicInfo(result.get(), UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 빈 Optional을 반환한다")
        void findByProviderAndProviderId_WithNonExistentUser_ReturnsEmpty() {
            // given
            userTestHelper.createAndSaveUser(testEntityManager,
                    UserTestConstants.PROVIDER_ID_VALID, UserTestConstants.KAKAO_PROVIDER,
                    UserTestConstants.USERNAME_TEST);

            // when
            Optional<User> result = userRepository.findByProviderAndProviderId(
                    UserTestConstants.GOOGLE_PROVIDER, UserTestConstants.NON_EXISTENT_USER_ID);

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
            userTestHelper.createAndSaveUser(testEntityManager,
                    UserTestConstants.PROVIDER_ID_VALID, UserTestConstants.KAKAO_PROVIDER,
                    UserTestConstants.USERNAME_EXISTING);

            // when
            boolean result = userRepository.existsByUsername(UserTestConstants.USERNAME_EXISTING);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 사용자명으로 조회 시 false를 반환한다")
        void existsByUsername_WithNonExistentUsername_ReturnsFalse() {
            // given
            userTestHelper.createAndSaveUser(testEntityManager,
                    UserTestConstants.PROVIDER_ID_VALID, UserTestConstants.KAKAO_PROVIDER,
                    UserTestConstants.USERNAME_EXISTING);

            // when
            boolean result = userRepository.existsByUsername(UserTestConstants.USERNAME_NON_EXISTENT);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("username unique 제약조건이 정상 동작한다")
        void uniqueUsernameConstraint_WorksCorrectly() {
            // given
            User user1 = userTestHelper.createAndSaveUser(testEntityManager,
                    UserTestConstants.PROVIDER_ID_VALID, UserTestConstants.KAKAO_PROVIDER,
                    UserTestConstants.USERNAME_SAME);

            User user2 = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_SHORT,
                    UserTestConstants.GOOGLE_PROVIDER, UserTestConstants.USERNAME_SAME);

            // when & then - 두 번째 사용자 저장 시 예외 발생해야 함
            try {
                testEntityManager.persistAndFlush(user2);
                testEntityManager.flush();
                assertThat(false).as("Unique constraint violation should occur").isTrue();
            } catch (Exception e) {
                userTestHelper.assertConstraintViolation(e);
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
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            // when
            User savedUser = userRepository.save(user);
            Optional<User> foundUser = userRepository.findById(savedUser.getId());

            // then
            assertThat(foundUser).isPresent();
            userTestHelper.assertUserDetails(foundUser.get(), UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);
        }

        @Test
        @DisplayName("User 삭제가 정상 동작한다")
        void userDelete_WorksCorrectly() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_DELETE);

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
