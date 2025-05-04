package com.dolpin.domain.place.client;

import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceAiClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public PlaceAiResponse searchPlacesByQuery(String query) {
        String url = aiServiceUrl + "/search";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, String> requestBody = Map.of("query", query);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        log.info("Requesting AI service for query: {}", query);
        ResponseEntity<PlaceAiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                PlaceAiResponse.class
        );

        return response.getBody();
    }
}