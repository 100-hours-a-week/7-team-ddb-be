package com.dolpin.domain.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceAiResponse {

    @JsonProperty("recommendations")
    private List<PlaceRecommendation> recommendations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceRecommendation {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("similarity_score")
        private Double similarityScore;
    }

    // 기존 메서드 호환성을 위한 메서드
    public List<PlaceRecommendation> getData() {
        return recommendations;
    }
}