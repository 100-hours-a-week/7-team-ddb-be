package com.dolpin.global.storage.controller;

import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import com.dolpin.global.storage.service.StorageService;
import com.dolpin.global.storage.dto.request.PresignedUrlRequest;
import com.dolpin.global.storage.dto.reseponse.PresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gcs")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/presigned-urls")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generateSignedUrl(
            @Valid @RequestBody PresignedUrlRequest presignedUrlRequest) {

        Long userId = getCurrentUserId();
        PresignedUrlResponse response = storageService.generateSignedUrl(presignedUrlRequest, userId);
        return ResponseEntity.ok(ApiResponse.success("presigned_url_generated_success", response));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal() == null) {
            throw new BusinessException(ResponseStatus.UNAUTHORIZED);
        }

        User userDetails = (User) authentication.getPrincipal();
        return Long.parseLong(userDetails.getUsername());
    }
}
