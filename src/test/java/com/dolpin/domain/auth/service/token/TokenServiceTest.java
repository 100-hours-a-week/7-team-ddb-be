package com.dolpin.domain.auth.service.token;

import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.auth.entity.enums.TokenStatus;
import com.dolpin.domain.auth.repository.TokenRepository;
import com.dolpin.domain.user.entity.User;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 테스트")
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .providerId(12345L)
                .provider("kakao")
                .username("testuser")
                .build();
    }

    private Token createTestToken(User user, String tokenValue, LocalDateTime expiredAt, boolean isRevoked) {
        return Token.builder()
                .id(1L)
                .user(user)
                .status(TokenStatus.ACTIVE)
                .token(tokenValue)
                .createdAt(LocalDateTime.now())
                .expiredAt(expiredAt)
                .isRevoked(isRevoked)
                .build();
    }

    @Nested
    @DisplayName("createRefreshToken 메서드 테스트")
    class CreateRefreshTokenTest {

        @Test
        @DisplayName("유효한 토큰이 있는 경우 기존 토큰을 재사용한다")
        void createRefreshToken_WithExistingValidToken_ReusesExistingToken() {
            // given
            User user = createTestUser();
            Token existingToken = createTestToken(user, "existing-token",
                    LocalDateTime.now().plusDays(7), false);

            given(tokenRepository.findValidTokensByUserId(user.getId()))
                    .willReturn(List.of(existingToken));

            // when
            Token result = tokenService.createRefreshToken(user);

            // then
            assertThat(result).isEqualTo(existingToken);
            verify(tokenRepository, never()).save(any(Token.class));
            verify(jwtTokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("유효한 토큰이 없는 경우 새 토큰을 생성한다")
        void createRefreshToken_WithNoValidToken_CreatesNewToken() {
            // given
            User user = createTestUser();
            String newTokenValue = "new-refresh-token";
            Token newToken = createTestToken(user, newTokenValue,
                    LocalDateTime.now().plusDays(14), false);

            given(tokenRepository.findValidTokensByUserId(user.getId()))
                    .willReturn(Collections.emptyList());
            given(tokenRepository.findAllByUser(user))
                    .willReturn(Collections.emptyList());
            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(newTokenValue);
            given(tokenRepository.save(any(Token.class)))
                    .willReturn(newToken);

            // when
            Token result = tokenService.createRefreshToken(user);

            // then
            assertThat(result.getToken()).isEqualTo(newTokenValue);
            verify(jwtTokenProvider).generateToken(user.getId());
            verify(tokenRepository).save(any(Token.class));
        }

        @Test
        @DisplayName("새 토큰 생성 시 만료된 토큰들을 정리한다")
        void createRefreshToken_WithExpiredTokens_CleansUpExpiredTokens() {
            // given
            User user = createTestUser();
            Token expiredToken1 = createTestToken(user, "expired-1",
                    LocalDateTime.now().minusDays(1), false);
            Token expiredToken2 = createTestToken(user, "expired-2",
                    LocalDateTime.now().minusDays(2), false);

            // Mock the isExpired method behavior
            expiredToken1 = Token.builder()
                    .id(1L)
                    .user(user)
                    .status(TokenStatus.ACTIVE)
                    .token("expired-1")
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .expiredAt(LocalDateTime.now().minusDays(1))
                    .isRevoked(false)
                    .build();

            expiredToken2 = Token.builder()
                    .id(2L)
                    .user(user)
                    .status(TokenStatus.ACTIVE)
                    .token("expired-2")
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .expiredAt(LocalDateTime.now().minusDays(2))
                    .isRevoked(false)
                    .build();

            String newTokenValue = "new-refresh-token";
            Token newToken = createTestToken(user, newTokenValue,
                    LocalDateTime.now().plusDays(14), false);

            given(tokenRepository.findValidTokensByUserId(user.getId()))
                    .willReturn(Collections.emptyList());
            given(tokenRepository.findAllByUser(user))
                    .willReturn(List.of(expiredToken1, expiredToken2));
            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(newTokenValue);
            given(tokenRepository.save(any(Token.class)))
                    .willReturn(newToken);

            // when
            tokenService.createRefreshToken(user);

            // then
            verify(tokenRepository).saveAll(anyList());
            verify(tokenRepository).save(any(Token.class));
        }
    }

    @Nested
    @DisplayName("cleanupExpiredTokens 메서드 테스트")
    class CleanupExpiredTokensTest {

        @Test
        @DisplayName("만료된 토큰들을 정상적으로 정리한다")
        void cleanupExpiredTokens_WithExpiredTokens_CleansUpTokens() {
            // given
            User user = createTestUser();
            Token expiredToken = Token.builder()
                    .id(1L)
                    .user(user)
                    .status(TokenStatus.ACTIVE)
                    .token("expired-token")
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .expiredAt(LocalDateTime.now().minusDays(1))
                    .isRevoked(false)
                    .build();

            Token validToken = createTestToken(user, "valid-token",
                    LocalDateTime.now().plusDays(1), false);

            given(tokenRepository.findAllByUser(user))
                    .willReturn(List.of(expiredToken, validToken));

            // when
            tokenService.cleanupExpiredTokens(user);

            // then
            verify(tokenRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("만료된 토큰이 없는 경우 정리하지 않는다")
        void cleanupExpiredTokens_WithNoExpiredTokens_DoesNotCleanup() {
            // given
            User user = createTestUser();
            Token validToken = createTestToken(user, "valid-token",
                    LocalDateTime.now().plusDays(1), false);

            given(tokenRepository.findAllByUser(user))
                    .willReturn(List.of(validToken));

            // when
            tokenService.cleanupExpiredTokens(user);

            // then
            verify(tokenRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("invalidateUserTokens 메서드 테스트")
    class InvalidateUserTokensTest {

        @Test
        @DisplayName("사용자의 모든 토큰을 무효화한다")
        void invalidateUserTokens_InvalidatesAllUserTokens() {
            // given
            User user = createTestUser();
            Token token1 = createTestToken(user, "token-1",
                    LocalDateTime.now().plusDays(1), false);
            Token token2 = createTestToken(user, "token-2",
                    LocalDateTime.now().plusDays(2), false);

            given(tokenRepository.findAllByUser(user))
                    .willReturn(List.of(token1, token2));

            // when
            tokenService.invalidateUserTokens(user);

            // then
            verify(tokenRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("토큰이 없는 사용자의 경우 정상 처리된다")
        void invalidateUserTokens_WithNoTokens_HandlesGracefully() {
            // given
            User user = createTestUser();

            given(tokenRepository.findAllByUser(user))
                    .willReturn(Collections.emptyList());

            // when
            tokenService.invalidateUserTokens(user);

            // then
            verify(tokenRepository).saveAll(Collections.emptyList());
        }
    }

    @Nested
    @DisplayName("refreshAccessToken 메서드 테스트")
    class RefreshAccessTokenTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 액세스 토큰을 갱신한다")
        void refreshAccessToken_WithValidRefreshToken_ReturnsNewAccessToken() {
            // given
            User user = createTestUser();
            String refreshTokenValue = "valid-refresh-token";
            String newAccessToken = "new-access-token";
            long expirationMs = 3600000L;

            Token refreshToken = createTestToken(user, refreshTokenValue,
                    LocalDateTime.now().plusDays(7), false);

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(refreshToken));
            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(newAccessToken);
            given(jwtTokenProvider.getExpirationMs())
                    .willReturn(expirationMs);

            // when
            RefreshTokenResponse result = tokenService.refreshAccessToken(refreshTokenValue);

            // then
            assertThat(result.getNewAccessToken()).isEqualTo(newAccessToken);
            assertThat(result.getExpiresIn()).isEqualTo(expirationMs / 1000);
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refreshAccessToken_WithNonExistentToken_ThrowsException() {
            // given
            String refreshTokenValue = "non-existent-token";

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenService.refreshAccessToken(refreshTokenValue))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리프레시 토큰이 유효하지 않습니다");
        }

        @Test
        @DisplayName("만료된 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refreshAccessToken_WithExpiredToken_ThrowsException() {
            // given
            User user = createTestUser();
            String refreshTokenValue = "expired-refresh-token";

            Token expiredToken = Token.builder()
                    .id(1L)
                    .user(user)
                    .status(TokenStatus.ACTIVE)
                    .token(refreshTokenValue)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .expiredAt(LocalDateTime.now().minusDays(1))
                    .isRevoked(false)
                    .build();

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(expiredToken));

            // when & then
            assertThatThrownBy(() -> tokenService.refreshAccessToken(refreshTokenValue))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리프레시 토큰이 만료되었습니다");
        }

        @Test
        @DisplayName("취소된 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refreshAccessToken_WithRevokedToken_ThrowsException() {
            // given
            User user = createTestUser();
            String refreshTokenValue = "revoked-refresh-token";

            Token revokedToken = createTestToken(user, refreshTokenValue,
                    LocalDateTime.now().plusDays(1), true);

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(revokedToken));

            // when & then
            assertThatThrownBy(() -> tokenService.refreshAccessToken(refreshTokenValue))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리프레시 토큰이 만료되었습니다");
        }
    }

    @Nested
    @DisplayName("invalidateRefreshToken 메서드 테스트")
    class InvalidateRefreshTokenTest {

        @Test
        @DisplayName("유효한 리프레시 토큰을 무효화한다")
        void invalidateRefreshToken_WithValidToken_InvalidatesToken() {
            // given
            User user = createTestUser();
            String refreshTokenValue = "valid-refresh-token";
            Token refreshToken = createTestToken(user, refreshTokenValue,
                    LocalDateTime.now().plusDays(7), false);

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(refreshToken));

            // when
            tokenService.invalidateRefreshToken(refreshTokenValue);

            // then
            verify(tokenRepository).save(eq(refreshToken));
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰 무효화 시 예외가 발생한다")
        void invalidateRefreshToken_WithNonExistentToken_ThrowsException() {
            // given
            String refreshTokenValue = "non-existent-token";

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenService.invalidateRefreshToken(refreshTokenValue))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리프레시 토큰이 유효하지 않습니다");
        }
    }

    @Nested
    @DisplayName("validateRefreshToken 메서드 테스트")
    class ValidateRefreshTokenTest {

        @Test
        @DisplayName("유효한 리프레시 토큰 검증 시 true를 반환한다")
        void validateRefreshToken_WithValidToken_ReturnsTrue() {
            // given
            User user = createTestUser();
            String refreshTokenValue = "valid-refresh-token";
            Token validToken = createTestToken(user, refreshTokenValue,
                    LocalDateTime.now().plusDays(7), false);

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(validToken));

            // when
            boolean result = tokenService.validateRefreshToken(refreshTokenValue);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("만료된 리프레시 토큰 검증 시 false를 반환한다")
        void validateRefreshToken_WithExpiredToken_ReturnsFalse() {
            // given
            User user = createTestUser();
            String refreshTokenValue = "expired-refresh-token";

            Token expiredToken = Token.builder()
                    .id(1L)
                    .user(user)
                    .status(TokenStatus.ACTIVE)
                    .token(refreshTokenValue)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .expiredAt(LocalDateTime.now().minusDays(1))
                    .isRevoked(false)
                    .build();

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(expiredToken));

            // when
            boolean result = tokenService.validateRefreshToken(refreshTokenValue);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰 검증 시 false를 반환한다")
        void validateRefreshToken_WithNonExistentToken_ReturnsFalse() {
            // given
            String refreshTokenValue = "non-existent-token";

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.empty());

            // when
            boolean result = tokenService.validateRefreshToken(refreshTokenValue);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("취소된 리프레시 토큰 검증 시 false를 반환한다")
        void validateRefreshToken_WithRevokedToken_ReturnsFalse() {
            // given
            User user = createTestUser();
            String refreshTokenValue = "revoked-refresh-token";
            Token revokedToken = createTestToken(user, refreshTokenValue,
                    LocalDateTime.now().plusDays(1), true);

            given(tokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(revokedToken));

            // when
            boolean result = tokenService.validateRefreshToken(refreshTokenValue);

            // then
            assertThat(result).isFalse();
        }
    }
}
