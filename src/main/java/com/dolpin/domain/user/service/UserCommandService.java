package com.dolpin.domain.user.service;

import com.dolpin.domain.auth.service.oauth.OAuthInfoResponse;
import com.dolpin.domain.user.entity.User;

public interface UserCommandService {
    User createUser(OAuthInfoResponse oAuthInfo);
    void updateProfile(Long userId, String username, String imageUrl, String introduction);
    void agreeToTerms(Long userId, boolean isPrivacyAgreed, boolean isLocationAgreed);
    void deleteUser(Long userId);
    void registerUser(Long userId, String nickname, String profileImage, String introduction);
}