package com.dolpin.domain.place.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceAiResponse {

    private List<PlaceRecommendation> recommendations;

    private String placeCategory;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceRecommendation {
        private Long id;

        private Double similarityScore;

        private List<String> keyword;
    }

    // 기존 메서드 호환성을 위한 메서드
    public List<PlaceRecommendation> getData() {
        return recommendations;
    }
}
