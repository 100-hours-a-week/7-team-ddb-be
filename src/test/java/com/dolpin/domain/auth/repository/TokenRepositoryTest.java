package com.dolpin.domain.auth.repository;

import com.dolpin.domain.auth.entity.Token;
import com.dolpin.global.helper.AuthTestHelper;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.repository.UserRepository;
import com.dolpin.global.config.TestConfig;
import org.junit.jupiter.api.AfterEach;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.dolpin.global.constants.AuthTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfig.class)
@DisplayName("TokenRepository 테스트")
class TokenRepositoryTest {

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
    private TokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @AfterEach
    void tearDown() {
        testEntityManager.clear();
    }

    private User persistUser(String username, Long providerId) {
        User user = AuthTestHelper.createUser(null, username, providerId);
        return testEntityManager.persistAndFlush(user);
    }

    private Token persistToken(User user, String tokenValue, LocalDateTime expiredAt, boolean isRevoked) {
        Token token = AuthTestHelper.createToken(user, tokenValue, expiredAt, isRevoked);
        return testEntityManager.persistAndFlush(token);
    }

    @Nested
    @DisplayName("findByToken 메서드 테스트")
    class FindByTokenTest {

        @Test
        @DisplayName("토큰 문자열로 토큰을 정상적으로 찾는다")
        void findByToken_WithValidToken_ReturnsToken() {
            // given
            User user = persistUser(TEST_USERNAME, TEST_PROVIDER_ID);
            Token token = persistToken(user, VALID_TOKEN_VALUE,
                    LocalDateTime.now().plusDays(1), false);

            // when
            Optional<Token> result = tokenRepository.findByToken(VALID_TOKEN_VALUE);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getToken()).isEqualTo(VALID_TOKEN_VALUE);
            assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("존재하지 않는 토큰 조회 시 빈 Optional을 반환한다")
        void findByToken_WithNonExistentToken_ReturnsEmpty() {
            // given
            User user = persistUser(TEST_USERNAME, TEST_PROVIDER_ID);
            persistToken(user, EXISTING_TOKEN_VALUE,
                    LocalDateTime.now().plusDays(1), false);

            // when
            Optional<Token> result = tokenRepository.findByToken(NON_EXISTENT_TOKEN_VALUE);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByUser 메서드 테스트")
    class FindAllByUserTest {

        @Test
        @DisplayName("사용자의 모든 토큰을 정상적으로 조회한다")
        void findAllByUser_WithMultipleTokens_ReturnsAllTokens() {
            // given
            User user = persistUser(TEST_USERNAME, TEST_PROVIDER_ID);

            String token1Value = VALID_TOKEN_VALUE + "-1";
            String token2Value = VALID_TOKEN_VALUE + "-2";
            String token3Value = VALID_TOKEN_VALUE + "-3";

            persistToken(user, token1Value, LocalDateTime.now().plusDays(1), false);
            persistToken(user, token2Value, LocalDateTime.now().plusDays(2), true);
            persistToken(user, token3Value, LocalDateTime.now().minusDays(1), false);

            // when
            List<Token> result = tokenRepository.findAllByUser(user);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(Token::getToken)
                    .containsExactlyInAnyOrder(token1Value, token2Value, token3Value);
        }

        @Test
        @DisplayName("토큰이 없는 사용자 조회 시 빈 리스트를 반환한다")
        void findAllByUser_WithNoTokens_ReturnsEmptyList() {
            // given
            User user = persistUser(TEST_USERNAME, TEST_PROVIDER_ID);

            // when
            List<Token> result = tokenRepository.findAllByUser(user);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 토큰은 조회되지 않는다")
        void findAllByUser_WithDifferentUser_ReturnsOnlyUserTokens() {
            // given
            User user1 = persistUser(TEST_USERNAME_2, TEST_PROVIDER_ID);
            User user2 = persistUser(TEST_USERNAME_3, TEST_PROVIDER_ID_2);

            String user1TokenValue = "user1-token";
            String user2TokenValue = "user2-token";

            persistToken(user1, user1TokenValue, LocalDateTime.now().plusDays(1), false);
            persistToken(user2, user2TokenValue, LocalDateTime.now().plusDays(1), false);

            // when
            List<Token> result = tokenRepository.findAllByUser(user1);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getToken()).isEqualTo(user1TokenValue);
        }
    }

    @Nested
    @DisplayName("findValidTokensByUserId 메서드 테스트")
    class FindValidTokensByUserIdTest {

        @Test
        @DisplayName("유효한 토큰만 조회한다")
        void findValidTokensByUserId_ReturnsOnlyValidTokens() {
            // given
            User user = persistUser(TEST_USERNAME, TEST_PROVIDER_ID);

            String validTokenValue = VALID_TOKEN_VALUE;
            String expiredTokenValue = EXPIRED_TOKEN_VALUE;
            String revokedTokenValue = REVOKED_TOKEN_VALUE;
            String expiredRevokedTokenValue = EXPIRED_REVOKED_TOKEN_VALUE;

            // 유효한 토큰
            persistToken(user, validTokenValue, LocalDateTime.now().plusDays(1), false);
            // 만료된 토큰
            persistToken(user, expiredTokenValue, LocalDateTime.now().minusDays(1), false);
            // 취소된 토큰
            persistToken(user, revokedTokenValue, LocalDateTime.now().plusDays(1), true);
            // 만료되고 취소된 토큰
            persistToken(user, expiredRevokedTokenValue, LocalDateTime.now().minusDays(1), true);

            // when
            List<Token> result = tokenRepository.findValidTokensByUserId(user.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getToken()).isEqualTo(validTokenValue);
        }

        @Test
        @DisplayName("유효한 토큰이 없는 경우 빈 리스트를 반환한다")
        void findValidTokensByUserId_WithNoValidTokens_ReturnsEmptyList() {
            // given
            User user = persistUser(TEST_USERNAME, TEST_PROVIDER_ID);

            persistToken(user, EXPIRED_TOKEN_VALUE, LocalDateTime.now().minusDays(1), false);
            persistToken(user, REVOKED_TOKEN_VALUE, LocalDateTime.now().plusDays(1), true);

            // when
            List<Token> result = tokenRepository.findValidTokensByUserId(user.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 리스트를 반환한다")
        void findValidTokensByUserId_WithNonExistentUserId_ReturnsEmptyList() {
            // given
            Long nonExistentUserId = 99999L;

            // when
            List<Token> result = tokenRepository.findValidTokensByUserId(nonExistentUserId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteAllByUser 메서드 테스트")
    class DeleteAllByUserTest {

        @Test
        @DisplayName("사용자의 모든 토큰을 정상적으로 삭제한다")
        void deleteAllByUser_DeletesAllUserTokens() {
            // given
            User user = persistUser(TEST_USERNAME, TEST_PROVIDER_ID);

            String token1Value = VALID_TOKEN_VALUE + "-1";
            String token2Value = VALID_TOKEN_VALUE + "-2";

            persistToken(user, token1Value, LocalDateTime.now().plusDays(1), false);
            persistToken(user, token2Value, LocalDateTime.now().plusDays(2), true);

            // when
            tokenRepository.deleteAllByUser(user);
            testEntityManager.flush();

            // then
            List<Token> result = tokenRepository.findAllByUser(user);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 토큰은 삭제되지 않는다")
        void deleteAllByUser_DoesNotDeleteOtherUserTokens() {
            // given
            User user1 = persistUser(TEST_USERNAME_2, TEST_PROVIDER_ID);
            User user2 = persistUser(TEST_USERNAME_3, TEST_PROVIDER_ID_2);

            String user1TokenValue = "user1-token";
            String user2TokenValue = "user2-token";

            persistToken(user1, user1TokenValue, LocalDateTime.now().plusDays(1), false);
            persistToken(user2, user2TokenValue, LocalDateTime.now().plusDays(1), false);

            // when
            tokenRepository.deleteAllByUser(user1);
            testEntityManager.flush();

            // then
            List<Token> user1Tokens = tokenRepository.findAllByUser(user1);
            List<Token> user2Tokens = tokenRepository.findAllByUser(user2);

            assertThat(user1Tokens).isEmpty();
            assertThat(user2Tokens).hasSize(1);
            assertThat(user2Tokens.get(0).getToken()).isEqualTo(user2TokenValue);
        }
    }

    @Nested
    @DisplayName("기본 JPA 동작 검증")
    class BasicJpaTest {

        @Test
        @DisplayName("Token 엔티티의 저장과 조회가 정상 동작한다")
        void tokenEntitySaveAndFind_WorksCorrectly() {
            // given
            User user = persistUser(TEST_USERNAME, TEST_PROVIDER_ID);
            Token token = AuthTestHelper.createToken(user, VALID_TOKEN_VALUE,
                    LocalDateTime.now().plusDays(1), false);

            // when
            Token savedToken = tokenRepository.save(token);
            Optional<Token> foundToken = tokenRepository.findById(savedToken.getId());

            // then
            assertThat(foundToken).isPresent();
            assertThat(foundToken.get().getToken()).isEqualTo(VALID_TOKEN_VALUE);
            assertThat(foundToken.get().isRevoked()).isFalse();
        }

        @Test
        @DisplayName("Token 삭제가 정상 동작한다")
        void tokenDelete_WorksCorrectly() {
            // given
            User user = persistUser(TEST_USERNAME, TEST_PROVIDER_ID);
            Token token = persistToken(user, "delete-token",
                    LocalDateTime.now().plusDays(1), false);
            Long tokenId = token.getId();

            // when
            tokenRepository.delete(token);

            // then
            Optional<Token> result = tokenRepository.findById(tokenId);
            assertThat(result).isEmpty();
        }
    }
}
