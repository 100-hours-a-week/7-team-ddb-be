package com.dolpin.global.storage.service;

import com.dolpin.global.storage.dto.request.PresignedUrlRequest;
import com.dolpin.global.storage.dto.reseponse.PresignedUrlResponse;

public interface StorageService {
    PresignedUrlResponse generateSignedUrl(PresignedUrlRequest request, Long userId);
    void deleteFile(String path);
}