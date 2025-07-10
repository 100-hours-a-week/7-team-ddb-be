package com.dolpin.domain.place.service.strategy;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PlaceSearchStrategy {

    Mono<List<PlaceSearchResponse.PlaceDto>> search(PlaceSearchContext context);

    boolean supports(PlaceSearchType searchType);

    default int getPriority() {
        return 100;
    }
}
