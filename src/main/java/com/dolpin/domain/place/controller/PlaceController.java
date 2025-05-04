package com.dolpin.domain.place.controller;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.service.PlaceQueryService;
import com.dolpin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@Slf4j
public class PlaceController {

    private final PlaceQueryService placeQueryService;

    /**
     * 장소 검색 API (AI 검색 결과 기반)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PlaceSearchResponse>> searchPlaces(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String category) {

        log.info("Searching places with query: {}, lat: {}, lng: {}, category: {}",
                query, lat, lng, category);

        PlaceSearchResponse response = placeQueryService.searchPlaces(query, lat, lng, category);

        return ResponseEntity.ok(ApiResponse.success("get_place_success", response));
    }
}