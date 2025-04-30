package com.dolpin.domain.auth.service.oauth;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;

public interface OAuthApiClient {
    // 토큰 발급 요청
    String requestAccessToken(OAuthLoginParams params);

    // 사용자 정보 요청
    OAuthInfoResponse requestUserInfo(String accessToken);

    OAuthProvider oauthProvider();
}