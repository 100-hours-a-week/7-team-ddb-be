package com.dolpin.domain.comment.service.template;

import com.dolpin.domain.comment.entity.Comment;
import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.service.cache.MomentCacheService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommentDeleteOperation extends CommentOperationTemplate {

    private final MomentCacheService momentCacheService;

    public CommentDeleteOperation(CommentRepository commentRepository,
                                  MomentRepository momentRepository,
                                  UserQueryService userQueryService, MomentCacheService momentCacheService) {
        super(commentRepository, momentRepository, userQueryService);
        this.momentCacheService = momentCacheService;
    }

    @Override
    protected void validateInputParameters(CommentOperationContext context) {
        // 부모 클래스의 기본 검증 호출
        super.validateInputParameters(context);

        // 댓글 삭제 특화 검증
        if (context.getCommentId() == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("댓글 ID가 필요합니다."));
        }
    }

    @Override
    protected void validateBusinessPermissions(CommentOperationContext context, Moment moment, User user) {
        // 1. 댓글 조회 및 존재 확인
        Comment comment = commentRepository.findByIdAndMomentIdAndNotDeleted(context.getCommentId(), context.getMomentId())
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("댓글을 찾을 수 없습니다.")));

        // 2. 삭제 권한 확인 (댓글 작성자 본인만 삭제 가능)
        if (!comment.canBeDeletedBy(context.getUserId())) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("댓글을 삭제할 권한이 없습니다."));
        }

        // 3. 이미 삭제된 댓글인지 확인
        if (comment.isDeleted()) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("이미 삭제된 댓글입니다."));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T executeBusinessLogic(CommentOperationContext context, Moment moment, User user) {
        // 1. 댓글 조회
        Comment comment = commentRepository.findByIdAndMomentIdAndNotDeleted(context.getCommentId(), context.getMomentId())
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("댓글을 찾을 수 없습니다.")));

        // 2. Soft Delete 실행
        comment.softDelete();

        // 3. 저장
        commentRepository.save(comment);

        // 4. 삭제 작업은 반환값이 없음
        return null;
    }

    @Override
    protected void performPostProcessing(CommentOperationContext context, Moment moment, User user, Object result) {
        // 부모 클래스의 기본 후처리 호출
        super.performPostProcessing(context, moment, user, result);

        // 댓글 삭제 특화 후처리
        log.info("댓글 삭제 완료: commentId={}, momentId={}, userId={}",
                context.getCommentId(), context.getMomentId(), context.getUserId());

        momentCacheService.invalidateCommentCount(context.getMomentId());

        // 필요시 이벤트 발행
        // eventPublisher.publishEvent(new CommentDeletedEvent(context.getCommentId(), context.getMomentId()));
    }
}
