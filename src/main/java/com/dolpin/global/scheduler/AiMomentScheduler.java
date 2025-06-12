// src/main/java/com/dolpin/global/scheduler/AiMomentScheduler.java
package com.dolpin.global.scheduler;

import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.service.ai.AiMomentGenerationService;
import com.dolpin.global.constants.SystemUserConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiMomentScheduler {

    private final AiMomentGenerationService aiMomentGenerationService;
    private final MomentRepository momentRepository;

    @Scheduled(cron = "0 0 1 * * *")
    public void generateDailyAiMoment() {

        // 오늘 이미 돌핀이 기록을 작성했는지 확인
        if (hasAlreadyGeneratedToday()) {
            log.info("Dolpin already generated moment today");
            return;
        }

        try {
            aiMomentGenerationService.generateDailyMoment();
            log.info("Daily AI moment generation scheduler completed successfully");
        } catch (Exception e) {
            log.error("Daily AI moment generation scheduler failed", e);
            // 실패해도 스케줄러는 계속 동작하도록 예외를 삼킴
        }
    }

    private boolean hasAlreadyGeneratedToday() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);

        long todayDolpinMomentCount = momentRepository.countByUserIdAndCreatedAtBetween(
                SystemUserConstants.DOLPIN_USER_ID,
                todayStart,
                todayEnd
        );

        if (todayDolpinMomentCount > 0) {
            log.info("Found {} moments created by Dolpin today", todayDolpinMomentCount);
            return true;
        }

        return false;
    }
}
