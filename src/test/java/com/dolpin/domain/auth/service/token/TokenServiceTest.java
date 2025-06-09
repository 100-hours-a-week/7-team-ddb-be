package com.dolpin.domain.auth.service.token;

import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.entity.Token;
import com.dolpin.global.helper.AuthTestHelper;
import com.dolpin.domain.auth.repository.TokenRepository;
import com.dolpin.domain.user.entity.User;
import com.dolpin.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.dolpin.global.constants.AuthTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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

    @Nested
    @DisplayName("createRefreshToken 메서드 테스트")
    class CreateRefreshTokenTest {

        @Test
        @DisplayName("유효한 토큰이 있는 경우 기존 토큰을 재사용한다")
        void createRefreshToken_WithExistingValidToken_ReusesExistingToken() {
            // given
            User user = AuthTestHelper.createUser();
            Token existingToken = AuthTestHelper.createValidToken(user, EXISTING_TOKEN_VALUE);

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
            User user = AuthTestHelper.createUser();
            Token newToken = AuthTestHelper.createValidToken(user, REFRESH_TOKEN_VALUE);

            given(tokenRepository.findValidTokensByUserId(user.getId()))
                    .willReturn(Collections.emptyList());
            given(tokenRepository.findAllByUser(user))
                    .willReturn(Collections.emptyList());
            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(REFRESH_TOKEN_VALUE);
            given(tokenRepository.save(any(Token.class)))
                    .willReturn(newToken);

            // when
            Token result = tokenService.createRefreshToken(user);

            // then
            assertThat(result.getToken()).isEqualTo(REFRESH_TOKEN_VALUE);
            verify(jwtTokenProvider).generateToken(user.getId());
            verify(tokenRepository).save(any(Token.class));
        }

        @Test
        @DisplayName("새 토큰 생성 시 만료된 토큰들을 정리한다")
        void createRefreshToken_WithExpiredTokens_CleansUpExpiredTokens() {
            // given
            User user = AuthTestHelper.createUser();
            Token expiredToken1 = AuthTestHelper.createExpiredToken(user, EXPIRED_TOKEN_VALUE + "-1");
            Token expiredToken2 = AuthTestHelper.createExpiredToken(user, EXPIRED_TOKEN_VALUE + "-2");
            Token newToken = AuthTestHelper.createValidToken(user, REFRESH_TOKEN_VALUE);

            given(tokenRepository.findValidTokensByUserId(user.getId()))
                    .willReturn(Collections.emptyList());
            given(tokenRepository.findAllByUser(user))
                    .willReturn(List.of(expiredToken1, expiredToken2));
            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(REFRESH_TOKEN_VALUE);
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
            User user = AuthTestHelper.createUser();
            Token expiredToken = AuthTestHelper.createExpiredToken(user, EXPIRED_TOKEN_VALUE);
            Token validToken = AuthTestHelper.createValidToken(user, VALID_TOKEN_VALUE);

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
            User user = AuthTestHelper.createUser();
            Token validToken = AuthTestHelper.createValidToken(user, VALID_TOKEN_VALUE);

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
            User user = AuthTestHelper.createUser();
            Token token1 = AuthTestHelper.createValidToken(user, VALID_TOKEN_VALUE + "-1");
            Token token2 = AuthTestHelper.createValidToken(user, VALID_TOKEN_VALUE + "-2");

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
            User user = AuthTestHelper.createUser();

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
            User user = AuthTestHelper.createUser();
            Token refreshToken = AuthTestHelper.createValidToken(user, VALID_REFRESH_TOKEN);

            given(tokenRepository.findByToken(VALID_REFRESH_TOKEN))
                    .willReturn(Optional.of(refreshToken));
            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(NEW_ACCESS_TOKEN);
            given(jwtTokenProvider.getExpirationMs())
                    .willReturn(TEST_EXPIRATION_MS);

            // when
            RefreshTokenResponse result = tokenService.refreshAccessToken(VALID_REFRESH_TOKEN);

            // then
            assertThat(result.getNewAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);
            assertThat(result.getExpiresIn()).isEqualTo(TEST_EXPIRATION_MS / 1000);
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refreshAccessToken_WithNonExistentToken_ThrowsException() {
            // given
            given(tokenRepository.findByToken(NON_EXISTENT_TOKEN_VALUE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenService.refreshAccessToken(NON_EXISTENT_TOKEN_VALUE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(REFRESH_TOKEN_INVALID_MESSAGE);
        }

        @Test
        @DisplayName("만료된 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refreshAccessToken_WithExpiredToken_ThrowsException() {
            // given
            User user = AuthTestHelper.createUser();
            Token expiredToken = AuthTestHelper.createExpiredToken(user, EXPIRED_REFRESH_TOKEN);

            given(tokenRepository.findByToken(EXPIRED_REFRESH_TOKEN))
                    .willReturn(Optional.of(expiredToken));

            // when & then
            assertThatThrownBy(() -> tokenService.refreshAccessToken(EXPIRED_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(REFRESH_TOKEN_EXPIRED_MESSAGE);
        }

        @Test
        @DisplayName("취소된 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refreshAccessToken_WithRevokedToken_ThrowsException() {
            // given
            User user = AuthTestHelper.createUser();
            Token revokedToken = AuthTestHelper.createRevokedToken(user, REVOKED_TOKEN_VALUE);

            given(tokenRepository.findByToken(REVOKED_TOKEN_VALUE))
                    .willReturn(Optional.of(revokedToken));

            // when & then
            assertThatThrownBy(() -> tokenService.refreshAccessToken(REVOKED_TOKEN_VALUE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(REFRESH_TOKEN_EXPIRED_MESSAGE);
        }
    }

    @Nested
    @DisplayName("invalidateRefreshToken 메서드 테스트")
    class InvalidateRefreshTokenTest {

        @Test
        @DisplayName("유효한 리프레시 토큰을 무효화한다")
        void invalidateRefreshToken_WithValidToken_InvalidatesToken() {
            // given
            User user = AuthTestHelper.createUser();
            Token refreshToken = AuthTestHelper.createValidToken(user, VALID_REFRESH_TOKEN);

            given(tokenRepository.findByToken(VALID_REFRESH_TOKEN))
                    .willReturn(Optional.of(refreshToken));

            // when
            tokenService.invalidateRefreshToken(VALID_REFRESH_TOKEN);

            // then
            verify(tokenRepository).save(eq(refreshToken));
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰 무효화 시 예외가 발생한다")
        void invalidateRefreshToken_WithNonExistentToken_ThrowsException() {
            // given
            given(tokenRepository.findByToken(NON_EXISTENT_TOKEN_VALUE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenService.invalidateRefreshToken(NON_EXISTENT_TOKEN_VALUE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(REFRESH_TOKEN_INVALID_MESSAGE);
        }
    }

    @Nested
    @DisplayName("validateRefreshToken 메서드 테스트")
    class ValidateRefreshTokenTest {

        @Test
        @DisplayName("유효한 리프레시 토큰 검증 시 true를 반환한다")
        void validateRefreshToken_WithValidToken_ReturnsTrue() {
            // given
            User user = AuthTestHelper.createUser();
            Token validToken = AuthTestHelper.createValidToken(user, VALID_REFRESH_TOKEN);

            given(tokenRepository.findByToken(VALID_REFRESH_TOKEN))
                    .willReturn(Optional.of(validToken));

            // when
            boolean result = tokenService.validateRefreshToken(VALID_REFRESH_TOKEN);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("만료된 리프레시 토큰 검증 시 false를 반환한다")
        void validateRefreshToken_WithExpiredToken_ReturnsFalse() {
            // given
            User user = AuthTestHelper.createUser();
            Token expiredToken = AuthTestHelper.createExpiredToken(user, EXPIRED_REFRESH_TOKEN);

            given(tokenRepository.findByToken(EXPIRED_REFRESH_TOKEN))
                    .willReturn(Optional.of(expiredToken));

            // when
            boolean result = tokenService.validateRefreshToken(EXPIRED_REFRESH_TOKEN);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰 검증 시 false를 반환한다")
        void validateRefreshToken_WithNonExistentToken_ReturnsFalse() {
            // given
            given(tokenRepository.findByToken(NON_EXISTENT_TOKEN_VALUE))
                    .willReturn(Optional.empty());

            // when
            boolean result = tokenService.validateRefreshToken(NON_EXISTENT_TOKEN_VALUE);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("취소된 리프레시 토큰 검증 시 false를 반환한다")
        void validateRefreshToken_WithRevokedToken_ReturnsFalse() {
            // given
            User user = AuthTestHelper.createUser();
            Token revokedToken = AuthTestHelper.createRevokedToken(user, REVOKED_TOKEN_VALUE);

            given(tokenRepository.findByToken(REVOKED_TOKEN_VALUE))
                    .willReturn(Optional.of(revokedToken));

            // when
            boolean result = tokenService.validateRefreshToken(REVOKED_TOKEN_VALUE);

            // then
            assertThat(result).isFalse();
        }
    }
}
