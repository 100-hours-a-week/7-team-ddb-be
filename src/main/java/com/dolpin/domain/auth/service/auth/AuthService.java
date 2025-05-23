package com.dolpin.domain.auth.service.auth;

import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;
import com.dolpin.domain.auth.dto.response.TokenResponse;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;

public interface AuthService {
    OAuthUrlResponse getOAuthLoginUrl(String provider, String redirectUri);
    TokenResponse generateTokenByAuthorizationCode(String code);
    RefreshTokenResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
}