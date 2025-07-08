package com.dolpin.domain.comment.service.template;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CommentOperationContext {
    // 공통 필드
    private CommentOperationType operationType;
    private Long momentId;
    private Long userId;

    // 생성용 필드
    private String content;
    private Long parentCommentId;

    // 삭제/수정용 필드
    private Long commentId;

    // 조회용 필드
    private Integer limit;
    private String cursor;
}

