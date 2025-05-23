package com.dolpin.domain.auth.service.oauth.kakao;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import com.dolpin.domain.auth.service.oauth.OAuthLoginUrlProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KakaoLoginUrlProvider implements OAuthLoginUrlProvider {

    @Value("${kakao.oauth.client-id}")
    private String clientId;

    @Value("${kakao.oauth.redirect-uri}")
    private String defaultRedirectUri;

    @Value("${kakao.oauth.authorization-uri}")
    private String authorizationUri;

    @Override
    public String getLoginUrl() {
        return getLoginUrl(null);
    }

    @Override
    public String getLoginUrl(String redirectUri) {
        // redirectUri가 있으면 사용하고, 없으면 환경변수 기본값 사용
        String finalRedirectUri = StringUtils.hasText(redirectUri) ? redirectUri : defaultRedirectUri;

        return authorizationUri +
                "?client_id=" + clientId +
                "&redirect_uri=" + finalRedirectUri +
                "&response_type=code";
    }

    @Override
    public OAuthProvider getOAuthProvider() {
        return OAuthProvider.KAKAO;
    }
}