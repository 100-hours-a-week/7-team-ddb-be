package com.dolpin.domain.place.controller;

import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.service.query.PlaceQueryService;
import com.dolpin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@Slf4j
public class PlaceController {

    private final PlaceQueryService placeQueryService;

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

    @GetMapping("/{place_id}")
    public ResponseEntity<ApiResponse<PlaceDetailResponse>> getPlaceDetail(
            @PathVariable("place_id") Long placeId) {

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentTime = now.format(formatter);

        log.info("Fetching place detail for placeId: {} at time: {}", placeId, currentTime);

        PlaceDetailResponse response = placeQueryService.getPlaceDetail(placeId);

        return ResponseEntity.ok(ApiResponse.success("get_place_detail_success", response));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<PlaceCategoryResponse>> getAllCategories() {
        log.info("Retrieving all categories");

        PlaceCategoryResponse response = placeQueryService.getAllCategories();

        return ResponseEntity.ok(ApiResponse.success("get_categories_success", response));
    }
}