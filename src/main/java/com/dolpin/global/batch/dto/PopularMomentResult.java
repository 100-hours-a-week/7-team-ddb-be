package com.dolpin.global.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularMomentResult {
    private Long momentId;
    private String title;
    private String content;
    private String thumbnailUrl;
    private Long userId;
    private String authorName;
    private String authorImage;
    private LocalDateTime createdAt;
    private Long viewCount;
    private Long commentCount;
    private Double popularityScore;
    private String periodType;
    private Integer rank;
    private LocalDateTime calculatedAt;
}
