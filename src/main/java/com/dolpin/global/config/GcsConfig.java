package com.dolpin.global.config;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class GcsConfig {

    @Value("${gcs.project-id:}")
    private String projectId;

    @Value("${gcs.credentials-path:}")
    private String credentialsPath;

    @Bean
    public Storage storage() throws IOException {
        Credentials credentials;

        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
        } else {
            // 환경 변수나 Google Cloud 자격 증명으로 자동 설정
            credentials = GoogleCredentials.getApplicationDefault();
        }

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}