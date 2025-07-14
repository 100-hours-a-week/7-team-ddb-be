package com.dolpin.domain.comment.service.template;

import com.dolpin.domain.comment.dto.response.CommentCreateResponse;
import com.dolpin.domain.comment.entity.Comment;
import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommentCreateOperation extends CommentOperationTemplate {

    public CommentCreateOperation(CommentRepository commentRepository,
                                  MomentRepository momentRepository,
                                  UserQueryService userQueryService) {
        super(commentRepository, momentRepository, userQueryService);
    }

    @Override
    protected void validateInputParameters(CommentOperationContext context) {
        // 부모 클래스의 기본 검증 호출
        super.validateInputParameters(context);

        // 댓글 생성 특화 검증
        if (context.getContent() == null || context.getContent().trim().isEmpty()) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("댓글 내용이 필요합니다."));
        }

        if (context.getContent().length() > 1000) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("댓글은 1000자 이내여야 합니다."));
        }
    }

    @Override
    protected void validateBusinessPermissions(CommentOperationContext context, Moment moment, User user) {

        // 2. 비공개 기록인 경우, 작성자 본인만 댓글 작성 가능
        if (!moment.getIsPublic() && !moment.isOwnedBy(context.getUserId())) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("다른 사용자의 비공개 기록에는 댓글을 작성할 수 없습니다."));
        }

        // 3. 일반 접근 권한 체크
        if (!moment.canBeViewedBy(context.getUserId())) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }

        // 4. 대댓글인 경우 부모 댓글 검증
        if (context.getParentCommentId() != null) {
            validateParentComment(context.getParentCommentId(), context.getMomentId());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T executeBusinessLogic(CommentOperationContext context, Moment moment, User user) {
        // 1. Comment 엔티티 빌드
        Comment.CommentBuilder commentBuilder = Comment.builder()
                .momentId(context.getMomentId())
                .userId(context.getUserId())
                .content(context.getContent())
                .depth(0); // 기본값

        // 2. 대댓글 처리
        if (context.getParentCommentId() != null) {
            Comment parentComment = getValidatedParentComment(context.getParentCommentId(), context.getMomentId());
            commentBuilder.parentComment(parentComment).depth(1);
        }

        // 3. 저장
        Comment comment = commentBuilder.build();
        Comment savedComment = commentRepository.save(comment);

        // 4. 응답 DTO 생성
        CommentCreateResponse response = CommentCreateResponse.from(savedComment, user, true);

        return (T) response;
    }

    @Override
    protected void performPostProcessing(CommentOperationContext context, Moment moment, User user, Object result) {
        // 부모 클래스의 기본 후처리 호출
        super.performPostProcessing(context, moment, user, result);

        // 댓글 생성 특화 후처리
        if (result instanceof CommentCreateResponse response) {
            log.info("새 댓글 생성: commentId={}, momentId={}, userId={}, isReply={}",
                    response.getId(), context.getMomentId(), context.getUserId(),
                    context.getParentCommentId() != null);

            // 필요시 이벤트 발행
            // eventPublisher.publishEvent(new CommentCreatedEvent(response.getId(), context.getMomentId()));
        }
    }

    // ============= Private Helper Methods =============

    private void validateParentComment(Long parentCommentId, Long momentId) {
        commentRepository.findValidParentComment(parentCommentId, momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("유효하지 않은 부모 댓글입니다.")));
    }

    private Comment getValidatedParentComment(Long parentCommentId, Long momentId) {
        return commentRepository.findValidParentComment(parentCommentId, momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("유효하지 않은 부모 댓글입니다.")));
    }
}
