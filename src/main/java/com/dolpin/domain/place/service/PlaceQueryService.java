package com.dolpin.domain.place.service;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import org.springframework.data.domain.Pageable;

public interface PlaceQueryService {

    /**
     * 장소 검색
     */
    PlaceSearchResponse searchPlaces(String query, Double lat, Double lng, String category);
}