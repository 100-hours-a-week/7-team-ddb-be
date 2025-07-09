package com.dolpin.domain.moment.service.template;

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
public class MomentDeleteOperation extends MomentOperationTemplate {

    public MomentDeleteOperation(MomentRepository momentRepository,
                                 UserQueryService userQueryService) {
        super(momentRepository, userQueryService);
    }

    @Override
    protected void validateInputParameters(MomentOperationContext context) {
        // 부모 클래스의 기본 검증 호출
        super.validateInputParameters(context);

        // Moment 삭제 특화 검증
        if (context.getMomentId() == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("기록 ID가 필요합니다."));
        }
    }

    @Override
    protected void validateBusinessPermissions(MomentOperationContext context, User user, Moment moment) {
        validateOwnership(moment, context.getUserId());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T executeBusinessLogic(MomentOperationContext context, User user, Moment moment) {
        // 1. 삭제 전 정보 저장 (로깅용)
        String momentTitle = moment.getTitle();
        int imageCount = moment.getImageCount();

        // 2. Moment 삭제 (Cascade로 관련 이미지도 함께 삭제됨)
        momentRepository.delete(moment);

        // 3. 삭제 작업은 반환값이 없음 (void)
        log.debug("Moment 엔티티 삭제 완료: momentId={}, title={}, imageCount={}",
                context.getMomentId(), momentTitle, imageCount);

        return null;
    }

    @Override
    protected void performPostProcessing(MomentOperationContext context, User user, Moment moment, Object result) {
        // 부모 클래스의 기본 후처리 호출
        super.performPostProcessing(context, user, moment, result);

        // Moment 삭제 특화 후처리
        log.info("Moment 삭제 완료: momentId={}, userId={}",
                context.getMomentId(), context.getUserId());

    }
}
