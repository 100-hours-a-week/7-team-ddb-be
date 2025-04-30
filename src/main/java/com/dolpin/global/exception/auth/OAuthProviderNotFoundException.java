package com.dolpin.global.exception.auth;

import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;

public class OAuthProviderNotFoundException extends BusinessException {
    public OAuthProviderNotFoundException(String provider) {
        super(ResponseStatus.OAUTH_PROVIDER_NOT_EXIST,
                "지원하지 않는 OAuth 제공자입니다: " + provider);
    }
}