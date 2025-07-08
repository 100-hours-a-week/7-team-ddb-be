package com.dolpin.domain.comment.service.template;

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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public abstract class CommentOperationTemplate {

    protected final CommentRepository commentRepository;
    protected final MomentRepository momentRepository;
    protected final UserQueryService userQueryService;

    /**
     * Template Method - 댓글 작업의 공통 플로우
     */
    @Transactional
    public <T> T executeCommentOperation(CommentOperationContext context) {
        log.debug("댓글 작업 시작: operation={}, momentId={}, userId={}",
                context.getOperationType(), context.getMomentId(), context.getUserId());

        // 1. 사전 검증 (입력값 검증)
        validateInputParameters(context);

        // 2. Moment 검증 및 조회
        Moment moment = validateAndGetMoment(context);

        // 3. User 조회
        User user = getUserForOperation(context);

        // 4. 비즈니스 권한 검증 (각 구현체에서 정의)
        validateBusinessPermissions(context, moment, user);

        // 5. 핵심 비즈니스 로직 실행 (각 구현체에서 정의)
        T result = executeBusinessLogic(context, moment, user);

        // 6. 후처리 (로깅, 이벤트 발행 등)
        performPostProcessing(context, moment, user, result);

        log.debug("댓글 작업 완료: operation={}, result={}", context.getOperationType(), result);
        return result;
    }

    // ============= Abstract Methods (구현체에서 반드시 구현) =============

    /**
     * 각 작업별 비즈니스 권한 검증
     */
    protected abstract void validateBusinessPermissions(CommentOperationContext context, Moment moment, User user);

    /**
     * 핵심 비즈니스 로직 실행
     */
    protected abstract <T> T executeBusinessLogic(CommentOperationContext context, Moment moment, User user);

    // ============= Hook Methods (필요시 오버라이드) =============

    /**
     * 입력 파라미터 검증 (기본 구현 제공, 필요시 오버라이드)
     */
    protected void validateInputParameters(CommentOperationContext context) {
        if (context.getMomentId() == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("기록 ID가 필요합니다."));
        }
        if (context.getUserId() == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("사용자 ID가 필요합니다."));
        }
    }

    /**
     * 후처리 작업 (기본 구현 제공, 필요시 오버라이드)
     */
    protected void performPostProcessing(CommentOperationContext context, Moment moment, User user, Object result) {
        // 기본적으로 로깅만 수행
        log.info("댓글 {} 완료: momentId={}, userId={}, operation={}",
                getOperationName(context.getOperationType()),
                context.getMomentId(),
                context.getUserId(),
                context.getOperationType());
    }

    // ============= Common Helper Methods =============

    /**
     * Moment 검증 및 조회 (공통 로직)
     */
    private Moment validateAndGetMoment(CommentOperationContext context) {
        return momentRepository.findBasicMomentById(context.getMomentId())
                .orElseThrow(() -> new BusinessException(
                        ResponseStatus.MOMENT_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));
    }

    /**
     * User 조회 (공통 로직)
     */
    private User getUserForOperation(CommentOperationContext context) {
        return userQueryService.getUserById(context.getUserId());
    }

    /**
     * 작업명 반환 (로깅용)
     */
    private String getOperationName(CommentOperationType type) {
        return switch (type) {
            case CREATE -> "생성";
            case DELETE -> "삭제";
            case UPDATE -> "수정";
            case LIST -> "조회";
        };
    }
}
