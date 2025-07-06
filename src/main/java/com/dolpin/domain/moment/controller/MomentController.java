package com.dolpin.domain.moment.controller;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.*;
import com.dolpin.domain.moment.service.command.MomentCommandService;
import com.dolpin.domain.moment.service.query.MomentQueryService;
import com.dolpin.global.redis.service.DuplicatePreventionService;
import com.dolpin.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class MomentController {

    private final MomentQueryService momentQueryService;
    private final MomentCommandService momentCommandService;
    private final DuplicatePreventionService duplicatePreventionService;


    @GetMapping("/users/moments")
    public ResponseEntity<ApiResponse<MomentListResponse>> getAllMoments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {

        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        MomentListResponse response = momentQueryService.getAllMoments(userId, limit, cursor);
        return ResponseEntity.ok(ApiResponse.success("all_moment_list_get_success", response));
    }

    @GetMapping("/users/me/moments")
    public ResponseEntity<ApiResponse<MomentListResponse>> getMyMoments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {

        Long userId = Long.parseLong(userDetails.getUsername());
        MomentListResponse response = momentQueryService.getMyMoments(userId, limit, cursor);
        return ResponseEntity.ok(ApiResponse.success("user_moment_list_get_success", response));
    }

    @GetMapping("/users/{user_id}/moments")
    public ResponseEntity<ApiResponse<MomentListResponse>> getUserMoments(
            @PathVariable("user_id") Long userId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {

        MomentListResponse response = momentQueryService.getUserMoments(userId, limit, cursor);
        return ResponseEntity.ok(ApiResponse.success("user_moment_list_get_success", response));
    }

    @GetMapping("/places/{place_id}/moments")
    public ResponseEntity<ApiResponse<MomentListResponse>> getPlaceMoments(
            @PathVariable("place_id") Long placeId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {

        MomentListResponse response = momentQueryService.getPlaceMoments(placeId, limit, cursor);
        return ResponseEntity.ok(ApiResponse.success("place_moment_list_get_success", response));
    }

    @GetMapping("/moments/{moment_id}")
    public ResponseEntity<ApiResponse<MomentDetailResponse>> getMomentDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("moment_id") Long momentId) {

        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        MomentDetailResponse response = momentQueryService.getMomentDetail(momentId, userId);
        return ResponseEntity.ok(ApiResponse.success("place_moment_get_success", response));
    }

    @PostMapping("/users/moments")
    public ResponseEntity<ApiResponse<MomentCreateResponse>> createMoment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MomentCreateRequest request) {

        Long userId = Long.parseLong(userDetails.getUsername());
        String lockKey = duplicatePreventionService.generateKey(userId, "createMoment");

        // Redisson 락으로 중복 요청 방지 (대기 0초, 점유 5초)
        return duplicatePreventionService.executeWithLock(lockKey, 0, 5, () -> {
            MomentCreateResponse response = momentCommandService.createMoment(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("moment_created", response));
        });
    }

    @PatchMapping("/users/moments/{moment_id}")
    public ResponseEntity<ApiResponse<MomentUpdateResponse>> updateMoment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("moment_id") Long momentId,
            @Valid @RequestBody MomentUpdateRequest request) {

        Long userId = Long.parseLong(userDetails.getUsername());
        String lockKey = duplicatePreventionService.generateKey(userId, "updateMoment", momentId);

        // Redisson 락으로 중복 요청 방지 (대기 0초, 점유 3초)
        return duplicatePreventionService.executeWithLock(lockKey, 0, 3, () -> {
            MomentUpdateResponse response = momentCommandService.updateMoment(userId, momentId, request);
            return ResponseEntity.ok(ApiResponse.success("moment_updated", response));
        });
    }

    @DeleteMapping("/users/moments/{moment_id}")
    public ResponseEntity<ApiResponse<Void>> deleteMoment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("moment_id") Long momentId) {

        Long userId = Long.parseLong(userDetails.getUsername());
        String lockKey = duplicatePreventionService.generateKey(userId, "deleteMoment", momentId);

        // Redisson 락으로 중복 요청 방지 (대기 0초, 점유 2초)
        return duplicatePreventionService.executeWithLock(lockKey, 0, 2, () -> {
            momentCommandService.deleteMoment(userId, momentId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponse.success("moment_deleted", null));
        });
    }
}
