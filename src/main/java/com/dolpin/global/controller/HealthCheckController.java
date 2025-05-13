package com.dolpin.global.controller;

import com.dolpin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthCheckController {

    // 마지막 로그 출력 시간을 저장
    private static LocalDateTime lastLogTime = LocalDateTime.now().minusMinutes(10);
    // 로그 출력 주기 (초 단위)
    private static final int LOG_INTERVAL_SECONDS = 60;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        LocalDateTime now = LocalDateTime.now();

        // 마지막 로그 출력 후 설정된 시간(LOG_INTERVAL_SECONDS)이 지났을 때만 로그 출력
        if (now.isAfter(lastLogTime.plusSeconds(LOG_INTERVAL_SECONDS))) {
            log.info("Health check requested (logging once per {} seconds)", LOG_INTERVAL_SECONDS);
            lastLogTime = now;
        } else if (log.isDebugEnabled()) {
            // DEBUG 레벨일 때만 모든 요청 로깅
            log.debug("Health check requested");
        }

        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", now.toString());

        return ResponseEntity.ok(ApiResponse.success("health_check_success", healthInfo));
    }
}