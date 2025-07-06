package com.dolpin.domain.auth.controller;

import com.dolpin.domain.auth.dto.request.TokenRequest;
import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.dto.response.TokenResponse;
import com.dolpin.domain.auth.service.auth.AuthService;
import com.dolpin.domain.auth.service.cookie.CookieService;
import com.dolpin.global.constants.AuthTestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.exception.GlobalExceptionHandler;
import com.dolpin.global.response.ResponseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @Mock
    private CookieService cookieService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("OAuth 로그인 URL 조회")
    class GetOAuthLoginUrlTest {

        @Test
        @DisplayName("성공: 기본 provider(kakao)로 OAuth URL 조회")
        void getOAuthLoginUrl_Success_WithDefaultProvider() throws Exception {
            // given
            OAuthUrlResponse response = new OAuthUrlResponse(
                    String.format(AuthTestConstants.KAKAO_LOGIN_URL, AuthTestConstants.TEST_REDIRECT_URI)
            );
            given(authService.getOAuthLoginUrl(eq(AuthTestConstants.KAKAO_PROVIDER), isNull()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/auth/oauth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("소셜 로그인 URL 조회에 성공하였습니다."))
                    .andExpect(jsonPath("$.data.redirectUrl").exists());

            verify(authService).getOAuthLoginUrl(AuthTestConstants.KAKAO_PROVIDER, null);
        }

        @Test
        @DisplayName("성공: 특정 provider와 redirect_uri로 OAuth URL 조회")
        void getOAuthLoginUrl_Success_WithSpecificProviderAndRedirectUri() throws Exception {
            // given
            String provider = AuthTestConstants.GOOGLE_PROVIDER;
            String redirectUri = AuthTestConstants.TEST_REDIRECT_URI;

            OAuthUrlResponse response = new OAuthUrlResponse(
                    String.format(AuthTestConstants.GOOGLE_LOGIN_URL, redirectUri)
            );
            given(authService.getOAuthLoginUrl(provider, redirectUri))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/auth/oauth")
                            .param("provider", provider)
                            .param("redirect_uri", redirectUri))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("소셜 로그인 URL 조회에 성공하였습니다."))
                    .andExpect(jsonPath("$.data.redirectUrl").exists());

            verify(authService).getOAuthLoginUrl(provider, redirectUri);
        }

        @Test
        @DisplayName("실패: 지원하지 않는 provider")
        void getOAuthLoginUrl_Fail_UnsupportedProvider() throws Exception {
            // given
            String unsupportedProvider = AuthTestConstants.UNSUPPORTED_PROVIDER;
            given(authService.getOAuthLoginUrl(unsupportedProvider, null))
                    .willThrow(new BusinessException(ResponseStatus.OAUTH_PROVIDER_NOT_EXIST));

            // when & then
            mockMvc.perform(get("/api/v1/auth/oauth")
                            .param("provider", unsupportedProvider))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("지원하지 않는 OAuth 제공자입니다."));

            verify(authService).getOAuthLoginUrl(unsupportedProvider, null);
        }
    }

    @Nested
    @DisplayName("토큰 발급")
    class GetTokensTest {

        private TokenRequest tokenRequest;
        private TokenResponse.UserInfo userInfo;
        private TokenResponse tokenResponse;

        @BeforeEach
        void setUpTokenTests() {
            tokenRequest = new TokenRequest(AuthTestConstants.TEST_AUTH_CODE);

            userInfo = TokenResponse.UserInfo.builder()
                    .id(AuthTestConstants.TEST_USER_ID)
                    .username(AuthTestConstants.TEST_USERNAME)
                    .provider(AuthTestConstants.TEST_PROVIDER)
                    .privacyAgreed(true)
                    .locationAgreed(true)
                    .profileCompleted(true)
                    .build();

            tokenResponse = TokenResponse.builder()
                    .accessToken(AuthTestConstants.JWT_ACCESS_TOKEN)
                    .refreshToken(AuthTestConstants.REFRESH_TOKEN_VALUE)
                    .tokenType("Cookie")
                    .expiresIn(AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN)
                    .user(userInfo)
                    .isNewUser(false)
                    .build();
        }

        @Test
        @DisplayName("성공: 인증 코드로 토큰 발급")
        void getTokens_Success() throws Exception {
            // given
            given(authService.generateTokenByAuthorizationCode(
                    AuthTestConstants.TEST_AUTH_CODE, null))
                    .willReturn(tokenResponse);

            // when & then
            mockMvc.perform(post("/api/v1/auth/tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tokenRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("login_success"))
                    .andExpect(jsonPath("$.data.tokenType").value("Cookie"))
                    .andExpect(jsonPath("$.data.expiresIn").value(AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN))
                    .andExpect(jsonPath("$.data.user.id").value(AuthTestConstants.TEST_USER_ID))
                    .andExpect(jsonPath("$.data.user.username").value(AuthTestConstants.TEST_USERNAME))
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                    .andExpect(jsonPath("$.data.refreshToken").doesNotExist());

            verify(authService).generateTokenByAuthorizationCode(AuthTestConstants.TEST_AUTH_CODE, null);
            verify(cookieService).addAccessTokenCookie(any(), eq(AuthTestConstants.JWT_ACCESS_TOKEN), eq(AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN));
            verify(cookieService).addRefreshTokenCookie(any(), eq(AuthTestConstants.REFRESH_TOKEN_VALUE));
        }

        @Test
        @DisplayName("성공: redirect_uri와 함께 토큰 발급")
        void getTokens_Success_WithRedirectUri() throws Exception {
            // given
            String redirectUri = AuthTestConstants.TEST_REDIRECT_URI;
            given(authService.generateTokenByAuthorizationCode(
                    AuthTestConstants.TEST_AUTH_CODE, redirectUri))
                    .willReturn(tokenResponse);

            // when & then
            mockMvc.perform(post("/api/v1/auth/tokens")
                            .param("redirect_uri", redirectUri)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tokenRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("login_success"));

            verify(authService).generateTokenByAuthorizationCode(AuthTestConstants.TEST_AUTH_CODE, redirectUri);
        }

        @Test
        @DisplayName("실패: 잘못된 인증 코드")
        void getTokens_Fail_InvalidAuthCode() throws Exception {
            // given
            TokenRequest invalidRequest = new TokenRequest(AuthTestConstants.INVALID_AUTH_CODE);
            given(authService.generateTokenByAuthorizationCode(
                    AuthTestConstants.INVALID_AUTH_CODE, null))
                    .willThrow(new BusinessException(ResponseStatus.UNAUTHORIZED,
                            AuthTestConstants.OAUTH_AUTH_FAILED_MESSAGE));

            // when & then
            mockMvc.perform(post("/api/v1/auth/tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(AuthTestConstants.OAUTH_AUTH_FAILED_MESSAGE));

            verify(authService).generateTokenByAuthorizationCode(AuthTestConstants.INVALID_AUTH_CODE, null);
            verify(cookieService, never()).addAccessTokenCookie(any(), anyString(), anyLong());
            verify(cookieService, never()).addRefreshTokenCookie(any(), anyString());
        }

        @Test
        @DisplayName("실패: 빈 요청 본문")
        void getTokens_Fail_EmptyRequestBody() throws Exception {
            // given - null authorizationCode로 인한 예외 상황 모킹
            given(authService.generateTokenByAuthorizationCode(null, null))
                    .willThrow(new BusinessException(ResponseStatus.INVALID_PARAMETER,
                            "인증 코드는 필수입니다."));

            // when & then
            mockMvc.perform(post("/api/v1/auth/tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("인증 코드는 필수입니다."));

            verify(authService).generateTokenByAuthorizationCode(null, null);
        }

        @Test
        @DisplayName("실패: null 인증 코드")
        void getTokens_Fail_NullAuthCode() throws Exception {
            // given
            TokenRequest nullRequest = new TokenRequest(null);
            given(authService.generateTokenByAuthorizationCode(null, null))
                    .willThrow(new BusinessException(ResponseStatus.INVALID_PARAMETER,
                            "인증 코드는 필수입니다."));

            // when & then
            mockMvc.perform(post("/api/v1/auth/tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(nullRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("인증 코드는 필수입니다."));

            verify(authService).generateTokenByAuthorizationCode(null, null);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("성공: 유효한 refresh token으로 로그아웃")
        void logout_Success_WithValidRefreshToken() throws Exception {
            // given
            willDoNothing().given(authService).logout(AuthTestConstants.VALID_REFRESH_TOKEN);

            // when & then
            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(createRefreshTokenCookie(AuthTestConstants.VALID_REFRESH_TOKEN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("logout_success"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(authService).logout(AuthTestConstants.VALID_REFRESH_TOKEN);
            verify(cookieService).deleteRefreshTokenCookie(any());
            verify(cookieService).deleteAccessTokenCookie(any());
        }

        @Test
        @DisplayName("성공: refresh token 없이 로그아웃")
        void logout_Success_WithoutRefreshToken() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("logout_success"));

            verify(authService, never()).logout(anyString());
            verify(cookieService).deleteRefreshTokenCookie(any());
            verify(cookieService).deleteAccessTokenCookie(any());
        }

        @Test
        @DisplayName("성공: 빈 refresh token으로 로그아웃")
        void logout_Success_WithEmptyRefreshToken() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(createRefreshTokenCookie("")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("logout_success"));

            verify(authService, never()).logout(anyString());
            verify(cookieService).deleteRefreshTokenCookie(any());
            verify(cookieService).deleteAccessTokenCookie(any());
        }
    }

    @Nested
    @DisplayName("토큰 갱신")
    class RefreshTokenTest {

        private RefreshTokenResponse refreshTokenResponse;

        @BeforeEach
        void setUpRefreshTests() {
            refreshTokenResponse = RefreshTokenResponse.builder()
                    .newAccessToken(AuthTestConstants.NEW_ACCESS_TOKEN)
                    .expiresIn(AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN)
                    .build();
        }

        @Test
        @DisplayName("성공: 유효한 refresh token으로 토큰 갱신")
        void refreshToken_Success() throws Exception {
            // given
            given(authService.refreshToken(AuthTestConstants.VALID_REFRESH_TOKEN))
                    .willReturn(refreshTokenResponse);

            // when & then
            mockMvc.perform(post("/api/v1/auth/token/refresh")
                            .cookie(createRefreshTokenCookie(AuthTestConstants.VALID_REFRESH_TOKEN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("token_refresh_success"))
                    .andExpect(jsonPath("$.data.expiresIn").value(AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN))
                    .andExpect(jsonPath("$.data.newAccessToken").doesNotExist());

            verify(authService).refreshToken(AuthTestConstants.VALID_REFRESH_TOKEN);
            verify(cookieService).addAccessTokenCookie(any(), eq(AuthTestConstants.NEW_ACCESS_TOKEN), eq(AuthTestConstants.ACCESS_TOKEN_EXPIRES_IN));
        }

        @Test
        @DisplayName("실패: refresh token이 없음")
        void refreshToken_Fail_NoRefreshToken() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/auth/token/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("리프레시 토큰이 없습니다."));

            verify(authService, never()).refreshToken(anyString());
            verify(cookieService, never()).addAccessTokenCookie(any(), anyString(), anyLong());
        }

        @Test
        @DisplayName("실패: 빈 refresh token")
        void refreshToken_Fail_EmptyRefreshToken() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/auth/token/refresh")
                            .cookie(createRefreshTokenCookie("")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("리프레시 토큰이 없습니다."));

            verify(authService, never()).refreshToken(anyString());
        }

        @Test
        @DisplayName("실패: 만료된 refresh token")
        void refreshToken_Fail_ExpiredRefreshToken() throws Exception {
            // given
            given(authService.refreshToken(AuthTestConstants.EXPIRED_REFRESH_TOKEN))
                    .willThrow(new BusinessException(ResponseStatus.UNAUTHORIZED,
                            AuthTestConstants.REFRESH_TOKEN_EXPIRED_MESSAGE));

            // when & then
            mockMvc.perform(post("/api/v1/auth/token/refresh")
                            .cookie(createRefreshTokenCookie(AuthTestConstants.EXPIRED_REFRESH_TOKEN)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(AuthTestConstants.REFRESH_TOKEN_EXPIRED_MESSAGE));

            verify(authService).refreshToken(AuthTestConstants.EXPIRED_REFRESH_TOKEN);
            verify(cookieService, never()).addAccessTokenCookie(any(), anyString(), anyLong());
        }

        @Test
        @DisplayName("실패: 유효하지 않은 refresh token")
        void refreshToken_Fail_InvalidRefreshToken() throws Exception {
            // given
            String invalidToken = AuthTestConstants.INVALID_TOKEN_FORMAT;
            given(authService.refreshToken(invalidToken))
                    .willThrow(new BusinessException(ResponseStatus.UNAUTHORIZED,
                            AuthTestConstants.REFRESH_TOKEN_INVALID_MESSAGE));

            // when & then
            mockMvc.perform(post("/api/v1/auth/token/refresh")
                            .cookie(createRefreshTokenCookie(invalidToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(AuthTestConstants.REFRESH_TOKEN_INVALID_MESSAGE));

            verify(authService).refreshToken(invalidToken);
        }
    }

    private jakarta.servlet.http.Cookie createRefreshTokenCookie(String value) {
        return new jakarta.servlet.http.Cookie("refresh_token", value);
    }
}
