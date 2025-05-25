package com.dolpin.domain.auth.controller;

import com.dolpin.domain.auth.dto.request.TokenRequest;
import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.dto.response.TokenResponse;
import com.dolpin.domain.auth.service.auth.AuthService;
import com.dolpin.domain.auth.service.cookie.CookieService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    @GetMapping("/oauth")
    public ResponseEntity<ApiResponse<OAuthUrlResponse>> getOAuthLoginUrl(
            @RequestParam(defaultValue = "kakao") String provider,
            @RequestParam(required = false) String redirect_uri) {
        OAuthUrlResponse response = authService.getOAuthLoginUrl(provider, redirect_uri);
        return ResponseEntity.ok(ApiResponse.success(
                ResponseStatus.SUCCESS.withMessage("소셜 로그인 URL 조회에 성공하였습니다."),
                response
        ));
    }

    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<TokenResponse>> getTokens(
            @RequestBody TokenRequest request,
            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.generateTokenByAuthorizationCode(request.getAuthorizationCode(), null);

        cookieService.addAccessTokenCookie(response, tokenResponse.getAccessToken(), tokenResponse.getExpiresIn());
        cookieService.addRefreshTokenCookie(response, tokenResponse.getRefreshToken());

        // 응답에서 토큰 제거
        TokenResponse responseWithoutTokens = TokenResponse.builder()
                .tokenType(tokenResponse.getTokenType())
                .expiresIn(tokenResponse.getExpiresIn())
                .user(tokenResponse.getUser())
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                "login_success",
                responseWithoutTokens
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null && !refreshToken.isEmpty()) {
            authService.logout(refreshToken);
        }

        cookieService.deleteRefreshTokenCookie(response);
        cookieService.deleteAccessTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success("logout_success", null));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BusinessException(ResponseStatus.UNAUTHORIZED.withMessage("리프레시 토큰이 없습니다."));
        }

        RefreshTokenResponse tokenResponse = authService.refreshToken(refreshToken);

        cookieService.addAccessTokenCookie(response, tokenResponse.getNewAccessToken(), tokenResponse.getExpiresIn());

        RefreshTokenResponse responseWithoutToken = RefreshTokenResponse.builder()
                .expiresIn(tokenResponse.getExpiresIn())
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                "token_refresh_success",
                responseWithoutToken
        ));
    }
}