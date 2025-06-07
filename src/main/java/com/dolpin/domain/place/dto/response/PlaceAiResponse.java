package com.dolpin.domain.place.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceAiResponse {

    private List<PlaceRecommendation> recommendations;

    private String placeCategory;

    @Data
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
