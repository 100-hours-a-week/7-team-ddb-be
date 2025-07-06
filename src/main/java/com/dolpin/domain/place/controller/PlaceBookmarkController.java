package com.dolpin.domain.place.controller;

import com.dolpin.domain.place.dto.response.BookmarkResponse;
import com.dolpin.domain.place.service.command.PlaceBookmarkCommandService;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import com.dolpin.global.redis.service.DuplicatePreventionService;
import com.dolpin.global.response.ApiResponse;
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

    @GetMapping
    public ResponseEntity<ApiResponse<BookmarkResponse>> getUserBookmarks(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());

        BookmarkResponse response = bookmarkQueryService.getUserBookmarks(userId);

        return ResponseEntity.ok(ApiResponse.success("get_bookmarks_success", response));
    }
}
