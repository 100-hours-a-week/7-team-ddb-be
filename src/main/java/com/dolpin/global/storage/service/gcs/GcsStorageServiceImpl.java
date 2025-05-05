package com.dolpin.global.storage.service.gcs;

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

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "gcs")
@RequiredArgsConstructor
public class GcsStorageServiceImpl implements StorageService {

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    private static final int EXPIRATION_TIME_MINUTES = 15;

    @Override
    public PresignedUrlResponse generateSignedUrl(PresignedUrlRequest request) {
        String objectPath = generateObjectPath(request);

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

        String objectUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectPath);

        log.info("Generated signed URL for file: {}", objectPath);

        return PresignedUrlResponse.builder()
                .signedUrl(signedUrl.toString())
                .objectUrl(objectUrl)
                .expiresIn(EXPIRATION_TIME_MINUTES * 60)
                .build();
    }

    @Override
    public String getFileUrl(String path) {
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, path);
    }

    @Override
    public void deleteFile(String path) {
        storage.delete(bucketName, path);
        log.info("Deleted file: {}", path);
    }

    private String generateObjectPath(PresignedUrlRequest request) {
        String uniqueFileName = generateUniqueFileName(request.getFileName());

        switch (request.getUploadType().toLowerCase()) {
            case "profile":
                if (request.getUserId() == null) {
                    throw new IllegalArgumentException("userId is required for profile upload");
                }
                return String.format("profiles/%d/%s", request.getUserId(), uniqueFileName);
            case "moment":
                if (request.getUserId() == null) {
                    throw new IllegalArgumentException("userId is required for moment upload");
                }
                return String.format("moments/%d/%s", request.getUserId(), uniqueFileName);
            case "place":
                return String.format("places/%s", uniqueFileName);
            default:
                return String.format("misc/%s", uniqueFileName);
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString();
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