package com.dolpin.domain.auth.controller;

import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.service.auth.AuthService;
import com.dolpin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/oauth")
    public ResponseEntity<ApiResponse<OAuthUrlResponse>> getOAuthLoginUrl(
            @RequestParam(defaultValue = "kakao") String provider) {
        try {
            OAuthUrlResponse response = authService.getOAuthLoginUrl(provider);
            return ResponseEntity.ok(ApiResponse.success("success", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("지원하지 않는 OAuth 제공자입니다: " + provider));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("서버 내부 오류입니다."));
        }
    }
}