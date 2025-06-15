package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;

public interface PlaceQueryService {

    PlaceSearchResponse searchPlaces(String query, Double lat, Double lng, String category, Long userId);

    PlaceDetailResponse getPlaceDetail(Long placeId, Long userId);

    PlaceDetailResponse getPlaceDetailWithoutBookmark(Long placeId);

    PlaceCategoryResponse getAllCategories();

    PlaceSearchResponse searchPlacesWithDevToken(String query, Double lat, Double lng, String category, String devToken, Long userId);
}
