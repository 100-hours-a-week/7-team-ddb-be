package com.dolpin.domain.auth.service.oauth.kakao;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import com.dolpin.domain.auth.service.oauth.OAuthLoginParams;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Objects;


@Getter
public class KakaoLoginParams implements OAuthLoginParams {
    private final String authorizationCode;
    private final String redirectUri;

    @Builder
    public KakaoLoginParams(String authorizationCode, String redirectUri) {
        // 유효성 검증 추가
        this.authorizationCode = Objects.requireNonNull(authorizationCode, "인증 코드는 필수입니다");
        this.redirectUri = redirectUri;
    }

    @Override
    public OAuthProvider oauthProvider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public MultiValueMap<String, String> makeBody() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authorizationCode);

        // redirectUri가 있을 때만 추가
        if (redirectUri != null && !redirectUri.isEmpty()) {
            body.add("redirect_uri", redirectUri);
        }

        return body;
    }
}