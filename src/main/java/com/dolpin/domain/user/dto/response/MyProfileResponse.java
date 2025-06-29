package com.dolpin.domain.user.dto.response;

import com.dolpin.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyProfileResponse {
    private Long id;

    private Long providerId;

    private String username;

    private String profileImage;

    private String introduction;

    private Boolean isPrivacyAgreed;

    private Boolean isLocationAgreed;

    private LocalDateTime privacyAgreedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static MyProfileResponse from(User user) {
        return MyProfileResponse.builder()
                .id(user.getId())
                .providerId(user.getProviderId())
                .username(user.getUsername())
                .profileImage(user.getImageUrl())  // User 엔티티의 imageUrl을 DTO의 profileImage로 매핑
                .introduction(user.getIntroduction())
                .isPrivacyAgreed(user.isPrivacyAgreed())
                .isLocationAgreed(user.isLocationAgreed())
                .privacyAgreedAt(user.getPrivacyAgreedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
