package com.dolpin.global.batch.processor;

import com.dolpin.global.batch.dto.MomentData;
import com.dolpin.global.batch.dto.PopularMomentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class PopularityCalculationProcessor implements ItemProcessor<MomentData, PopularMomentResult> {

    @Override
    public PopularMomentResult process(MomentData moment) throws Exception {
        // 인기도 점수 계산
        double popularityScore = calculatePopularityScore(moment);

        // 최소 점수 필터링 (의미있는 참여가 있는 게시글만)
        if (popularityScore <= 0) {
            log.debug("Filtering out moment {} with score {}", moment.getId(), popularityScore);
            return null; // null 반환시 Writer로 전달되지 않음
        }

        log.debug("Processing moment {} with popularity score {}", moment.getId(), popularityScore);

        return PopularMomentResult.builder()
                .momentId(moment.getId())
                .title(moment.getTitle())
                .content(moment.getContent())
                .thumbnailUrl(moment.getThumbnailUrl())
                .userId(moment.getUserId())
                .authorName(moment.getAuthorName())
                .authorImage(moment.getAuthorImage())
                .createdAt(moment.getCreatedAt())
                .viewCount(moment.getViewCount())
                .commentCount(moment.getCommentCount())
                .popularityScore(popularityScore)
                .periodType(moment.getPeriodType())
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    private double calculatePopularityScore(MomentData moment) {
        double viewWeight = 1.0;
        double commentWeight = 3.0;

        // 주간 집계는 댓글 가중치를 더 높게 (더 깊은 참여)
        if ("weekly".equals(moment.getPeriodType())) {
            commentWeight = 5.0;
        }

        double score = (moment.getViewCount() * viewWeight) + (moment.getCommentCount() * commentWeight);

        // 최신성 보너스 (최근 24시간 게시글에 추가 점수)
        if (moment.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24))) {
            score += 2.0;
        }

        return score;
    }
}
