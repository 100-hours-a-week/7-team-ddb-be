package com.dolpin.domain.auth.service.auth;

import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import com.dolpin.domain.auth.service.oauth.OAuthLoginUrlProvider;
import com.dolpin.domain.auth.service.oauth.OAuthLoginUrlProviderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final OAuthLoginUrlProviderFactory oAuthLoginUrlProviderFactory;

    @Override
    public OAuthUrlResponse getOAuthLoginUrl(String provider) {
        OAuthProvider oAuthProvider = OAuthProvider.fromString(provider);
        OAuthLoginUrlProvider urlProvider = oAuthLoginUrlProviderFactory.getProvider(oAuthProvider);
        return new OAuthUrlResponse(urlProvider.getLoginUrl());
    }
}