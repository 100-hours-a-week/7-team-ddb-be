package com.dolpin.domain.place.client;

import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import com.dolpin.global.ratelimit.RateLimiter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${ai.service.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @PostConstruct
    public void init() {
        rateLimiter.init();
    }

    public PlaceAiResponse recommendPlaces(String query) {
        // 레이트 리밋 검사
        if (rateLimitEnabled && !rateLimiter.allowRequest("ai-service")) {
            int remainingTime = 60;

            log.warn("AI service request rate limit exceeded");
            throw new BusinessException(
                    ResponseStatus.TOO_MANY_REQUESTS,
                    "AI 서비스 요청 한도를 초과했습니다. " + remainingTime + "초 후에 다시 시도해주세요."
            );
        }

        String requestUrl = String.format("%s/v1/recommend?text=%s", aiServiceUrl, query);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            PlaceAiResponse responseBody = objectMapper.readValue(response.getBody(), PlaceAiResponse.class);
            return responseBody;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
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

    public int getRemainingRequests() {
        return rateLimiter.getRemainingRequests("ai-service");
    }
}