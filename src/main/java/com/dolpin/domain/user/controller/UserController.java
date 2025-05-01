package com.dolpin.domain.user.controller;

import com.dolpin.domain.user.dto.request.AgreementRequest;
import com.dolpin.domain.user.service.UserCommandService;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.security.CurrentUser;
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
    public ResponseEntity<ApiResponse<Void>> saveAgreement (
            @CurrentUser UserDetails userDetails,
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

}
