package com.dolpin.domain.auth.service.oauth;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;

public interface OAuthLoginUrlProvider {
    String getLoginUrl();
    String getLoginUrl(String redirectUri);
    OAuthProvider getOAuthProvider();
}