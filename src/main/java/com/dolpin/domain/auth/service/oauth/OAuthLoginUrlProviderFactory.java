package com.dolpin.domain.auth.service.oauth;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuthLoginUrlProviderFactory {

    private final List<OAuthLoginUrlProvider> providers;

    public OAuthLoginUrlProvider getProvider(OAuthProvider provider) {
        return providers.stream()
                .filter(p -> p.getOAuthProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: " + provider.getName())); //customexception 만들면 수정해야함
    }
}