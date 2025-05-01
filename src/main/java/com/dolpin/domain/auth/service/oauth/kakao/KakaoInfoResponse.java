package com.dolpin.domain.auth.service.oauth.kakao;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import com.dolpin.domain.auth.service.oauth.OAuthInfoResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoInfoResponse implements OAuthInfoResponse {

    @JsonProperty("id")
    private Long id;


    @Override
    public String getProviderId() {
        return id.toString();
    }

    @Override
    public String getProvider() {
        return OAuthProvider.KAKAO.getName();
    }
}