package com.dolpin.domain.place.service.factory;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;

public interface PlaceDtoStrategy {

    PlaceSearchResponse.PlaceDto createPlaceDto(PlaceDtoContext context);

    boolean supports(PlaceDtoContext context);


    default int getPriority() {
        return 100;
    }
}
