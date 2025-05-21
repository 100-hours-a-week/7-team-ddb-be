package com.dolpin.domain.user.controller;

import com.dolpin.domain.user.dto.request.AgreementRequest;
import com.dolpin.domain.user.dto.request.UserProfileUpdateRequest;
import com.dolpin.domain.user.dto.request.UserRegisterRequest;
import com.dolpin.domain.user.dto.response.MyProfileResponse;
import com.dolpin.domain.user.dto.response.UserProfileResponse;
import com.dolpin.domain.auth.service.cookie.CookieService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserCommandService;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final CookieService cookieService;

    @PostMapping("/agreement")
    public ResponseEntity<ApiResponse<Void>> saveAgreement(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AgreementRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        userCommandService.agreeToTerms(
                userId,
                request.getPrivacyAgreed(),
                request.getLocationAgreed()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("agreement_saved", null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registerUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserRegisterRequest request) {

        // 현재 인증된 사용자 ID 추출
        Long userId = Long.parseLong(userDetails.getUsername());

        // 사용자 등록 처리
        userCommandService.registerUser(
                userId,
                request.getNickname(),
                request.getProfile_image(),
                request.getIntroduction()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("user_info_saved", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());

        User user= userQueryService.getUserById(userId);
        MyProfileResponse response = MyProfileResponse.from(user);

        return ResponseEntity.ok(ApiResponse.success("retrieve_user_info_success", response));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable Long userId) {

        User user = userQueryService.getUserById(userId);
        UserProfileResponse response = UserProfileResponse.from(user);

        return ResponseEntity.ok(ApiResponse.success("retrieve_user_info_success", response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        // 현재 인증된 사용자 ID 추출
        Long userId = Long.parseLong(userDetails.getUsername());

        // 프로필 업데이트 (서비스에서 닉네임 중복 확인 및 업데이트 처리)
        User updatedUser = userCommandService.updateProfile(
                userId,
                request.getNickname(),
                request.getProfile_image(),
                request.getIntroduction()
        );

        // DTO로 변환하여 응답 생성
        UserProfileResponse response = UserProfileResponse.from(updatedUser);

        return ResponseEntity.ok(ApiResponse.success("user_info_updated", response));
    }


    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        // 현재 인증된 사용자 ID 추출
        Long userId = Long.parseLong(userDetails.getUsername());

        // 1. 사용자 삭제 (서비스에서 토큰 무효화 로직이 포함됨)
        userCommandService.deleteUser(userId);

        // 2. 쿠키 삭제
        cookieService.deleteAccessTokenCookie(response);
        cookieService.deleteRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success("user_delete_success", null));
    }

}
