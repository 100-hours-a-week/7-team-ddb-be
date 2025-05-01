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
public class UserProfileResponse {
    private Long id;
    private String username;
    private String profileImage;
    private String introduction;
    private LocalDateTime createdAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileImage(user.getImageUrl())
                .introduction(user.getIntroduction())
                .createdAt(user.getCreatedAt())
                .build();
    }
}