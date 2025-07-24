package com.dolpin.domain.place.controller;

import com.dolpin.domain.place.dto.response.BookmarkResponse;
import com.dolpin.domain.place.dto.response.BookmarkStatusResponse;
import com.dolpin.domain.place.service.command.PlaceBookmarkCommandService;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.redis.service.DuplicatePreventionService;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/bookmarks")
@RequiredArgsConstructor
@Slf4j
public class PlaceBookmarkController {

    private final PlaceBookmarkCommandService bookmarkCommandService;
    private final PlaceBookmarkQueryService bookmarkQueryService;
    private final DuplicatePreventionService duplicatePreventionService;

    /**
     * 북마크 토글 (기존 기능)
     * POST /api/v1/users/bookmarks/{place_id}
     */
    @PostMapping("/{place_id}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleBookmark(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("place_id") Long placeId) {

        Long userId = Long.parseLong(userDetails.getUsername());
        String lockKey = duplicatePreventionService.generateKey(userId, "toggleBookmark", placeId);

        // Redisson 락으로 중복 요청 방지 (대기 0초, 점유 1초)
        return duplicatePreventionService.executeWithLock(lockKey, 0, 1, () -> {
            boolean isBookmarked = bookmarkCommandService.toggleBookmark(userId, placeId);
            Map<String, Boolean> responseData = Map.of("is_bookmarked", isBookmarked);
            return ResponseEntity.ok(ApiResponse.success("toggle_bookmark_success", responseData));
        });
    }

    /**
     * 북마크 목록 조회 (기존 기능)
     * GET /api/v1/users/bookmarks
     */
    @GetMapping
    public ResponseEntity<ApiResponse<BookmarkResponse>> getUserBookmarks(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        BookmarkResponse response = bookmarkQueryService.getUserBookmarks(userId);
        return ResponseEntity.ok(ApiResponse.success("get_bookmarks_success", response));
    }

    /**
     * 특정 장소의 북마크 상태 조회 (새로 추가)
     * GET /api/v1/users/bookmarks/{place_id}
     */
    @GetMapping("/{place_id}")
    public ResponseEntity<ApiResponse<BookmarkStatusResponse>> getBookmarkStatus(
            @PathVariable("place_id") Long placeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("북마크 상태 조회 요청: placeId={}", placeId);

        // 인증된 사용자 ID 추출
        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;

        if (userId == null) {
            log.warn("인증되지 않은 사용자의 북마크 조회 시도: placeId={}", placeId);
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("재로그인이 필요합니다."));
        }

        try {
            // 장소 존재 여부 검증과 북마크 상태 조회를 한번에
            boolean isBookmarked = bookmarkQueryService.isBookmarkedWithValidation(userId, placeId);

            BookmarkStatusResponse response = BookmarkStatusResponse.of(isBookmarked);

            log.debug("북마크 상태 조회 완료: userId={}, placeId={}, isBookmarked={}",
                    userId, placeId, isBookmarked);

            return ResponseEntity.ok(
                    ApiResponse.success("toggle_bookmark_success", response)
            );

        } catch (BusinessException e) {
            if (e.getResponseStatus() == ResponseStatus.PLACE_NOT_FOUND) {
                log.warn("존재하지 않는 장소 북마크 조회: placeId={}", placeId);
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("장소를 찾을 수 없습니다."));
            }

            log.error("북마크 상태 조회 비즈니스 에러: userId={}, placeId={}", userId, placeId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("내부 서버 오류입니다."));

        } catch (Exception e) {
            log.error("북마크 상태 조회 시스템 에러: userId={}, placeId={}", userId, placeId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("내부 서버 오류입니다."));
        }
    }
}
