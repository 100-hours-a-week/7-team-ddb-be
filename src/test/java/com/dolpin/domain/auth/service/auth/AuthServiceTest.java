package com.dolpin.domain.auth.service.auth;

import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.dto.response.TokenResponse;
import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import com.dolpin.domain.auth.entity.enums.TokenStatus;
import com.dolpin.domain.auth.service.oauth.OAuthApiClient;
import com.dolpin.domain.auth.service.oauth.OAuthInfoResponse;
import com.dolpin.domain.auth.service.oauth.OAuthLoginUrlProvider;
import com.dolpin.domain.auth.service.oauth.OAuthLoginUrlProviderFactory;
import com.dolpin.domain.auth.service.token.JwtTokenProvider;
import com.dolpin.domain.auth.service.token.TokenService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserCommandService;
import com.dolpin.domain.user.service.UserQueryService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private OAuthLoginUrlProviderFactory oAuthLoginUrlProviderFactory;

    @Mock
    private List<OAuthApiClient> oAuthApiClients;

    @Mock
    private OAuthApiClient kakaoApiClient;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private UserCommandService userCommandService;

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private OAuthLoginUrlProvider oAuthLoginUrlProvider;

    @Mock
    private OAuthInfoResponse oAuthInfoResponse;

    private User createTestUser() {
        return User.builder()
                .id(1L)
                .providerId(12345L)
                .provider("kakao")
                .username("testuser")
                .build();
    }

    private Token createTestToken(User user, String tokenValue) {
        return Token.builder()
                .id(1L)
                .user(user)
                .status(TokenStatus.ACTIVE)
                .token(tokenValue)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusDays(14))
                .isRevoked(false)
                .build();
    }

    @Nested
    @DisplayName("getOAuthLoginUrl 메서드 테스트")
    class GetOAuthLoginUrlTest {

        @Test
        @DisplayName("카카오 OAuth 로그인 URL을 정상적으로 생성한다")
        void getOAuthLoginUrl_WithKakaoProvider_ReturnsLoginUrl() {
            // given
            String provider = "kakao";
            String redirectUri = "http://localhost:3000/auth/callback";
            String expectedUrl = "https://kauth.kakao.com/oauth/authorize?client_id=test&redirect_uri=" + redirectUri + "&response_type=code";

            given(oAuthLoginUrlProviderFactory.getProvider(OAuthProvider.KAKAO))
                    .willReturn(oAuthLoginUrlProvider);
            given(oAuthLoginUrlProvider.getLoginUrl(redirectUri))
                    .willReturn(expectedUrl);

            // when
            OAuthUrlResponse result = authService.getOAuthLoginUrl(provider, redirectUri);

            // then
            assertThat(result.getRedirectUrl()).isEqualTo(expectedUrl);
            verify(oAuthLoginUrlProviderFactory).getProvider(OAuthProvider.KAKAO);
            verify(oAuthLoginUrlProvider).getLoginUrl(redirectUri);
        }

        @Test
        @DisplayName("구글 OAuth 로그인 URL을 정상적으로 생성한다")
        void getOAuthLoginUrl_WithGoogleProvider_ReturnsLoginUrl() {
            // given
            String provider = "google";
            String redirectUri = "http://localhost:3000/auth/callback";
            String expectedUrl = "https://accounts.google.com/oauth/authorize?client_id=test&redirect_uri=" + redirectUri + "&response_type=code";

            given(oAuthLoginUrlProviderFactory.getProvider(OAuthProvider.GOOGLE))
                    .willReturn(oAuthLoginUrlProvider);
            given(oAuthLoginUrlProvider.getLoginUrl(redirectUri))
                    .willReturn(expectedUrl);

            // when
            OAuthUrlResponse result = authService.getOAuthLoginUrl(provider, redirectUri);

            // then
            assertThat(result.getRedirectUrl()).isEqualTo(expectedUrl);
            verify(oAuthLoginUrlProviderFactory).getProvider(OAuthProvider.GOOGLE);
        }

        @Test
        @DisplayName("지원하지 않는 OAuth 제공자 요청 시 예외가 발생한다")
        void getOAuthLoginUrl_WithUnsupportedProvider_ThrowsException() {
            // given
            String unsupportedProvider = "naver";

            // when & then
            assertThatThrownBy(() -> authService.getOAuthLoginUrl(unsupportedProvider, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown OAuth provider: naver");
        }
    }

    @Nested
    @DisplayName("generateTokenByAuthorizationCode 메서드 테스트")
    class GenerateTokenByAuthorizationCodeTest {

        @Test
        @DisplayName("기존 사용자 로그인 시 토큰을 정상적으로 발급한다")
        void generateTokenByAuthorizationCode_WithExistingUser_ReturnsToken() {
            // given
            String authCode = "test-auth-code";
            String redirectUri = "http://localhost:3000/auth/callback";
            String oauthAccessToken = "oauth-access-token";
            String jwtAccessToken = "jwt-access-token";
            String refreshTokenValue = "refresh-token";
            long expirationMs = 3600000L;

            User existingUser = createTestUser();
            Token refreshToken = createTestToken(existingUser, refreshTokenValue);

            // Mock OAuth API Client 찾기
            given(oAuthApiClients.stream()).willReturn(List.of(kakaoApiClient).stream());
            given(kakaoApiClient.oauthProvider()).willReturn(OAuthProvider.KAKAO);

            // Mock OAuth 플로우
            given(kakaoApiClient.requestAccessToken(any()))
                    .willReturn(oauthAccessToken);
            given(kakaoApiClient.requestUserInfo(oauthAccessToken))
                    .willReturn(oAuthInfoResponse);
            given(oAuthInfoResponse.getProvider()).willReturn("kakao");
            given(oAuthInfoResponse.getProviderId()).willReturn("12345");

            // Mock 사용자 조회 (기존 사용자)
            given(userQueryService.findByProviderAndProviderId("kakao", 12345L))
                    .willReturn(Optional.of(existingUser));

            // Mock JWT 토큰 생성
            given(jwtTokenProvider.generateToken(existingUser.getId()))
                    .willReturn(jwtAccessToken);
            given(jwtTokenProvider.getExpirationMs())
                    .willReturn(expirationMs);

            // Mock 리프레시 토큰 생성
            given(tokenService.createRefreshToken(existingUser))
                    .willReturn(refreshToken);

            // when
            TokenResponse result = authService.generateTokenByAuthorizationCode(authCode, redirectUri);

            // then
            assertThat(result.getAccessToken()).isEqualTo(jwtAccessToken);
            assertThat(result.getRefreshToken()).isEqualTo(refreshTokenValue);
            assertThat(result.getExpiresIn()).isEqualTo(expirationMs / 1000);
            assertThat(result.getUser().getId()).isEqualTo(existingUser.getId());
            assertThat(result.isNewUser()).isFalse();

            verify(kakaoApiClient).requestAccessToken(any());
            verify(kakaoApiClient).requestUserInfo(oauthAccessToken);
            verify(userQueryService).findByProviderAndProviderId("kakao", 12345L);
            verify(jwtTokenProvider).generateToken(existingUser.getId());
            verify(tokenService).createRefreshToken(existingUser);
        }

        @Test
        @DisplayName("신규 사용자 가입 시 토큰을 정상적으로 발급한다")
        void generateTokenByAuthorizationCode_WithNewUser_ReturnsTokenAndCreatesUser() {
            // given
            String authCode = "test-auth-code";
            String redirectUri = "http://localhost:3000/auth/callback";
            String oauthAccessToken = "oauth-access-token";
            String jwtAccessToken = "jwt-access-token";
            String refreshTokenValue = "refresh-token";
            long expirationMs = 3600000L;

            User newUser = createTestUser();
            Token refreshToken = createTestToken(newUser, refreshTokenValue);

            // Mock OAuth API Client 찾기
            given(oAuthApiClients.stream()).willReturn(List.of(kakaoApiClient).stream());
            given(kakaoApiClient.oauthProvider()).willReturn(OAuthProvider.KAKAO);

            // Mock OAuth 플로우
            given(kakaoApiClient.requestAccessToken(any()))
                    .willReturn(oauthAccessToken);
            given(kakaoApiClient.requestUserInfo(oauthAccessToken))
                    .willReturn(oAuthInfoResponse);
            given(oAuthInfoResponse.getProvider()).willReturn("kakao");
            given(oAuthInfoResponse.getProviderId()).willReturn("12345");

            // Mock 사용자 조회 (신규 사용자 - 없음)
            given(userQueryService.findByProviderAndProviderId("kakao", 12345L))
                    .willReturn(Optional.empty());

            // Mock 신규 사용자 생성
            given(userCommandService.createUser(oAuthInfoResponse))
                    .willReturn(newUser);

            // Mock JWT 토큰 생성
            given(jwtTokenProvider.generateToken(newUser.getId()))
                    .willReturn(jwtAccessToken);
            given(jwtTokenProvider.getExpirationMs())
                    .willReturn(expirationMs);

            // Mock 리프레시 토큰 생성
            given(tokenService.createRefreshToken(newUser))
                    .willReturn(refreshToken);

            // when
            TokenResponse result = authService.generateTokenByAuthorizationCode(authCode, redirectUri);

            // then
            assertThat(result.getAccessToken()).isEqualTo(jwtAccessToken);
            assertThat(result.getRefreshToken()).isEqualTo(refreshTokenValue);
            assertThat(result.getExpiresIn()).isEqualTo(expirationMs / 1000);
            assertThat(result.getUser().getId()).isEqualTo(newUser.getId());
            assertThat(result.isNewUser()).isTrue();

            verify(userCommandService).createUser(oAuthInfoResponse);
            verify(jwtTokenProvider).generateToken(newUser.getId());
            verify(tokenService).createRefreshToken(newUser);
        }

        @Test
        @DisplayName("OAuth 액세스 토큰 발급 실패 시 예외가 발생한다")
        void generateTokenByAuthorizationCode_WithOAuthTokenFailure_ThrowsException() {
            // given
            String authCode = "invalid-auth-code";
            String redirectUri = "http://localhost:3000/auth/callback";

            // Mock OAuth API Client 찾기
            given(oAuthApiClients.stream()).willReturn(List.of(kakaoApiClient).stream());
            given(kakaoApiClient.oauthProvider()).willReturn(OAuthProvider.KAKAO);

            // Mock OAuth 토큰 발급 실패
            given(kakaoApiClient.requestAccessToken(any()))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.generateTokenByAuthorizationCode(authCode, redirectUri))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("OAuth 인증에 실패했습니다");
        }

        @Test
        @DisplayName("OAuth 액세스 토큰이 빈 문자열인 경우 예외가 발생한다")
        void generateTokenByAuthorizationCode_WithEmptyOAuthToken_ThrowsException() {
            // given
            String authCode = "test-auth-code";
            String redirectUri = "http://localhost:3000/auth/callback";

            // Mock OAuth API Client 찾기
            given(oAuthApiClients.stream()).willReturn(List.of(kakaoApiClient).stream());
            given(kakaoApiClient.oauthProvider()).willReturn(OAuthProvider.KAKAO);

            // Mock OAuth 토큰 발급 실패 (빈 문자열)
            given(kakaoApiClient.requestAccessToken(any()))
                    .willReturn("");

            // when & then
            assertThatThrownBy(() -> authService.generateTokenByAuthorizationCode(authCode, redirectUri))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("OAuth 인증에 실패했습니다");
        }

        @Test
        @DisplayName("지원하지 않는 OAuth 제공자인 경우 예외가 발생한다")
        void generateTokenByAuthorizationCode_WithUnsupportedProvider_ThrowsException() {
            // given
            String authCode = "test-auth-code";
            String redirectUri = "http://localhost:3000/auth/callback";

            // Mock OAuth API Client 찾기 실패 (빈 리스트)
            given(oAuthApiClients.stream()).willReturn(List.<OAuthApiClient>of().stream());

            // when & then
            assertThatThrownBy(() -> authService.generateTokenByAuthorizationCode(authCode, redirectUri))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("refreshToken 메서드 테스트")
    class RefreshTokenTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 액세스 토큰을 갱신한다")
        void refreshToken_WithValidRefreshToken_ReturnsNewAccessToken() {
            // given
            String refreshTokenValue = "valid-refresh-token";
            String newAccessToken = "new-access-token";
            long expirationMs = 3600000L;

            RefreshTokenResponse expectedResponse = RefreshTokenResponse.builder()
                    .newAccessToken(newAccessToken)
                    .expiresIn(expirationMs / 1000)
                    .build();

            given(tokenService.refreshAccessToken(refreshTokenValue))
                    .willReturn(expectedResponse);

            // when
            RefreshTokenResponse result = authService.refreshToken(refreshTokenValue);

            // then
            assertThat(result.getNewAccessToken()).isEqualTo(newAccessToken);
            assertThat(result.getExpiresIn()).isEqualTo(expirationMs / 1000);

            verify(tokenService).refreshAccessToken(refreshTokenValue);
        }

        @Test
        @DisplayName("만료된 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refreshToken_WithExpiredRefreshToken_ThrowsException() {
            // given
            String expiredRefreshToken = "expired-refresh-token";

            given(tokenService.refreshAccessToken(expiredRefreshToken))
                    .willThrow(new BusinessException(ResponseStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."));

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(expiredRefreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리프레시 토큰이 만료되었습니다");

            verify(tokenService).refreshAccessToken(expiredRefreshToken);
        }
    }

    @Nested
    @DisplayName("logout 메서드 테스트")
    class LogoutTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 로그아웃을 정상 처리한다")
        void logout_WithValidRefreshToken_InvalidatesToken() {
            // given
            String refreshTokenValue = "valid-refresh-token";

            // when
            authService.logout(refreshTokenValue);

            // then
            verify(tokenService).invalidateRefreshToken(refreshTokenValue);
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰으로 로그아웃 시 예외가 발생한다")
        void logout_WithNonExistentRefreshToken_ThrowsException() {
            // given
            String nonExistentToken = "non-existent-token";

            willThrow(new BusinessException(ResponseStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."))
                    .given(tokenService).invalidateRefreshToken(nonExistentToken);

            // when & then
            assertThatThrownBy(() -> authService.logout(nonExistentToken))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("리프레시 토큰이 유효하지 않습니다");

            verify(tokenService).invalidateRefreshToken(nonExistentToken);
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("OAuth 로그인부터 토큰 갱신까지 전체 플로우가 정상 동작한다")
        void fullAuthFlow_WorksCorrectly() {
            // given
            String provider = "kakao";
            String authCode = "test-auth-code";
            String redirectUri = "http://localhost:3000/auth/callback";
            String loginUrl = "https://kauth.kakao.com/oauth/authorize?client_id=test&redirect_uri=" + redirectUri + "&response_type=code";
            String oauthAccessToken = "oauth-access-token";
            String jwtAccessToken = "jwt-access-token";
            String refreshTokenValue = "refresh-token";
            String newAccessToken = "new-access-token";
            long expirationMs = 3600000L;

            User user = createTestUser();
            Token refreshToken = createTestToken(user, refreshTokenValue);

            // 1. OAuth URL 생성
            given(oAuthLoginUrlProviderFactory.getProvider(OAuthProvider.KAKAO))
                    .willReturn(oAuthLoginUrlProvider);
            given(oAuthLoginUrlProvider.getLoginUrl(redirectUri))
                    .willReturn(loginUrl);

            // 2. 토큰 발급
            given(oAuthApiClients.stream()).willReturn(List.of(kakaoApiClient).stream());
            given(kakaoApiClient.oauthProvider()).willReturn(OAuthProvider.KAKAO);
            given(kakaoApiClient.requestAccessToken(any())).willReturn(oauthAccessToken);
            given(kakaoApiClient.requestUserInfo(oauthAccessToken)).willReturn(oAuthInfoResponse);
            given(oAuthInfoResponse.getProvider()).willReturn("kakao");
            given(oAuthInfoResponse.getProviderId()).willReturn("12345");
            given(userQueryService.findByProviderAndProviderId("kakao", 12345L))
                    .willReturn(Optional.of(user));
            given(jwtTokenProvider.generateToken(user.getId())).willReturn(jwtAccessToken);
            given(jwtTokenProvider.getExpirationMs()).willReturn(expirationMs);
            given(tokenService.createRefreshToken(user)).willReturn(refreshToken);

            // 3. 토큰 갱신
            RefreshTokenResponse refreshResponse = RefreshTokenResponse.builder()
                    .newAccessToken(newAccessToken)
                    .expiresIn(expirationMs / 1000)
                    .build();
            given(tokenService.refreshAccessToken(refreshTokenValue)).willReturn(refreshResponse);

            // when & then
            // 1. OAuth URL 생성
            OAuthUrlResponse urlResponse = authService.getOAuthLoginUrl(provider, redirectUri);
            assertThat(urlResponse.getRedirectUrl()).isEqualTo(loginUrl);

            // 2. 토큰 발급
            TokenResponse tokenResponse = authService.generateTokenByAuthorizationCode(authCode, redirectUri);
            assertThat(tokenResponse.getAccessToken()).isEqualTo(jwtAccessToken);
            assertThat(tokenResponse.getRefreshToken()).isEqualTo(refreshTokenValue);

            // 3. 토큰 갱신
            RefreshTokenResponse newTokenResponse = authService.refreshToken(refreshTokenValue);
            assertThat(newTokenResponse.getNewAccessToken()).isEqualTo(newAccessToken);

            // 4. 로그아웃
            authService.logout(refreshTokenValue);

            verify(tokenService).invalidateRefreshToken(refreshTokenValue);
        }
    }
}
