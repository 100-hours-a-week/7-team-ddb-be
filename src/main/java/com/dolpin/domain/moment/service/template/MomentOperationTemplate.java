package com.dolpin.domain.moment.service.template;

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
public abstract class MomentOperationTemplate {

    protected final MomentRepository momentRepository;
    protected final UserQueryService userQueryService;

    @Transactional
    public <T> T executeMomentOperation(MomentOperationContext context) {
        log.debug("Moment 작업 시작: operation={}, momentId={}, userId={}",
                context.getOperationType(), context.getMomentId(), context.getUserId());

        // 1. 사전 검증 (입력값 검증)
        validateInputParameters(context);

        // 2. User 조회
        User user = getUserForOperation(context);

        // 3. Moment 조회 (생성 작업의 경우 null)
        Moment moment = getMomentIfExists(context);

        // 4. 비즈니스 권한 검증 (각 구현체에서 정의)
        validateBusinessPermissions(context, user, moment);

        // 5. 핵심 비즈니스 로직 실행 (각 구현체에서 정의)
        T result = executeBusinessLogic(context, user, moment);

        // 6. 후처리 (로깅, 이벤트 발행 등)
        performPostProcessing(context, user, moment, result);

        log.debug("Moment 작업 완료: operation={}, result={}", context.getOperationType(), result);
        return result;
    }

    protected abstract void validateBusinessPermissions(MomentOperationContext context, User user, Moment moment);

    protected abstract <T> T executeBusinessLogic(MomentOperationContext context, User user, Moment moment);

    protected void validateInputParameters(MomentOperationContext context) {
        if (context.getUserId() == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("사용자 ID가 필요합니다."));
        }
    }

    protected void performPostProcessing(MomentOperationContext context, User user, Moment moment, Object result) {
        // 기본적으로 로깅만 수행
        log.info("Moment {} 완료: momentId={}, userId={}, operation={}",
                getOperationName(context.getOperationType()),
                context.getMomentId(),
                context.getUserId(),
                context.getOperationType());
    }

    private User getUserForOperation(MomentOperationContext context) {
        return userQueryService.getUserById(context.getUserId());
    }

    private Moment getMomentIfExists(MomentOperationContext context) {
        if (context.getMomentId() == null) {
            return null; // 생성 작업의 경우
        }

        // 작업 타입에 따라 다른 조회 메서드 사용
        return switch (context.getOperationType()) {
            case UPDATE -> momentRepository.findByIdWithImages(context.getMomentId())
                    .orElseThrow(() -> new BusinessException(
                            ResponseStatus.MOMENT_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));
            case DELETE -> momentRepository.findBasicMomentById(context.getMomentId())
                    .orElseThrow(() -> new BusinessException(
                            ResponseStatus.MOMENT_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));
            default -> null; // CREATE의 경우
        };
    }

    private String getOperationName(MomentOperationType type) {
        return switch (type) {
            case CREATE -> "생성";
            case UPDATE -> "수정";
            case DELETE -> "삭제";
        };
    }

    protected void validateOwnership(Moment moment, Long userId) {
        if (moment == null) {
            return; // 생성 작업의 경우 소유권 검증 불필요
        }

        if (!moment.isOwnedBy(userId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }
    }
}
