package com.dolpin.domain.place.client;

import com.dolpin.domain.place.dto.request.PlaceRecommendRequest;
import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceAiClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public PlaceAiResponse recommendPlaces(String query) {
        String url = aiServiceUrl + "/place/recommend";  // 엔드포인트 변경

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        PlaceRecommendRequest request = PlaceRecommendRequest.builder()
                .userQuery(query)
                .build();

        HttpEntity<PlaceRecommendRequest> requestEntity = new HttpEntity<>(request, headers);

        log.info("Requesting AI service for query: {}", query);
        try {
            ResponseEntity<PlaceAiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    PlaceAiResponse.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 400) {
                log.error("Invalid request to AI service: {}", e.getMessage());
                throw new BusinessException(ResponseStatus.INVALID_PARAMETER, "AI 서비스 요청 파라미터가 잘못되었습니다");
            } else {
                log.error("Error from AI service: {}", e.getMessage());
                throw new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, "AI 서비스 에러가 발생했습니다");
            }
        } catch (Exception e) {
            log.error("Error while calling AI service: {}", e.getMessage(), e);
            throw new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, "AI 서비스 연결 중 오류가 발생했습니다");
        }
    }
}