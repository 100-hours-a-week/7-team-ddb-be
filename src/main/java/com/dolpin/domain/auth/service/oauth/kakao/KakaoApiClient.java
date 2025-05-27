package com.dolpin.domain.auth.service.oauth.kakao;

import com.dolpin.domain.auth.entity.enums.OAuthProvider;
import com.dolpin.domain.auth.service.oauth.OAuthApiClient;
import com.dolpin.domain.auth.service.oauth.OAuthLoginParams;
import com.dolpin.domain.auth.service.oauth.OAuthInfoResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Kakao OAuth API Client
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class KakaoApiClient implements OAuthApiClient {

    private final RestTemplate restTemplate;

    @Value("${kakao.oauth.authorization-uri}")
    private String authorizationUri;

    @Value("${kakao.oauth.client-id}")
    private String clientId;

    @Value("${kakao.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.oauth.api-url:https://kapi.kakao.com}")
    private String apiUrl;

    @Override
    public OAuthProvider oauthProvider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public String requestAccessToken(OAuthLoginParams loginParams) {

        String tokenUrl = authorizationUri.replace("/oauth/authorize", "/oauth/token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = loginParams.makeBody();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);

        String actualRedirectUri;
        String requestedUri = loginParams.getRedirectUri();


        if (requestedUri != null && !requestedUri.isEmpty()) {
            actualRedirectUri = requestedUri;
        } else {
            actualRedirectUri = redirectUri;
        }

        body.add("redirect_uri", actualRedirectUri);
        body.add("code", loginParams.getAuthorizationCode());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        KakaoTokenResponse response = restTemplate.postForObject(tokenUrl, request, KakaoTokenResponse.class);
        return (response != null) ? response.getAccessToken() : null;
    }

    @Override
    public OAuthInfoResponse requestUserInfo(String accessToken) {
        String url = apiUrl + "/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(accessToken);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("property_keys", "[\"kakao_account.email\",\"kakao_account.profile\"]");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(url, request, KakaoInfoResponse.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        public String getAccessToken() {
            return accessToken;
        }
    }
}