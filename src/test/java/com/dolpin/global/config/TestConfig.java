package com.dolpin.global.config;

import com.dolpin.global.storage.dto.request.PresignedUrlRequest;
import com.dolpin.global.storage.dto.reseponse.PresignedUrlResponse;
import com.dolpin.global.storage.service.StorageService;
import com.google.cloud.storage.Storage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public StorageService mockStorageService() {
        return new StorageService() {
            @Override
            public PresignedUrlResponse generateSignedUrl(PresignedUrlRequest request, Long userId) {
                return PresignedUrlResponse.builder()
                        .signedUrl("https://mock-test.com/signed-url")
                        .objectUrl("https://mock-test.com/object-url")
                        .expiresIn(900)
                        .build();
            }

            @Override
            public void deleteFile(String path) {
                // Mock implementation - do nothing
            }
        };
    }

    @Bean
    @Primary
    public Storage mockStorage() {
        // GCS Storage를 Mock으로 대체하여 인증 오류 방지
        return mock(Storage.class);
    }
}