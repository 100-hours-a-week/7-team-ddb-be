package com.dolpin.global.storage.controller;

import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import com.dolpin.global.storage.service.StorageService;
import com.dolpin.global.storage.dto.request.PresignedUrlRequest;
import com.dolpin.global.storage.dto.reseponse.PresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

        log.info("파일: {}, 업로드 타입: {}에 대한 서명된 URL 생성 중",
                presignedUrlRequest.getFileName(), presignedUrlRequest.getUploadType());

        // Spring Security 컨텍스트에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal() == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ResponseStatus.UNAUTHORIZED));
        }

        try {
            // 인증된 사용자 정보에서 ID 추출
            User userDetails = (User) authentication.getPrincipal();
            Long userId = Long.parseLong(userDetails.getUsername());

            PresignedUrlResponse response = storageService.generateSignedUrl(presignedUrlRequest, userId);
            return ResponseEntity.ok(ApiResponse.success("presigned_url_generated_success", response));
        } catch (ClassCastException e) {
            log.error("인증 주체가 User 인스턴스가 아닙니다: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ResponseStatus.UNAUTHORIZED));
        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청 파라미터: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ResponseStatus.INVALID_PARAMETER.withMessage(e.getMessage())));
        } catch (Exception e) {
            log.error("서명된 URL 생성 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ResponseStatus.INTERNAL_SERVER_ERROR));
        }
    }
}