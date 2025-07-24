package com.dolpin.domain.auth.controller;

import com.dolpin.domain.auth.dto.request.TokenRequest;
import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.dto.response.SessionValidationResponse;
import com.dolpin.domain.auth.dto.response.TokenResponse;
import com.dolpin.domain.auth.service.auth.AuthService;
import com.dolpin.domain.auth.service.cookie.CookieService;
import com.dolpin.domain.auth.service.session.SessionValidationService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;
    private final SessionValidationService sessionValidationService;

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
            @RequestParam(required = false) String redirect_uri,
            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.generateTokenByAuthorizationCode(request.getAuthorizationCode(), redirect_uri);

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

    /**
     * 세션 유효성 검증
     */
    @GetMapping("/session")
    public ResponseEntity<ApiResponse<SessionValidationResponse>> validateSession() {
        log.debug("세션 유효성 검증 요청");

        try {
            boolean isValid = sessionValidationService.validateCurrentSession();
            SessionValidationResponse response = SessionValidationResponse.of(isValid);

            return ResponseEntity.ok(
                    ApiResponse.success("valid_session_success", response)
            );

        } catch (BusinessException e) {
            log.warn("세션 검증 중 비즈니스 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("세션 검증 중 예상치 못한 오류 발생", e);
            throw new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류입니다.");
        }
    }

}
