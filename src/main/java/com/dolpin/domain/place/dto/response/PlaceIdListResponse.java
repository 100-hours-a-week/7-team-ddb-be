package com.dolpin.domain.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlaceIdListResponse {

    @JsonProperty("ids")
    private final List<Long> ids;

    public static PlaceIdListResponse of(List<Long> placeIds) {
        return PlaceIdListResponse.builder()
                .ids(placeIds)
                .build();
    }
}
