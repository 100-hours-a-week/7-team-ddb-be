package com.dolpin.domain.auth.service.oauth;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;

/**
 * OAuth 로그인 URL 제공자 인터페이스
 */
public interface OAuthLoginUrlProvider {
    String getLoginUrl();
    OAuthProvider getOAuthProvider();
}