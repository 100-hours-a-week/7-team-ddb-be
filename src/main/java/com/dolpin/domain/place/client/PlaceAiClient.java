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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
        // 변경된 엔드포인트
        String url = aiServiceUrl + "/v1/recommend";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // GET 요청이므로 파라미터를 URL에 추가
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("text", query);

        log.info("Requesting AI service for query: {}", query);
        log.info("Request URL: {}", builder.toUriString());

        try {
            // GET 메서드로 변경하고 요청 본문은 필요 없음
            ResponseEntity<String> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // 응답 본문을 PlaceAiResponse로 변환
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
}