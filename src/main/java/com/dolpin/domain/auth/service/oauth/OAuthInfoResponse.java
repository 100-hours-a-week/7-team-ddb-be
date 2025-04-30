package com.dolpin.domain.auth.service.oauth;


public interface OAuthInfoResponse {
    String getProviderId();
    String getEmail();
    String getNickname();
    String getProfileImageUrl();
    String getProvider();
}