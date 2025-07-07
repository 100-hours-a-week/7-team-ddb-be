package com.dolpin.domain.moment.controller;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.*;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.service.command.MomentCommandService;
import com.dolpin.domain.moment.service.query.MomentQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.redis.service.DuplicatePreventionService;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class MomentController {

    private final MomentQueryService momentQueryService;
    private final MomentCommandService momentCommandService;
    private final DuplicatePreventionService duplicatePreventionService;
    private final MomentRepository momentRepository;

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

        log.info("Moment 생성 요청: userId={}, title={}", userId, request.getTitle());

        // Redisson 락으로 중복 요청 방지 (대기 0초, 점유 5초)
        return duplicatePreventionService.executeWithLock(lockKey, 0, 5, () -> {

            log.debug("락 획득 후 중복 체크 시작: userId={}", userId);

            // 컨텐츠 중복 체크 - 3개 파라미터 전달
            String contentKey = duplicatePreventionService.generateContentKey(
                    userId, 0L, request.getTitle() + ":" + request.getContent()); // placeId 대신 0L 사용

            duplicatePreventionService.checkDuplicateContent(
                    contentKey,
                    "동일한 내용의 기록이 최근에 등록되었습니다.",
                    Duration.ofMinutes(5)
            );

            log.debug("중복 체크 완료, 비즈니스 로직 실행: userId={}", userId);

            MomentCreateResponse response = momentCommandService.createMoment(userId, request);

            log.info("Moment 생성 완료: userId={}, momentId={}, title={}",
                    userId, response.getId(), request.getTitle());

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

        log.info("Moment 수정 요청: userId={}, momentId={}", userId, momentId);

        // Redisson 락으로 중복 요청 방지 (대기 0초, 점유 3초)
        return duplicatePreventionService.executeWithLock(lockKey, 0, 3, () -> {

            log.debug("락 획득 후 수정 로직 실행: userId={}, momentId={}", userId, momentId);

            MomentUpdateResponse response = momentCommandService.updateMoment(userId, momentId, request);

            log.info("Moment 수정 완료: userId={}, momentId={}", userId, momentId);

            return ResponseEntity.ok(ApiResponse.success("moment_updated", response));
        });
    }

    @DeleteMapping("/users/moments/{moment_id}")
    public ResponseEntity<ApiResponse<Void>> deleteMoment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("moment_id") Long momentId) {

        Long userId = Long.parseLong(userDetails.getUsername());
        String lockKey = duplicatePreventionService.generateKey(userId, "deleteMoment", momentId);

        log.info("Moment 삭제 요청: userId={}, momentId={}", userId, momentId);

        // Redisson 락으로 중복 요청 방지 (대기 0초, 점유 2초)
        return duplicatePreventionService.executeWithLock(lockKey, 0, 2, () -> {

            log.debug("락 획득 후 삭제 로직 실행: userId={}, momentId={}", userId, momentId);

            momentCommandService.deleteMoment(userId, momentId);

            log.info("Moment 삭제 완료: userId={}, momentId={}", userId, momentId);

            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(ApiResponse.success("moment_deleted", null));
        });
    }
}
