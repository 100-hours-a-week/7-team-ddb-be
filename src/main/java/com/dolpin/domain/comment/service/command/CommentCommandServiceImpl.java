package com.dolpin.domain.comment.service.command;

import com.dolpin.domain.comment.dto.request.CommentCreateRequest;
import com.dolpin.domain.comment.dto.response.CommentCreateResponse;
import com.dolpin.domain.comment.service.template.CommentCreateOperation;
import com.dolpin.domain.comment.service.template.CommentDeleteOperation;
import com.dolpin.domain.comment.service.template.CommentOperationContext;
import com.dolpin.domain.comment.service.template.CommentOperationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentCommandServiceImpl implements CommentCommandService {

    private final CommentCreateOperation commentCreateOperation;
    private final CommentDeleteOperation commentDeleteOperation;

    @Override
    public CommentCreateResponse createComment(Long momentId, CommentCreateRequest request, Long userId) {
        CommentOperationContext context = CommentOperationContext.builder()
                .operationType(CommentOperationType.CREATE)
                .momentId(momentId)
                .userId(userId)
                .content(request.getContent())
                .parentCommentId(request.getParentCommentId())
                .build();

        return commentCreateOperation.executeCommentOperation(context);
    }

    @Override
    public void deleteComment(Long momentId, Long commentId, Long userId) {
        CommentOperationContext context = CommentOperationContext.builder()
                .operationType(CommentOperationType.DELETE)
                .momentId(momentId)
                .commentId(commentId)
                .userId(userId)
                .build();

        commentDeleteOperation.executeCommentOperation(context);
    }
}
