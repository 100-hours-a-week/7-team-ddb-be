package com.dolpin.domain.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceAiResponse {

    private List<PlaceRecommendation> data;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceRecommendation {

        @JsonProperty("place_id")
        private Long placeId;

        @JsonProperty("similarity_score")
        private Double similarityScore;
    }
}