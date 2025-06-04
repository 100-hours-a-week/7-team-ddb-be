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
import java.util.Set;
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
    private static final Set<String> SUPPORTED_UPLOAD_TYPES = Set.of("profile", "moment");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    @Override
    @Transactional
    public PresignedUrlResponse generateSignedUrl(PresignedUrlRequest request, Long userId) {
        validateRequest(request);
        userQueryService.getUserById(userId); // 사용자 존재 확인

        String objectPath = generateObjectPath(request, userId);

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectPath)
                .setContentType(request.getContentType())
                .build();

        URL signedUrl = storage.signUrl(
                blobInfo,
                EXPIRATION_TIME_MINUTES,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature()
        );

        String objectUrl = String.format("https://%s/%s", customDomain, objectPath);

        // 프로필 이미지인 경우에만 자동 업데이트
        if ("profile".equalsIgnoreCase(request.getUploadType())) {
            updateUserProfileImage(userId, objectUrl);
        }

        log.info("Generated presigned URL for {} upload: userId={}, path={}",
                request.getUploadType(), userId, objectPath);

        return PresignedUrlResponse.builder()
                .signedUrl(signedUrl.toString())
                .objectUrl(objectUrl)
                .expiresIn(EXPIRATION_TIME_MINUTES * 60)
                .build();
    }

    @Override
    public void deleteFile(String path) {
        try {
            boolean deleted = storage.delete(bucketName, path);
            if (deleted) {
                log.info("파일 삭제 성공: {}", path);
            } else {
                log.warn("파일 삭제 실패 (파일이 존재하지 않음): {}", path);
            }
        } catch (Exception e) {
            log.error("파일 삭제 중 오류 발생: {}", path, e);
            throw new RuntimeException("파일 삭제 실패: " + path, e);
        }
    }

    private void validateRequest(PresignedUrlRequest request) {
        String uploadType = request.getUploadType().toLowerCase();
        if (!SUPPORTED_UPLOAD_TYPES.contains(uploadType)) {
            throw new IllegalArgumentException(
                    String.format("지원하지 않는 업로드 타입: %s. 지원 타입: %s",
                            request.getUploadType(),
                            String.join(", ", SUPPORTED_UPLOAD_TYPES))
            );
        }

        validateImageContentType(request.getContentType());
        validateFileName(request.getFileName());
    }

    private void validateImageContentType(String contentType) {
        if (!ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "지원하지 않는 이미지 형식입니다. 지원 형식: " + String.join(", ", ALLOWED_IMAGE_TYPES)
            );
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 이름이 유효하지 않습니다.");
        }

        String extension = getFileExtension(fileName).toLowerCase();
        Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png", ".webp");
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(
                    "지원하지 않는 파일 확장자입니다. 지원 확장자: " + String.join(", ", allowedExtensions)
            );
        }
    }

    private void updateUserProfileImage(Long userId, String objectUrl) {
        User user = userQueryService.getUserById(userId);
        userCommandService.updateProfile(userId, user.getUsername(), objectUrl, user.getIntroduction());
    }

    private String generateObjectPath(PresignedUrlRequest request, Long userId) {
        String uniqueFileName = generateUniqueFileName(request.getFileName());
        String uploadType = request.getUploadType().toLowerCase();

        return switch (uploadType) {
            case "profile" -> String.format("profile/u%d/%s", userId, uniqueFileName);
            case "moment" -> {
                if (request.getMomentId() != null) {
                    // 기존 moment 수정
                    yield String.format("moment/u%d/m%d/%s", userId, request.getMomentId(), uniqueFileName);
                } else {
                    // 새 moment 생성용 임시 경로
                    yield String.format("moment/u%d/temp/%s", userId, uniqueFileName);
                }
            }
            default -> throw new IllegalArgumentException("지원하지 않는 업로드 타입: " + uploadType);
        };
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().substring(0, 8);
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
