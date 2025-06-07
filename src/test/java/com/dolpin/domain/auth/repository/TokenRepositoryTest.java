package com.dolpin.domain.auth.repository;

import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.auth.entity.enums.TokenStatus;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    private User createAndSaveUser(String username, Long providerId) {
        User user = User.builder()
                .providerId(providerId)
                .provider("kakao")
                .username(username)
                .build();
        return testEntityManager.persistAndFlush(user);
    }

    private Token createToken(User user, String tokenValue, LocalDateTime expiredAt, boolean isRevoked) {
        return Token.builder()
                .user(user)
                .status(TokenStatus.ACTIVE)
                .token(tokenValue)
                .createdAt(LocalDateTime.now())
                .expiredAt(expiredAt)
                .isRevoked(isRevoked)
                .build();
    }

    @Nested
    @DisplayName("findByToken 메서드 테스트")
    class FindByTokenTest {

        @Test
        @DisplayName("토큰 문자열로 토큰을 정상적으로 찾는다")
        void findByToken_WithValidToken_ReturnsToken() {
            // given
            User user = createAndSaveUser("testuser", 12345L);
            Token token = createToken(user, "valid-token-123",
                    LocalDateTime.now().plusDays(1), false);
            testEntityManager.persistAndFlush(token);

            // when
            Optional<Token> result = tokenRepository.findByToken("valid-token-123");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getToken()).isEqualTo("valid-token-123");
            assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("존재하지 않는 토큰 조회 시 빈 Optional을 반환한다")
        void findByToken_WithNonExistentToken_ReturnsEmpty() {
            // given
            User user = createAndSaveUser("testuser", 12345L);
            Token token = createToken(user, "existing-token",
                    LocalDateTime.now().plusDays(1), false);
            testEntityManager.persistAndFlush(token);

            // when
            Optional<Token> result = tokenRepository.findByToken("non-existent-token");

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
            User user = createAndSaveUser("testuser", 12345L);

            Token token1 = createToken(user, "token-1",
                    LocalDateTime.now().plusDays(1), false);
            Token token2 = createToken(user, "token-2",
                    LocalDateTime.now().plusDays(2), true);
            Token token3 = createToken(user, "token-3",
                    LocalDateTime.now().minusDays(1), false);

            testEntityManager.persistAndFlush(token1);
            testEntityManager.persistAndFlush(token2);
            testEntityManager.persistAndFlush(token3);

            // when
            List<Token> result = tokenRepository.findAllByUser(user);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(Token::getToken)
                    .containsExactlyInAnyOrder("token-1", "token-2", "token-3");
        }

        @Test
        @DisplayName("토큰이 없는 사용자 조회 시 빈 리스트를 반환한다")
        void findAllByUser_WithNoTokens_ReturnsEmptyList() {
            // given
            User user = createAndSaveUser("notokens", 12345L);

            // when
            List<Token> result = tokenRepository.findAllByUser(user);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 토큰은 조회되지 않는다")
        void findAllByUser_WithDifferentUser_ReturnsOnlyUserTokens() {
            // given
            User user1 = createAndSaveUser("user1", 12345L);
            User user2 = createAndSaveUser("user2", 67890L);

            Token token1 = createToken(user1, "user1-token",
                    LocalDateTime.now().plusDays(1), false);
            Token token2 = createToken(user2, "user2-token",
                    LocalDateTime.now().plusDays(1), false);

            testEntityManager.persistAndFlush(token1);
            testEntityManager.persistAndFlush(token2);

            // when
            List<Token> result = tokenRepository.findAllByUser(user1);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getToken()).isEqualTo("user1-token");
        }
    }

    @Nested
    @DisplayName("findValidTokensByUserId 메서드 테스트")
    class FindValidTokensByUserIdTest {

        @Test
        @DisplayName("유효한 토큰만 조회한다")
        void findValidTokensByUserId_ReturnsOnlyValidTokens() {
            // given
            User user = createAndSaveUser("testuser", 12345L);

            // 유효한 토큰
            Token validToken = createToken(user, "valid-token",
                    LocalDateTime.now().plusDays(1), false);

            // 만료된 토큰
            Token expiredToken = createToken(user, "expired-token",
                    LocalDateTime.now().minusDays(1), false);

            // 취소된 토큰
            Token revokedToken = createToken(user, "revoked-token",
                    LocalDateTime.now().plusDays(1), true);

            // 만료되고 취소된 토큰
            Token expiredAndRevokedToken = createToken(user, "expired-revoked-token",
                    LocalDateTime.now().minusDays(1), true);

            testEntityManager.persistAndFlush(validToken);
            testEntityManager.persistAndFlush(expiredToken);
            testEntityManager.persistAndFlush(revokedToken);
            testEntityManager.persistAndFlush(expiredAndRevokedToken);

            // when
            List<Token> result = tokenRepository.findValidTokensByUserId(user.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getToken()).isEqualTo("valid-token");
        }

        @Test
        @DisplayName("유효한 토큰이 없는 경우 빈 리스트를 반환한다")
        void findValidTokensByUserId_WithNoValidTokens_ReturnsEmptyList() {
            // given
            User user = createAndSaveUser("testuser", 12345L);

            Token expiredToken = createToken(user, "expired-token",
                    LocalDateTime.now().minusDays(1), false);
            Token revokedToken = createToken(user, "revoked-token",
                    LocalDateTime.now().plusDays(1), true);

            testEntityManager.persistAndFlush(expiredToken);
            testEntityManager.persistAndFlush(revokedToken);

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
            User user = createAndSaveUser("testuser", 12345L);

            Token token1 = createToken(user, "token-1",
                    LocalDateTime.now().plusDays(1), false);
            Token token2 = createToken(user, "token-2",
                    LocalDateTime.now().plusDays(2), true);

            testEntityManager.persistAndFlush(token1);
            testEntityManager.persistAndFlush(token2);

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
            User user1 = createAndSaveUser("user1", 12345L);
            User user2 = createAndSaveUser("user2", 67890L);

            Token user1Token = createToken(user1, "user1-token",
                    LocalDateTime.now().plusDays(1), false);
            Token user2Token = createToken(user2, "user2-token",
                    LocalDateTime.now().plusDays(1), false);

            testEntityManager.persistAndFlush(user1Token);
            testEntityManager.persistAndFlush(user2Token);

            // when
            tokenRepository.deleteAllByUser(user1);
            testEntityManager.flush();

            // then
            List<Token> user1Tokens = tokenRepository.findAllByUser(user1);
            List<Token> user2Tokens = tokenRepository.findAllByUser(user2);

            assertThat(user1Tokens).isEmpty();
            assertThat(user2Tokens).hasSize(1);
            assertThat(user2Tokens.get(0).getToken()).isEqualTo("user2-token");
        }
    }

    @Nested
    @DisplayName("기본 JPA 동작 검증")
    class BasicJpaTest {

        @Test
        @DisplayName("Token 엔티티의 저장과 조회가 정상 동작한다")
        void tokenEntitySaveAndFind_WorksCorrectly() {
            // given
            User user = createAndSaveUser("testuser", 12345L);
            Token token = createToken(user, "test-token",
                    LocalDateTime.now().plusDays(1), false);

            // when
            Token savedToken = tokenRepository.save(token);
            Optional<Token> foundToken = tokenRepository.findById(savedToken.getId());

            // then
            assertThat(foundToken).isPresent();
            assertThat(foundToken.get().getToken()).isEqualTo("test-token");
            assertThat(foundToken.get().getStatus()).isEqualTo(TokenStatus.ACTIVE);
            assertThat(foundToken.get().isRevoked()).isFalse();
        }

        @Test
        @DisplayName("Token 삭제가 정상 동작한다")
        void tokenDelete_WorksCorrectly() {
            // given
            User user = createAndSaveUser("testuser", 12345L);
            Token token = createToken(user, "delete-token",
                    LocalDateTime.now().plusDays(1), false);

            Token savedToken = tokenRepository.save(token);
            Long tokenId = savedToken.getId();

            // when
            tokenRepository.delete(savedToken);

            // then
            Optional<Token> result = tokenRepository.findById(tokenId);
            assertThat(result).isEmpty();
        }
    }
}
