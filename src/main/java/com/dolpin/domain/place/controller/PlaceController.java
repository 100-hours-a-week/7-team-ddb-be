package com.dolpin.domain.place.controller;

import com.dolpin.domain.place.dto.response.PlaceCategoryResponse;
import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.service.query.PlaceQueryService;
import com.dolpin.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        PlaceSearchResponse response = placeQueryService.searchPlaces(query, lat, lng, category, userId);

        return ResponseEntity.ok(ApiResponse.success("get_place_success", response));
    }

    // dev 전용 엔드포인트
    @GetMapping("/search/dev")
    @Profile("dev") // dev 프로파일에서만 활성화
    public ResponseEntity<ApiResponse<PlaceSearchResponse>> searchPlacesForDev(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String category,
            @RequestHeader(value = "X-Dev-Token", required = false) String devToken,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("DEV: Rate limit bypass request with token: {}",
                devToken != null ? devToken.substring(0, Math.min(4, devToken.length())) + "***" : "null");

        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        PlaceSearchResponse response = placeQueryService.searchPlacesWithDevToken(query, lat, lng, category, devToken, userId);

        return ResponseEntity.ok(ApiResponse.success("get_place_success", response));
    }

    @GetMapping("/{place_id}")
    public ResponseEntity<ApiResponse<PlaceDetailResponse>> getPlaceDetail(
            @PathVariable("place_id") Long placeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        PlaceDetailResponse response = placeQueryService.getPlaceDetail(placeId, userId);

        return ResponseEntity.ok(ApiResponse.success("get_place_detail_success", response));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<PlaceCategoryResponse>> getAllCategories() {

        PlaceCategoryResponse response = placeQueryService.getAllCategories();

        return ResponseEntity.ok(ApiResponse.success("get_categories_success", response));
    }
}
