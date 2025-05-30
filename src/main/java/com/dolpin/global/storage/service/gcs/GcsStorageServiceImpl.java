package com.dolpin.global.storage.service.gcs;

import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserCommandService;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.storage.service.StorageService;
import com.dolpin.global.storage.dto.request.PresignedUrlRequest;
import com.dolpin.global.storage.dto.reseponse.PresignedUrlResponse;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "gcs")
@RequiredArgsConstructor
public class GcsStorageServiceImpl implements StorageService {

    private final Storage storage;
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    @Value("${gcs.custom-domain}")
    private String customDomain;

    private static final int EXPIRATION_TIME_MINUTES = 15;

    @Override
    @Transactional
    public PresignedUrlResponse generateSignedUrl(PresignedUrlRequest request, Long userId) {
        validateRequest(request);

        // 사용자 존재 확인
        userQueryService.getUserById(userId);

        String objectPath = generateObjectPath(request, userId);

        // 실제 버킷 이름으로 BlobInfo 생성
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectPath)
                .setContentType(request.getContentType())
                .build();

        // 실제 버킷 이름으로 서명된 URL 생성
        URL signedUrl = storage.signUrl(
                blobInfo,
                EXPIRATION_TIME_MINUTES,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature()
        );

        // 커스텀 도메인으로 객체 URL 생성
        String objectUrl = String.format("https://%s/%s", customDomain, objectPath);

        // 프로필 이미지 업데이트
        if ("profile".equalsIgnoreCase(request.getUploadType())) {
            // 현재 사용자 정보 가져오기
            User user = userQueryService.getUserById(userId);
            // 기존 username과 introduction은 유지하면서 이미지 URL만 업데이트
            userCommandService.updateProfile(userId, user.getUsername(), objectUrl, user.getIntroduction());
        }

        return PresignedUrlResponse.builder()
                .signedUrl(signedUrl.toString())
                .objectUrl(objectUrl)
                .expiresIn(EXPIRATION_TIME_MINUTES * 60)
                .build();
    }

    // 나머지 메서드는 그대로 유지
    private void validateRequest(PresignedUrlRequest request) {
        // 업로드 타입 검증
        String uploadType = request.getUploadType().toLowerCase();
        if (!uploadType.equals("profile")) {
            throw new IllegalArgumentException("잘못된 업로드 타입: " + request.getUploadType() +
                    ". 현재 'profile' 타입만 지원합니다.");
        }
    }

    @Override
    public void deleteFile(String path) {
        storage.delete(bucketName, path);
    }

    private String generateObjectPath(PresignedUrlRequest request, Long userId) {
        String uniqueFileName = generateUniqueFileName(request.getFileName());

        if ("profile".equalsIgnoreCase(request.getUploadType())) {
            return String.format("profile/u%d/%s", userId, uniqueFileName);
        } else {
            throw new IllegalArgumentException("잘못된 업로드 타입: " + request.getUploadType());
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().substring(0, 8); // 짧은 UUID
        return uuid + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex);
    }
}