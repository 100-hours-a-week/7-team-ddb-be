package com.dolpin.domain.comment.service.command;

import com.dolpin.domain.comment.dto.request.CommentCreateRequest;
import com.dolpin.domain.comment.dto.response.CommentCreateResponse;

public interface CommentCommandService {
    CommentCreateResponse createComment(Long momentId, CommentCreateRequest request, Long userId);
    void deleteComment(Long momentId, Long commentId, Long userId);
}
