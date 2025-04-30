package com.dolpin.domain.auth.service.auth;

import com.dolpin.domain.auth.dto.response.OAuthUrlResponse;

public interface AuthService {

    OAuthUrlResponse getOAuthLoginUrl(String provider);
}