package com.dolpin.domain.auth.controller;

import com.dolpin.domain.auth.dto.request.TokenRequest;
import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.dto.response.TokenResponse;
import com.dolpin.domain.auth.service.auth.AuthService;
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

    @GetMapping("/oauth")
    public ResponseEntity<ApiResponse<OAuthUrlResponse>> getOAuthLoginUrl(
            @RequestParam(defaultValue = "kakao") String provider) {
        log.info("oauthProvider : {}", provider);
        OAuthUrlResponse response = authService.getOAuthLoginUrl(provider);
        return ResponseEntity.ok(ApiResponse.success(
                ResponseStatus.SUCCESS.withMessage("소셜 로그인 URL 조회에 성공하였습니다."),
                response
        ));
    }

    // POST로만 처리하도록 변경 (프론트엔드에서 인증 코드를 백엔드에 전달)
    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<TokenResponse>> getTokens(
            @RequestBody TokenRequest request,
            HttpServletResponse response) {
        log.info("Authorization code received in POST request: {}", request.getAuthorizationCode());
        TokenResponse tokenResponse = authService.generateTokenByAuthorizationCode(request.getAuthorizationCode());

        // 리프레시 토큰을 쿠키에 설정
        addRefreshTokenCookie(response, tokenResponse.getRefreshToken());

        // 응답에서 리프레시 토큰 제거 (보안)
        TokenResponse responseWithoutRefreshToken = TokenResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .tokenType(tokenResponse.getTokenType())
                .expiresIn(tokenResponse.getExpiresIn())
                .user(tokenResponse.getUser())
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                "login_success",
                responseWithoutRefreshToken
        ));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new BusinessException(ResponseStatus.UNAUTHORIZED.withMessage("리프레시 토큰이 없습니다."));
        }

        log.info("Refresh token received from cookie");
        RefreshTokenResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(
                "token_refresh_success",
                response
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null && !refreshToken.isEmpty()) {
            log.info("Logout requested with refresh token from cookie");
            authService.logout(refreshToken);
        }

        // 리프레시 토큰 쿠키 삭제
        deleteRefreshTokenCookie(response);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("logout_success", null));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);  // JavaScript에서 접근 불가
        cookie.setSecure(true);    // HTTPS에서만 전송
        cookie.setPath("/");       // 모든 경로에서 접근 가능
        cookie.setMaxAge(14 * 24 * 60 * 60);  // 14일 유효
        response.addCookie(cookie);
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        response.addCookie(cookie);
    }
}