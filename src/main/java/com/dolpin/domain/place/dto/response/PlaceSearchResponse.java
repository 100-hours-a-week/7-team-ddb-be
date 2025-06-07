package com.dolpin.domain.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchResponse {

    private int total;

    private List<PlaceDto> places;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceDto {
        private Long id;
        private String name;
        private String thumbnail;
        private Double distance;

        private Long momentCount;

        private List<String> keywords;
        private Map<String, Object> location;

        private Double similarityScore;
    }
}
