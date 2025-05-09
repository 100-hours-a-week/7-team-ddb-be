package com.dolpin.domain.place.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceRecommendRequest {

    @JsonProperty("text")  // "userQuery"에서 "text"로 변경
    private String userQuery;
}