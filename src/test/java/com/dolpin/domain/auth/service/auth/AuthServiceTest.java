package com.dolpin.domain.auth.service.auth;

import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.dto.response.TokenResponse;
import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import com.dolpin.global.helper.AuthTestHelper;
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

import java.util.List;
import java.util.Optional;

import static com.dolpin.global.constants.AuthTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private OAuthApiClient oAuthApiClient;

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

    @Nested
    @DisplayName("getOAuthLoginUrl 메서드 테스트")
    class GetOAuthLoginUrlTest {

        @Test
        @DisplayName("카카오 OAuth 로그인 URL을 정상적으로 생성한다")
        void getOAuthLoginUrl_WithKakaoProvider_ReturnsLoginUrl() {
            // given
            String expectedUrl = String.format(KAKAO_LOGIN_URL, TEST_REDIRECT_URI);

            given(oAuthLoginUrlProviderFactory.getProvider(OAuthProvider.KAKAO))
                    .willReturn(oAuthLoginUrlProvider);
            given(oAuthLoginUrlProvider.getLoginUrl(TEST_REDIRECT_URI))
                    .willReturn(expectedUrl);

            // when
            OAuthUrlResponse result = authService.getOAuthLoginUrl(KAKAO_PROVIDER, TEST_REDIRECT_URI);

            // then
            assertThat(result.getRedirectUrl()).isEqualTo(expectedUrl);
            verify(oAuthLoginUrlProviderFactory).getProvider(OAuthProvider.KAKAO);
            verify(oAuthLoginUrlProvider).getLoginUrl(TEST_REDIRECT_URI);
        }

        @Test
        @DisplayName("구글 OAuth 로그인 URL을 정상적으로 생성한다")
        void getOAuthLoginUrl_WithGoogleProvider_ReturnsLoginUrl() {
            // given
            String expectedUrl = String.format(GOOGLE_LOGIN_URL, TEST_REDIRECT_URI);

            given(oAuthLoginUrlProviderFactory.getProvider(OAuthProvider.GOOGLE))
                    .willReturn(oAuthLoginUrlProvider);
            given(oAuthLoginUrlProvider.getLoginUrl(TEST_REDIRECT_URI))
                    .willReturn(expectedUrl);

            // when
            OAuthUrlResponse result = authService.getOAuthLoginUrl(GOOGLE_PROVIDER, TEST_REDIRECT_URI);

            // then
            assertThat(result.getRedirectUrl()).isEqualTo(expectedUrl);
            verify(oAuthLoginUrlProviderFactory).getProvider(OAuthProvider.GOOGLE);
        }

        @Test
        @DisplayName("지원하지 않는 OAuth 제공자 요청 시 예외가 발생한다")
        void getOAuthLoginUrl_WithUnsupportedProvider_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> authService.getOAuthLoginUrl(UNSUPPORTED_PROVIDER, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(UNKNOWN_OAUTH_PROVIDER_MESSAGE + UNSUPPORTED_PROVIDER);
        }
    }

    @Nested
    @DisplayName("generateTokenByAuthorizationCode 메서드 테스트")
    class GenerateTokenByAuthorizationCodeTest {

        @Test
        @DisplayName("기존 사용자 로그인 시 토큰을 정상적으로 발급한다")
        void generateTokenByAuthorizationCode_WithExistingUser_ReturnsToken() {
            // given
            User existingUser = AuthTestHelper.createUser();
            Token refreshToken = AuthTestHelper.createValidToken(existingUser, REFRESH_TOKEN_VALUE);

            setupOAuthApiClientMocks();
            setupOAuthFlowMocks();
            setupExistingUserMocks(existingUser);
            setupTokenGenerationMocks(existingUser, refreshToken);

            // when
            TokenResponse result = authService.generateTokenByAuthorizationCode(TEST_AUTH_CODE, TEST_REDIRECT_URI);

            // then
            assertThat(result.getAccessToken()).isEqualTo(JWT_ACCESS_TOKEN);
            assertThat(result.getRefreshToken()).isEqualTo(REFRESH_TOKEN_VALUE);
            assertThat(result.getExpiresIn()).isEqualTo(TEST_EXPIRATION_MS / 1000);
            assertThat(result.getUser().getId()).isEqualTo(existingUser.getId());
            assertThat(result.isNewUser()).isFalse();

            verifyOAuthFlow();
            verify(userQueryService).findByProviderAndProviderId(KAKAO_PROVIDER, TEST_PROVIDER_ID);
            verifyTokenGeneration(existingUser);
        }

        @Test
        @DisplayName("신규 사용자 가입 시 토큰을 정상적으로 발급한다")
        void generateTokenByAuthorizationCode_WithNewUser_ReturnsTokenAndCreatesUser() {
            // given
            User newUser = AuthTestHelper.createUser();
            Token refreshToken = AuthTestHelper.createValidToken(newUser, REFRESH_TOKEN_VALUE);

            setupOAuthApiClientMocks();
            setupOAuthFlowMocks();
            setupNewUserMocks(newUser);
            setupTokenGenerationMocks(newUser, refreshToken);

            // when
            TokenResponse result = authService.generateTokenByAuthorizationCode(TEST_AUTH_CODE, TEST_REDIRECT_URI);

            // then
            assertThat(result.getAccessToken()).isEqualTo(JWT_ACCESS_TOKEN);
            assertThat(result.getRefreshToken()).isEqualTo(REFRESH_TOKEN_VALUE);
            assertThat(result.getExpiresIn()).isEqualTo(TEST_EXPIRATION_MS / 1000);
            assertThat(result.getUser().getId()).isEqualTo(newUser.getId());
            assertThat(result.isNewUser()).isTrue();

            verify(userCommandService).createUser(oAuthInfoResponse);
            verifyTokenGeneration(newUser);
        }

        @Test
        @DisplayName("OAuth 액세스 토큰 발급 실패 시 예외가 발생한다")
        void generateTokenByAuthorizationCode_WithOAuthTokenFailure_ThrowsException() {
            // given
            setupOAuthApiClientMocks();
            given(oAuthApiClient.requestAccessToken(any())).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.generateTokenByAuthorizationCode(INVALID_AUTH_CODE, TEST_REDIRECT_URI))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(OAUTH_AUTH_FAILED_MESSAGE);
        }

        @Test
        @DisplayName("OAuth 액세스 토큰이 빈 문자열인 경우 예외가 발생한다")
        void generateTokenByAuthorizationCode_WithEmptyOAuthToken_ThrowsException() {
            // given
            setupOAuthApiClientMocks();
            given(oAuthApiClient.requestAccessToken(any())).willReturn("");

            // when & then
            assertThatThrownBy(() -> authService.generateTokenByAuthorizationCode(TEST_AUTH_CODE, TEST_REDIRECT_URI))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(OAUTH_AUTH_FAILED_MESSAGE);
        }

        @Test
        @DisplayName("지원하지 않는 OAuth 제공자인 경우 예외가 발생한다")
        void generateTokenByAuthorizationCode_WithUnsupportedProvider_ThrowsException() {
            // given
            given(oAuthApiClients.stream()).willReturn(List.<OAuthApiClient>of().stream());

            // when & then
            assertThatThrownBy(() -> authService.generateTokenByAuthorizationCode(TEST_AUTH_CODE, TEST_REDIRECT_URI))
                    .isInstanceOf(BusinessException.class);
        }

        private void setupOAuthApiClientMocks() {
            given(oAuthApiClients.stream()).willReturn(List.of(oAuthApiClient).stream());
            given(oAuthApiClient.oauthProvider()).willReturn(OAuthProvider.KAKAO);
        }

        private void setupOAuthFlowMocks() {
            given(oAuthApiClient.requestAccessToken(any())).willReturn(OAUTH_ACCESS_TOKEN);
            given(oAuthApiClient.requestUserInfo(OAUTH_ACCESS_TOKEN)).willReturn(oAuthInfoResponse);
            given(oAuthInfoResponse.getProvider()).willReturn(KAKAO_PROVIDER);
            given(oAuthInfoResponse.getProviderId()).willReturn(TEST_PROVIDER_ID.toString());
        }

        private void setupExistingUserMocks(User existingUser) {
            given(userQueryService.findByProviderAndProviderId(KAKAO_PROVIDER, TEST_PROVIDER_ID))
                    .willReturn(Optional.of(existingUser));
        }

        private void setupNewUserMocks(User newUser) {
            given(userQueryService.findByProviderAndProviderId(KAKAO_PROVIDER, TEST_PROVIDER_ID))
                    .willReturn(Optional.empty());
            given(userCommandService.createUser(oAuthInfoResponse)).willReturn(newUser);
        }

        private void setupTokenGenerationMocks(User user, Token refreshToken) {
            given(jwtTokenProvider.generateToken(user.getId())).willReturn(JWT_ACCESS_TOKEN);
            given(jwtTokenProvider.getExpirationMs()).willReturn(TEST_EXPIRATION_MS);
            given(tokenService.createRefreshToken(user)).willReturn(refreshToken);
        }

        private void verifyOAuthFlow() {
            verify(oAuthApiClient).requestAccessToken(any());
            verify(oAuthApiClient).requestUserInfo(OAUTH_ACCESS_TOKEN);
        }

        private void verifyTokenGeneration(User user) {
            verify(jwtTokenProvider).generateToken(user.getId());
            verify(tokenService).createRefreshToken(user);
        }
    }

    @Nested
    @DisplayName("refreshToken 메서드 테스트")
    class RefreshTokenTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 액세스 토큰을 갱신한다")
        void refreshToken_WithValidRefreshToken_ReturnsNewAccessToken() {
            // given
            RefreshTokenResponse expectedResponse = RefreshTokenResponse.builder()
                    .newAccessToken(NEW_ACCESS_TOKEN)
                    .expiresIn(TEST_EXPIRATION_MS / 1000)
                    .build();

            given(tokenService.refreshAccessToken(VALID_REFRESH_TOKEN))
                    .willReturn(expectedResponse);

            // when
            RefreshTokenResponse result = authService.refreshToken(VALID_REFRESH_TOKEN);

            // then
            assertThat(result.getNewAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);
            assertThat(result.getExpiresIn()).isEqualTo(TEST_EXPIRATION_MS / 1000);

            verify(tokenService).refreshAccessToken(VALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("만료된 리프레시 토큰으로 갱신 시 예외가 발생한다")
        void refreshToken_WithExpiredRefreshToken_ThrowsException() {
            // given
            given(tokenService.refreshAccessToken(EXPIRED_REFRESH_TOKEN))
                    .willThrow(new BusinessException(ResponseStatus.UNAUTHORIZED, REFRESH_TOKEN_EXPIRED_MESSAGE));

            // when & then
            assertThatThrownBy(() -> authService.refreshToken(EXPIRED_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(REFRESH_TOKEN_EXPIRED_MESSAGE);

            verify(tokenService).refreshAccessToken(EXPIRED_REFRESH_TOKEN);
        }
    }

    @Nested
    @DisplayName("logout 메서드 테스트")
    class LogoutTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 로그아웃을 정상 처리한다")
        void logout_WithValidRefreshToken_InvalidatesToken() {
            // when
            authService.logout(VALID_REFRESH_TOKEN);

            // then
            verify(tokenService).invalidateRefreshToken(VALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰으로 로그아웃 시 예외가 발생한다")
        void logout_WithNonExistentRefreshToken_ThrowsException() {
            // given
            willThrow(new BusinessException(ResponseStatus.UNAUTHORIZED, REFRESH_TOKEN_INVALID_MESSAGE))
                    .given(tokenService).invalidateRefreshToken(NON_EXISTENT_TOKEN_VALUE);

            // when & then
            assertThatThrownBy(() -> authService.logout(NON_EXISTENT_TOKEN_VALUE))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(REFRESH_TOKEN_INVALID_MESSAGE);

            verify(tokenService).invalidateRefreshToken(NON_EXISTENT_TOKEN_VALUE);
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("OAuth 로그인부터 토큰 갱신까지 전체 플로우가 정상 동작한다")
        void fullAuthFlow_WorksCorrectly() {
            // given
            String loginUrl = String.format(KAKAO_LOGIN_URL, TEST_REDIRECT_URI);
            User user = AuthTestHelper.createUser();
            Token refreshToken = AuthTestHelper.createValidToken(user, REFRESH_TOKEN_VALUE);

            setupFullFlowMocks(loginUrl, user, refreshToken);

            // when & then
            // 1. OAuth URL 생성
            OAuthUrlResponse urlResponse = authService.getOAuthLoginUrl(KAKAO_PROVIDER, TEST_REDIRECT_URI);
            assertThat(urlResponse.getRedirectUrl()).isEqualTo(loginUrl);

            // 2. 토큰 발급
            TokenResponse tokenResponse = authService.generateTokenByAuthorizationCode(TEST_AUTH_CODE, TEST_REDIRECT_URI);
            assertThat(tokenResponse.getAccessToken()).isEqualTo(JWT_ACCESS_TOKEN);
            assertThat(tokenResponse.getRefreshToken()).isEqualTo(REFRESH_TOKEN_VALUE);

            // 3. 토큰 갱신
            RefreshTokenResponse newTokenResponse = authService.refreshToken(REFRESH_TOKEN_VALUE);
            assertThat(newTokenResponse.getNewAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);

            // 4. 로그아웃
            authService.logout(REFRESH_TOKEN_VALUE);

            verify(tokenService).invalidateRefreshToken(REFRESH_TOKEN_VALUE);
        }

        private void setupFullFlowMocks(String loginUrl, User user, Token refreshToken) {
            // OAuth URL 생성 설정
            given(oAuthLoginUrlProviderFactory.getProvider(OAuthProvider.KAKAO))
                    .willReturn(oAuthLoginUrlProvider);
            given(oAuthLoginUrlProvider.getLoginUrl(TEST_REDIRECT_URI))
                    .willReturn(loginUrl);

            // 토큰 발급 설정
            given(oAuthApiClients.stream()).willReturn(List.of(oAuthApiClient).stream());
            given(oAuthApiClient.oauthProvider()).willReturn(OAuthProvider.KAKAO);
            given(oAuthApiClient.requestAccessToken(any())).willReturn(OAUTH_ACCESS_TOKEN);
            given(oAuthApiClient.requestUserInfo(OAUTH_ACCESS_TOKEN)).willReturn(oAuthInfoResponse);
            given(oAuthInfoResponse.getProvider()).willReturn(KAKAO_PROVIDER);
            given(oAuthInfoResponse.getProviderId()).willReturn(TEST_PROVIDER_ID.toString());
            given(userQueryService.findByProviderAndProviderId(KAKAO_PROVIDER, TEST_PROVIDER_ID))
                    .willReturn(Optional.of(user));
            given(jwtTokenProvider.generateToken(user.getId())).willReturn(JWT_ACCESS_TOKEN);
            given(jwtTokenProvider.getExpirationMs()).willReturn(TEST_EXPIRATION_MS);
            given(tokenService.createRefreshToken(user)).willReturn(refreshToken);

            // 토큰 갱신 설정
            RefreshTokenResponse refreshResponse = RefreshTokenResponse.builder()
                    .newAccessToken(NEW_ACCESS_TOKEN)
                    .expiresIn(TEST_EXPIRATION_MS / 1000)
                    .build();
            given(tokenService.refreshAccessToken(REFRESH_TOKEN_VALUE)).willReturn(refreshResponse);
        }
    }
}
