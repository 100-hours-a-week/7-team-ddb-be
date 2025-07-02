// CommentCommandServiceImpl.java - Method 2 수정
package com.dolpin.domain.comment.service.command;

import com.dolpin.domain.comment.dto.request.CommentCreateRequest;
import com.dolpin.domain.comment.dto.response.CommentCreateResponse;
import com.dolpin.domain.comment.entity.Comment;
import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentCommandServiceImpl implements CommentCommandService {

    private final CommentRepository commentRepository;
    private final MomentRepository momentRepository;
    private final UserQueryService userQueryService;

    @Override
    @Transactional
    public CommentCreateResponse createComment(Long momentId, CommentCreateRequest request, Long userId) {
        // 댓글 작성을 위한 특별한 권한 검증
        Moment moment = validateMomentForComment(momentId, userId);

        User user = userQueryService.getUserById(userId);

        // 댓글 생성
        Comment.CommentBuilder commentBuilder = Comment.builder()
                .momentId(momentId)
                .userId(userId)
                .content(request.getContent())
                .depth(0);  // 기본값 설정

        // 대댓글인 경우 부모 댓글 처리
        if (request.getParentCommentId() != null) {
            Comment parentComment = validateParentComment(request.getParentCommentId(), momentId);
            commentBuilder.parentComment(parentComment)
                    .depth(1);  // 대댓글은 depth 1
        }

        Comment comment = commentBuilder.build();
        Comment savedComment = commentRepository.save(comment);

        log.info("Comment created: commentId={}, momentId={}, userId={}, isReply={}",
                savedComment.getId(), momentId, userId, savedComment.isReply());

        return CommentCreateResponse.from(savedComment, user, true);
    }

    // 부모 댓글 유효성 검사 메서드
    private Comment validateParentComment(Long parentCommentId, Long momentId) {
        return commentRepository.findValidParentComment(parentCommentId, momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.INVALID_PARAMETER
                        .withMessage("유효하지 않은 부모 댓글입니다.")));
    }

    @Override
    @Transactional
    public void deleteComment(Long momentId, Long commentId, Long userId) {
        // 기록 존재 확인
        validateMomentExists(momentId);

        // 댓글 조회 및 권한 확인
        Comment comment = commentRepository.findByIdAndMomentIdAndNotDeleted(commentId, momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("댓글을 찾을 수 없습니다.")));

        if (!comment.canBeDeletedBy(userId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("댓글을 삭제할 권한이 없습니다."));
        }

        comment.softDelete();
        commentRepository.save(comment);

        log.info("Comment deleted: commentId={}, momentId={}, userId={}", commentId, momentId, userId);
    }

    private Moment validateMomentForComment(Long momentId, Long userId) {
        Moment moment = momentRepository.findBasicMomentById(momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.MOMENT_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));

        // 비공개 기록인 경우, 작성자 본인만 댓글 작성 가능
        if (!moment.getIsPublic() && !moment.isOwnedBy(userId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("다른 사용자의 비공개 기록에는 댓글을 작성할 수 없습니다."));
        }

        // 일반 접근 권한 체크 (공개 기록이거나 본인 비공개 기록인 경우)
        if (!moment.canBeViewedBy(userId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }

        return moment;
    }

    private Moment validateMomentAccess(Long momentId, Long currentUserId) {
        Moment moment = momentRepository.findBasicMomentById(momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.MOMENT_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));

        if (!moment.canBeViewedBy(currentUserId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }

        return moment;
    }

    private Moment validateMomentExists(Long momentId) {
        return momentRepository.findBasicMomentById(momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.MOMENT_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));
    }
}
