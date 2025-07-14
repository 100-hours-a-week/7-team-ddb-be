package com.dolpin.domain.comment.controller;

import com.dolpin.domain.comment.dto.request.CommentCreateRequest;
import com.dolpin.domain.comment.dto.response.CommentCreateResponse;
import com.dolpin.domain.comment.dto.response.CommentListResponse;
import com.dolpin.domain.comment.service.command.CommentCommandService;
import com.dolpin.domain.comment.service.query.CommentQueryService;
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

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/moments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentQueryService commentQueryService;
    private final CommentCommandService commentCommandService;
    private final DuplicatePreventionService duplicatePreventionService;

    @GetMapping("/{moment_id}/comments")
    public ResponseEntity<ApiResponse<CommentListResponse>> getComments(
            @PathVariable("moment_id") Long momentId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long currentUserId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        CommentListResponse response = commentQueryService.getCommentsByMomentId(momentId, limit, cursor, currentUserId);

        return ResponseEntity.ok(ApiResponse.success("get_comment_success", response));
    }

    @PostMapping("/{moment_id}/comments")
    public ResponseEntity<ApiResponse<CommentCreateResponse>> createComment(
            @PathVariable("moment_id") Long momentId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());

        // 1. 동시 요청 방지를 위한 락 키
        String lockKey = duplicatePreventionService.generateKey(userId, "createComment", momentId);

        // 2. 내용 중복 검증을 위한 키 (해시 기반)
        String contentKey = duplicatePreventionService.generateContentKey(
                userId, momentId, request.getContent());

        // Redisson 락으로 중복 요청 방지 (대기 0초, 점유 3초)
        return duplicatePreventionService.executeWithLock(lockKey, 0, 3, () -> {

            // 내용 중복 검증
            duplicatePreventionService.checkDuplicateContent(
                    contentKey,
                    "동일한 댓글이 이미 등록되어 있습니다.",
                    Duration.ofMinutes(1)
            );

            CommentCreateResponse response = commentCommandService.createComment(momentId, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("comment_created", response));
        });
    }

    @DeleteMapping("/{moment_id}/comments/{comment_id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable("moment_id") Long momentId,
            @PathVariable("comment_id") Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        commentCommandService.deleteComment(momentId, commentId, userId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success("comment_deleted_success", null));
    }
}
