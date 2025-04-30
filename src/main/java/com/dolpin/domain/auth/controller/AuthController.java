package com.dolpin.domain.auth.controller;

import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.service.auth.AuthService;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/tokens")
    public ResponseEntity<ApiResponse<String>> getToken(@RequestParam String code) {
        log.info("Received authorization code: {}", code);

        // 개발 초기 단계에서 간단한 응답 반환
        return ResponseEntity.ok(ApiResponse.success(
                ResponseStatus.SUCCESS.withMessage("로그인 성공"),
                "코드: " + code + " (토큰 발급 로직 구현 예정)"
        ));

        // 실제 구현 시에는 다음과 같이 서비스 메서드 호출
        // TokenResponse tokenResponse = authService.getToken(code);
        // return ResponseEntity.ok(ApiResponse.success(ResponseStatus.SUCCESS, tokenResponse));
    }
}