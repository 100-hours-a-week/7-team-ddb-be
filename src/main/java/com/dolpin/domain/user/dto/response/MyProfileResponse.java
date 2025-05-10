package com.dolpin.domain.user.dto.response;

import com.dolpin.domain.user.entity.User;
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
    @JsonProperty("user_id")
    private Long id;

    @JsonProperty("provider_id")
    private Long providerId;

    private String username;  // 프론트엔드에서도 username으로 사용

    @JsonProperty("profile_image")
    private String profileImage;  // JSON 변환 시 profile_image로

    private String introduction;

    @JsonProperty("privacy_agreed")
    private Boolean isPrivacyAgreed;

    @JsonProperty("location_agreed")
    private Boolean isLocationAgreed;

    @JsonProperty("privacy_agreed_at")
    private LocalDateTime privacyAgreedAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
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