package com.dolpin.global.storage.controller;

import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.storage.service.StorageService;
import com.dolpin.global.storage.dto.request.PresignedUrlRequest;
import com.dolpin.global.storage.dto.reseponse.PresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/signed-urls")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generateSignedUrl(
            @Valid @RequestBody PresignedUrlRequest request) {

        log.info("Generating signed URL for file: {}", request.getFileName());

        PresignedUrlResponse response = storageService.generateSignedUrl(request);

        return ResponseEntity.ok(ApiResponse.success("signed_url_generated_success", response));
    }
}