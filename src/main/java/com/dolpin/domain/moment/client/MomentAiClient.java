package com.dolpin.domain.moment.client;

import com.dolpin.domain.moment.dto.request.AiMomentGenerationRequest;
import com.dolpin.domain.moment.dto.response.AiMomentGenerationResponse;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class MomentAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public AiMomentGenerationResponse generateMomentFromPlace(AiMomentGenerationRequest request) {
        String url = aiServiceUrl + "/v1/moment/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AiMomentGenerationRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("Requesting AI moment generation for place: {} (ID: {})",
                    request.getName(), request.getId());

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );
            AiMomentGenerationResponse result = objectMapper.readValue(response.getBody(), AiMomentGenerationResponse.class);

            System.out.println(request);

            if (result != null) {
                log.info("AI generated moment successfully: title={}, placeId={}",
                        result.getTitle(), result.getPlaceId());
            } else {
                log.warn("AI returned null response for place: {}", request.getName());
            }

            return result;

        } catch (Exception e) {
            log.error("AI moment generation API call failed for place {}: {}",
                    request.getName(), e.getMessage(), e);
            throw new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR,
                    "AI 기록 생성 서비스 호출 실패");
        }
    }
}
