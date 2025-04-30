package com.dolpin.domain.auth.service.oauth;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import com.dolpin.global.exception.auth.OAuthProviderNotFoundException;
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
                .orElseThrow(() -> new OAuthProviderNotFoundException(provider.getName()));
    }
}