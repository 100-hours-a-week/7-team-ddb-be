package com.dolpin.domain.user.controller;

import com.dolpin.domain.user.dto.request.AgreementRequest;
import com.dolpin.domain.user.dto.request.UserRegisterRequest;
import com.dolpin.domain.user.dto.response.MyProfileResponse;
import com.dolpin.domain.user.dto.response.UserProfileResponse;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserCommandService;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.response.ApiResponse;
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
        log.info("Registering user profile for user {}: nickname={}", userId, request.getNickname());

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
        log.info("Getting profile for current user {}", userId);

        User user= userQueryService.getUserById(userId);
        MyProfileResponse response = MyProfileResponse.from(user);

        return ResponseEntity.ok(ApiResponse.success("retrieve_user_info_success", response));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable Long userId) {
        log.info("Getting profile for user {}", userId);

        User user = userQueryService.getUserById(userId);
        UserProfileResponse response = UserProfileResponse.from(user);

        return ResponseEntity.ok(ApiResponse.success("retrieve_user_info_success", response));
    }

    @GetMapping("/test/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> testGetUserProfile(
            @PathVariable Long userId) {

        log.info("Test getting profile for user: {}", userId);

        User user = userQueryService.getUserById(userId);
        UserProfileResponse response = UserProfileResponse.from(user);

        return ResponseEntity.ok(ApiResponse.success("retrieve_user_info_success", response));
    }
}
