package com.dolpin.domain.place.client;

import com.dolpin.domain.place.dto.response.PlaceAiResponse;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import com.dolpin.global.ratelimit.RateLimiter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceAiClient {

    private final WebClient webClient;
    private final RateLimiter rateLimiter;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${ai.service.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @PostConstruct
    public void init() {
        rateLimiter.init();
    }

    // 기존 동기 버전 (호환성 유지)
    public PlaceAiResponse recommendPlaces(String query) {
        return recommendPlacesAsync(query).block();
    }

    // 토큰 지원 동기 버전 (호환성 유지)
    public PlaceAiResponse recommendPlaces(String query, String token) {
        return recommendPlacesAsync(query, token).block();
    }

    // 새로운 비동기 버전
    public Mono<PlaceAiResponse> recommendPlacesAsync(String query) {
        // 레이트 리밋 검사
        if (rateLimitEnabled && !rateLimiter.allowRequest("ai-service")) {
            return Mono.error(new BusinessException(
                    ResponseStatus.TOO_MANY_REQUESTS,
                    "AI 서비스 요청 한도를 초과했습니다. 60초 후에 다시 시도해주세요."
            ));
        }

        return executeAiRequestAsync(query);
    }

    // 토큰 지원 비동기 버전
    public Mono<PlaceAiResponse> recommendPlacesAsync(String query, String token) {
        // 레이트 리밋 검사 (토큰 전달)
        if (rateLimitEnabled && !rateLimiter.allowRequest("ai-service", token)) {
            return Mono.error(new BusinessException(
                    ResponseStatus.TOO_MANY_REQUESTS,
                    "AI 서비스 요청 한도를 초과했습니다. 60초 후에 다시 시도해주세요."
            ));
        }

        return executeAiRequestAsync(query);
    }

    // 공통 비동기 AI 요청 로직
    private Mono<PlaceAiResponse> executeAiRequestAsync(String query) {
        return webClient.get()
                .uri(aiServiceUrl + "/v1/recommend?text={query}", query)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> {
                            log.error("AI service client error: {}", response.statusCode());
                            return Mono.error(new BusinessException(
                                    ResponseStatus.INTERNAL_SERVER_ERROR, "AI 서비스 클라이언트 에러"));
                        })
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> {
                            log.error("AI service server error: {}", response.statusCode());
                            return Mono.error(new BusinessException(
                                    ResponseStatus.INTERNAL_SERVER_ERROR, "AI 서비스 서버 에러"));
                        })
                .bodyToMono(PlaceAiResponse.class)
                .timeout(Duration.ofSeconds(30))
                .retry(2)
                .doOnSuccess(response -> log.debug("AI 요청 성공: query={}", query))
                .doOnError(error -> log.error("AI 요청 실패: query={}, error={}", query, error.getMessage()));
    }

    public int getRemainingRequests() {
        return rateLimiter.getRemainingRequests("ai-service");
    }
}
