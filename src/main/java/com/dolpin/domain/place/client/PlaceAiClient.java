package com.dolpin.domain.place.client;

import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public PlaceAiResponse recommendPlaces(String query) {
        String url = aiServiceUrl + "/v1/recommend";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // text 필드 사용
        Map<String, String> request = new HashMap<>();
        request.put("text", query);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(request, headers);

        log.info("Requesting AI service for query: {}", query);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            // 응답 본문을 PlaceAiResponse로 변환
            PlaceAiResponse responseBody = objectMapper.readValue(response.getBody(), PlaceAiResponse.class);
            return responseBody;

        } catch (HttpClientErrorException e) {
            log.error("Error from AI service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, "AI 서비스 에러가 발생했습니다");
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI service response: {}", e.getMessage());
            throw new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, "AI 서비스 응답 처리 중 오류가 발생했습니다");
        } catch (Exception e) {
            log.error("Error while calling AI service: {}", e.getMessage());
            throw new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, "AI 서비스 연결 중 오류가 발생했습니다");
        }
    }
}