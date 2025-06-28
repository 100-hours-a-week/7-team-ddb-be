package com.dolpin.domain.auth.service.token;

import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.auth.service.cache.RefreshTokenCacheService;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.dolpin.global.constants.AuthTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
    private RefreshTokenCacheService refreshTokenCacheService; // 새로 추가

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("createRefreshToken 메서드 테스트")
    class CreateRefreshTokenTest {

        @Test
        @DisplayName("새 토큰을 생성하고 Redis와 DB에 저장한다")
        void createRefreshToken_CreatesNewTokenAndSavesToRedisAndDB() {
            // given
            User user = AuthTestHelper.createUser();
            Token newToken = AuthTestHelper.createValidToken(user, REFRESH_TOKEN_VALUE);

            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(REFRESH_TOKEN_VALUE);
            given(tokenRepository.save(any(Token.class)))
                    .willReturn(newToken);

            // when
            Token result = tokenService.createRefreshToken(user);

            // then
            assertThat(result.getToken()).isEqualTo(REFRESH_TOKEN_VALUE);

            // Redis에 저장되는지 확인
            verify(refreshTokenCacheService).saveRefreshToken(anyString(), any(RefreshTokenCacheService.RefreshTokenData.class));

            // DB에도 저장되는지 확인 (호환성)
            verify(tokenRepository).save(any(Token.class));
            verify(jwtTokenProvider).generateToken(user.getId());
        }

        @Test
        @DisplayName("토큰 생성 시 만료된 토큰들을 정리한다")
        void createRefreshToken_CleansUpExpiredTokens() {
            // given
            User user = AuthTestHelper.createUser();
            Token newToken = AuthTestHelper.createValidToken(user, REFRESH_TOKEN_VALUE);

            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(REFRESH_TOKEN_VALUE);
            given(tokenRepository.save(any(Token.class)))
                    .willReturn(newToken);

            // when
            tokenService.createRefreshToken(user);

            // then
            // cleanupExpiredTokensForUser 메서드가 호출되는지 확인 (내부 로직)
            verify(refreshTokenCacheService).saveRefreshToken(anyString(), any(RefreshTokenCacheService.RefreshTokenData.class));
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
            RefreshTokenCacheService.RefreshTokenData tokenData =
                    RefreshTokenCacheService.RefreshTokenData.builder()
                            .userId(user.getId())
                            .token(VALID_REFRESH_TOKEN)
                            .createdAt(LocalDateTime.now())
                            .expiredAt(LocalDateTime.now().plusDays(14))
                            .isRevoked(false)
                            .build();

            given(refreshTokenCacheService.isValidToken(anyString()))
                    .willReturn(true);
            given(refreshTokenCacheService.getRefreshToken(anyString()))
                    .willReturn(tokenData);
            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(NEW_ACCESS_TOKEN);
            given(jwtTokenProvider.getExpirationMs())
                    .willReturn(TEST_EXPIRATION_MS);

            // when
            RefreshTokenResponse result = tokenService.refreshAccessToken(VALID_REFRESH_TOKEN);

            // then
            assertThat(result.getNewAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);
            assertThat(result.getExpiresIn()).isEqualTo(TEST_EXPIRATION_MS / 1000);

            verify(refreshTokenCacheService).isValidToken(anyString());
            verify(refreshTokenCacheService).getRefreshToken(anyString());
            verify(jwtTokenProvider).generateToken(user.getId());
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refreshAccessToken_WithInvalidToken_ThrowsException() {
            // given
            given(refreshTokenCacheService.isValidToken(anyString()))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> tokenService.refreshAccessToken(NON_EXISTENT_TOKEN_VALUE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리프레시 토큰이 유효하지 않습니다");
        }

        @Test
        @DisplayName("토큰 데이터를 찾을 수 없는 경우 예외가 발생한다")
        void refreshAccessToken_WithTokenDataNotFound_ThrowsException() {
            // given
            given(refreshTokenCacheService.isValidToken(anyString()))
                    .willReturn(true);
            given(refreshTokenCacheService.getRefreshToken(anyString()))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> tokenService.refreshAccessToken(VALID_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리프레시 토큰을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("invalidateRefreshToken 메서드 테스트")
    class InvalidateRefreshTokenTest {

        @Test
        @DisplayName("리프레시 토큰을 Redis와 DB에서 무효화한다")
        void invalidateRefreshToken_InvalidatesTokenInRedisAndDB() {
            // given
            User user = AuthTestHelper.createUser();
            Token refreshToken = AuthTestHelper.createValidToken(user, VALID_REFRESH_TOKEN);

            given(tokenRepository.findByToken(VALID_REFRESH_TOKEN))
                    .willReturn(Optional.of(refreshToken));

            // when
            tokenService.invalidateRefreshToken(VALID_REFRESH_TOKEN);

            // then
            // Redis에서 블랙리스트 추가 및 삭제
            verify(refreshTokenCacheService).blacklistToken(anyString());
            verify(refreshTokenCacheService).deleteRefreshToken(anyString());

            // DB에서도 무효화 (호환성)
            verify(tokenRepository).save(eq(refreshToken));
        }

        @Test
        @DisplayName("DB에서 토큰을 찾지 못해도 Redis 무효화는 진행한다")
        void invalidateRefreshToken_WithNonExistentTokenInDB_StillInvalidatesRedis() {
            // given
            given(tokenRepository.findByToken(NON_EXISTENT_TOKEN_VALUE))
                    .willReturn(Optional.empty());

            // when
            tokenService.invalidateRefreshToken(NON_EXISTENT_TOKEN_VALUE);

            // then
            // Redis 무효화는 실행됨
            verify(refreshTokenCacheService).blacklistToken(anyString());
            verify(refreshTokenCacheService).deleteRefreshToken(anyString());

            // DB 저장은 실행되지 않음
            verify(tokenRepository, never()).save(any(Token.class));
        }
    }

    @Nested
    @DisplayName("invalidateUserTokens 메서드 테스트")
    class InvalidateUserTokensTest {

        @Test
        @DisplayName("사용자의 모든 토큰을 Redis와 DB에서 무효화한다")
        void invalidateUserTokens_InvalidatesAllUserTokensInRedisAndDB() {
            // given
            User user = AuthTestHelper.createUser();
            Token token1 = AuthTestHelper.createValidToken(user, VALID_TOKEN_VALUE + "-1");
            Token token2 = AuthTestHelper.createValidToken(user, VALID_TOKEN_VALUE + "-2");

            given(tokenRepository.findAllByUser(user))
                    .willReturn(List.of(token1, token2));

            // when
            tokenService.invalidateUserTokens(user);

            // then
            // Redis에서 사용자 토큰 무효화
            verify(refreshTokenCacheService).invalidateUserTokens(user.getId());

            // DB에서도 무효화 (호환성)
            verify(tokenRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("토큰이 없는 사용자의 경우에도 정상 처리된다")
        void invalidateUserTokens_WithNoTokens_HandlesGracefully() {
            // given
            User user = AuthTestHelper.createUser();

            given(tokenRepository.findAllByUser(user))
                    .willReturn(Collections.emptyList());

            // when
            tokenService.invalidateUserTokens(user);

            // then
            // Redis 무효화는 실행됨
            verify(refreshTokenCacheService).invalidateUserTokens(user.getId());

            // DB 저장도 실행됨 (빈 리스트)
            verify(tokenRepository).saveAll(Collections.emptyList());
        }
    }

    @Nested
    @DisplayName("validateRefreshToken 메서드 테스트")
    class ValidateRefreshTokenTest {

        @Test
        @DisplayName("유효한 리프레시 토큰 검증 시 true를 반환한다")
        void validateRefreshToken_WithValidToken_ReturnsTrue() {
            // given
            given(refreshTokenCacheService.isValidToken(anyString()))
                    .willReturn(true);

            // when
            boolean result = tokenService.validateRefreshToken(VALID_REFRESH_TOKEN);

            // then
            assertThat(result).isTrue();
            verify(refreshTokenCacheService).isValidToken(anyString());
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰 검증 시 false를 반환한다")
        void validateRefreshToken_WithInvalidToken_ReturnsFalse() {
            // given
            given(refreshTokenCacheService.isValidToken(anyString()))
                    .willReturn(false);

            // when
            boolean result = tokenService.validateRefreshToken(EXPIRED_REFRESH_TOKEN);

            // then
            assertThat(result).isFalse();
            verify(refreshTokenCacheService).isValidToken(anyString());
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰 검증 시 false를 반환한다")
        void validateRefreshToken_WithNonExistentToken_ReturnsFalse() {
            // given
            given(refreshTokenCacheService.isValidToken(anyString()))
                    .willReturn(false);

            // when
            boolean result = tokenService.validateRefreshToken(NON_EXISTENT_TOKEN_VALUE);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Redis 통합 테스트")
    class RedisIntegrationTest {

        @Test
        @DisplayName("토큰 해시 생성이 일관되게 동작한다")
        void tokenHashGeneration_IsConsistent() {
            // given
            User user = AuthTestHelper.createUser();

            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(REFRESH_TOKEN_VALUE);
            given(tokenRepository.save(any(Token.class)))
                    .willReturn(AuthTestHelper.createValidToken(user, REFRESH_TOKEN_VALUE));

            // when
            tokenService.createRefreshToken(user);

            // then
            // 같은 토큰에 대해 일관된 해시가 생성되는지 확인
            verify(refreshTokenCacheService).saveRefreshToken(anyString(), any(RefreshTokenCacheService.RefreshTokenData.class));
        }

        @Test
        @DisplayName("토큰 데이터가 올바른 형식으로 생성된다")
        void tokenData_IsCreatedWithCorrectFormat() {
            // given
            User user = AuthTestHelper.createUser();

            given(jwtTokenProvider.generateToken(user.getId()))
                    .willReturn(REFRESH_TOKEN_VALUE);
            given(tokenRepository.save(any(Token.class)))
                    .willReturn(AuthTestHelper.createValidToken(user, REFRESH_TOKEN_VALUE));

            // when
            tokenService.createRefreshToken(user);

            // then
            verify(refreshTokenCacheService).saveRefreshToken(
                    anyString(),
                    any(RefreshTokenCacheService.RefreshTokenData.class)
            );
        }
    }
}
