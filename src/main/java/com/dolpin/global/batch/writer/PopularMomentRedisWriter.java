package com.dolpin.global.batch.writer;

import com.dolpin.global.batch.dto.PopularMomentResult;
import com.dolpin.global.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularMomentRedisWriter implements ItemWriter<PopularMomentResult> {

    private final RedisService redisService;

    @Override
    public void write(Chunk<? extends PopularMomentResult> chunk) throws Exception {
        List<? extends PopularMomentResult> items = chunk.getItems();

        if (items.isEmpty()) {
            log.info("No popular moments to write");
            return;
        }

        // 기간별로 그룹화
        Map<String, List<PopularMomentResult>> groupedByPeriod = items.stream()
                .collect(Collectors.groupingBy(PopularMomentResult::getPeriodType));

        // 일간 처리
        if (groupedByPeriod.containsKey("daily")) {
            processDailyMoments(groupedByPeriod.get("daily"));
        }

        // 주간 처리
        if (groupedByPeriod.containsKey("weekly")) {
            processWeeklyMoments(groupedByPeriod.get("weekly"));
        }

        log.info("Successfully wrote {} popular moments to Redis", items.size());
    }

    private void processDailyMoments(List<PopularMomentResult> dailyMoments) {
        // 인기도 순으로 정렬하고 순위 부여
        List<PopularMomentResult> sortedDaily = dailyMoments.stream()
                .sorted(Comparator.comparing(PopularMomentResult::getPopularityScore).reversed())
                .limit(20) // 상위 20개만
                .collect(Collectors.toList());

        // 순위 부여
        assignRanks(sortedDaily);

        // Redis에 저장
        String dailyKey = generateDailyKey();
        redisService.set(dailyKey, sortedDaily, Duration.ofHours(25));

        // 최신 일간 인기 게시글
        redisService.set("popular:moments:latest_daily", sortedDaily, Duration.ofHours(26));

        log.info("Processed {} daily popular moments", sortedDaily.size());
    }

    private void processWeeklyMoments(List<PopularMomentResult> weeklyMoments) {
        // 인기도 순으로 정렬하고 순위 부여
        List<PopularMomentResult> sortedWeekly = weeklyMoments.stream()
                .sorted(Comparator.comparing(PopularMomentResult::getPopularityScore).reversed())
                .limit(30) // 상위 30개만
                .collect(Collectors.toList());

        // 순위 부여
        assignRanks(sortedWeekly);

        // Redis에 저장
        String weeklyKey = generateWeeklyKey();
        redisService.set(weeklyKey, sortedWeekly, Duration.ofDays(8));

        // 최신 주간 인기 게시글
        redisService.set("popular:moments:latest_weekly", sortedWeekly, Duration.ofDays(9));

        log.info("Processed {} weekly popular moments", sortedWeekly.size());
    }


    private void assignRanks(List<PopularMomentResult> moments) {
        for (int i = 0; i < moments.size(); i++) {
            moments.get(i).setRank(i + 1);
        }
    }


    private String generateDailyKey() {
        return "popular:moments:daily:" + LocalDate.now().minusDays(1);
    }


    private String generateWeeklyKey() {
        return "popular:moments:weekly:" + LocalDate.now().minusDays(7);
    }
}
