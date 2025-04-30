package com.dolpin.domain.auth.service.oauth;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import org.springframework.util.MultiValueMap;

/**
 * OAuth 로그인에 필요한 파라미터
 */
public interface OAuthLoginParams {
    OAuthProvider oauthProvider();
    MultiValueMap<String, String> makeBody();
    String getAuthorizationCode();
}