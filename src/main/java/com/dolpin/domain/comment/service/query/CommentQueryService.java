package com.dolpin.domain.comment.service.query;

import com.dolpin.domain.comment.dto.response.CommentListResponse;

public interface CommentQueryService {
    CommentListResponse getCommentsByMomentId(Long momentId, Integer limit, String cursor, Long currentUserId);
}
