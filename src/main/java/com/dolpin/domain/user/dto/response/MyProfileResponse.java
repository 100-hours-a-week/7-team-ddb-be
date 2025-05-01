package com.dolpin.domain.user.dto.response;

import com.dolpin.domain.user.entity.User;
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
    private String username;
    private String provider;
    private String profileImage;
    private String introduction;
    private Boolean privacyAgreed;
    private Boolean locationAgreed;
    private LocalDateTime privacyAgreedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MyProfileResponse from(User user) {
        return MyProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .provider(user.getProvider())
                .profileImage(user.getImageUrl())
                .introduction(user.getIntroduction())
                .privacyAgreed(user.isPrivacyAgreed())
                .locationAgreed(user.isLocationAgreed())
                .privacyAgreedAt(user.getPrivacyAgreedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}