package com.dolpin.domain.place.controller;

import com.dolpin.domain.place.dto.response.PlaceBusinessStatusResponse;
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
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@Slf4j
public class PlaceController {

    private final PlaceQueryService placeQueryService;

    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponse<PlaceSearchResponse>>> searchPlaces(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;

        return placeQueryService.searchPlacesAsync(query, lat, lng, category, userId)
                .map(response -> ResponseEntity.ok(ApiResponse.success("get_place_success", response)))
                .doOnSuccess(result -> log.debug("비동기 검색 완료: query={}", query));
    }

    @GetMapping("/search/dev")
    @Profile("dev")
    public Mono<ResponseEntity<ApiResponse<PlaceSearchResponse>>> searchPlacesForDev(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String category,
            @RequestHeader(value = "X-Dev-Token", required = false) String devToken,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("DEV: Rate limit bypass request with token: {}",
                devToken != null ? devToken.substring(0, Math.min(4, devToken.length())) + "***" : "null");

        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;

        return placeQueryService.searchPlacesWithDevTokenAsync(query, lat, lng, category, devToken, userId)
                .map(response -> ResponseEntity.ok(ApiResponse.success("get_place_success", response)));
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

    @GetMapping("/{place_id}/business_status")
    public ResponseEntity<ApiResponse<PlaceBusinessStatusResponse>> getPlaceBusinessStatus(
            @PathVariable("place_id") Long placeId) {

        PlaceBusinessStatusResponse response = placeQueryService.getPlaceBusinessStatus(placeId);

        return ResponseEntity.ok(ApiResponse.success("get_place_business_status_success", response));
    }
}
