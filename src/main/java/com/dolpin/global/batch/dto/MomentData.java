package com.dolpin.global.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MomentData {
    private Long id;
    private String title;
    private String content;
    private String thumbnailUrl;
    private Long userId;
    private LocalDateTime createdAt;
    private Long viewCount;
    private String authorName;
    private String authorImage;
    private Long commentCount;
    private String periodType;
}
