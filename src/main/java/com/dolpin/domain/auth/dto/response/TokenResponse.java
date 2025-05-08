package com.dolpin.domain.auth.dto.response;

import com.dolpin.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken; // 추가된 필드
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;
    private boolean isNewUser;  // 신규 사용자 여부 추가

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String profileImageUrl;
        private String provider;
        private Boolean privacyAgreed;    // 개인정보 동의 여부
        private Boolean locationAgreed;   // 위치정보 동의 여부
        private Boolean profileCompleted; // 프로필 완성 여부

        public static UserInfo from(User user) {
            return UserInfo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .profileImageUrl(user.getImageUrl())
                    .provider(user.getProvider())
                    .privacyAgreed(user.isPrivacyAgreed())
                    .locationAgreed(user.isLocationAgreed())
                    .profileCompleted(user.getUsername() != null && !user.getUsername().startsWith("user"))
                    .build();
        }
    }

    public static TokenResponse of(String accessToken, String refreshToken, Long expiresIn, User user, boolean isNewUser) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Cookie")
                .expiresIn(expiresIn)
                .user(UserInfo.from(user))
                .isNewUser(isNewUser)
                .build();
    }
}