package com.dolpin.global.storage.service.s3;

import com.dolpin.global.storage.dto.request.PresignedUrlRequest;
import com.dolpin.global.storage.dto.reseponse.PresignedUrlResponse;
import com.dolpin.global.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${s3.bucket-name}")
    private String bucketName;

    @Value("${s3.custom-domain}")
    private String customDomain;

    private static final int EXPIRATION_SECONDS = 900;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Set<String> SUPPORTED_UPLOAD_TYPES = Set.of("profile", "moment");

    @Override
    public PresignedUrlResponse generateSignedUrl(PresignedUrlRequest request, Long userId) {
        validateRequest(request);

        String objectKey = generateObjectPath(request.getUploadType(), request.getFileName(), userId);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(request.getContentType())
                .acl("bucket-owner-full-control")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofSeconds(EXPIRATION_SECONDS))
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        String objectUrl = String.format("https://%s/%s", customDomain, objectKey);
        return PresignedUrlResponse.builder()
                .signedUrl(presigned.url().toString())
                .objectUrl(objectUrl)
                .expiresIn(EXPIRATION_SECONDS)
                .build();
    }

    @Override
    public void deleteFile(String path) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .build());
            log.info("파일 삭제 성공: {}", path);
        } catch (Exception e) {
            log.error("파일 삭제 중 오류 발생: {}", path, e);
            throw new RuntimeException("파일 삭제 실패", e);
        }
    }

    private void validateRequest(PresignedUrlRequest request) {
        String uploadType = request.getUploadType().toLowerCase();
        if (!SUPPORTED_UPLOAD_TYPES.contains(uploadType)) {
            throw new IllegalArgumentException("지원하지 않는 업로드 타입입니다: " + uploadType);
        }

        String contentType = request.getContentType().toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }

        String fileName = request.getFileName();
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 이름이 유효하지 않습니다.");
        }

        String extension = getFileExtension(fileName);
        Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png", ".webp");
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 확장자입니다.");
        }
    }

    private String generateObjectPath(String uploadType, String originalFileName, Long userId) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFileName);
        return String.format("%s/u%d/%s%s", uploadType.toLowerCase(), userId, uuid, extension);
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex != -1 ? fileName.substring(lastDotIndex).toLowerCase() : "";
    }
}
