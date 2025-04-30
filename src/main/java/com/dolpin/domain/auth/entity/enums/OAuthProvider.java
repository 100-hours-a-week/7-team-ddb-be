package com.dolpin.domain.auth.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum OAuthProvider {
    KAKAO("kakao"),
    GOOGLE("google");

    private final String name;

    public static OAuthProvider fromString(String provider) {
        return Arrays.stream(OAuthProvider.values())
                .filter(p -> p.getName().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown OAuth provider: " + provider));
    }
}