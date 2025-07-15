package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.PlaceBusinessStatusResponse;
import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import reactor.core.publisher.Mono;

public interface PlaceQueryService {

    PlaceDetailResponse getPlaceDetail(Long placeId, Long userId);

    PlaceDetailResponse getPlaceDetailWithoutBookmark(Long placeId);

    PlaceCategoryResponse getAllCategories();

    Mono<PlaceSearchResponse> searchPlacesAsync(String query, Double lat, Double lng, String category, Long userId);

    Mono<PlaceSearchResponse> searchPlacesWithDevTokenAsync(String query, Double lat, Double lng, String category, String devToken, Long userId);

    PlaceBusinessStatusResponse getPlaceBusinessStatus(Long placeId);
}
