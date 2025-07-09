package com.dolpin.domain.moment.service.template;

import com.dolpin.domain.moment.dto.response.MomentUpdateResponse;
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
public class MomentUpdateOperation extends MomentOperationTemplate {

    public MomentUpdateOperation(MomentRepository momentRepository,
                                 UserQueryService userQueryService) {
        super(momentRepository, userQueryService);
    }

    @Override
    protected void validateInputParameters(MomentOperationContext context) {
        // 부모 클래스의 기본 검증 호출
        super.validateInputParameters(context);

        // Moment 수정 특화 검증
        if (context.getMomentId() == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("기록 ID가 필요합니다."));
        }

        // 수정할 내용이 있는지 확인
        if (context.getUpdatedTitle() == null &&
                context.getUpdatedContent() == null &&
                context.getUpdatedPlaceId() == null &&
                context.getUpdatedPlaceName() == null &&
                context.getUpdatedImages() == null &&
                context.getUpdatedIsPublic() == null) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("수정할 내용이 없습니다."));
        }

        // 제목 검증
        if (context.getUpdatedTitle() != null) {
            if (context.getUpdatedTitle().trim().isEmpty()) {
                throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("제목은 비어있을 수 없습니다."));
            }
            if (context.getUpdatedTitle().length() > 100) {
                throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("제목은 100자 이내여야 합니다."));
            }
        }

        // 내용 검증
        if (context.getUpdatedContent() != null && context.getUpdatedContent().length() > 5000) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("내용은 5000자 이내여야 합니다."));
        }

        // 장소 정보 검증
        if ((context.getUpdatedPlaceId() != null && context.getUpdatedPlaceName() == null) ||
                (context.getUpdatedPlaceId() == null && context.getUpdatedPlaceName() != null)) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("장소 ID와 장소명은 함께 제공되어야 합니다."));
        }
    }

    @Override
    protected void validateBusinessPermissions(MomentOperationContext context, User user, Moment moment) {
        // 1. Moment 존재 확인 (이미 getMomentIfExists에서 확인됨)

        // 2. 소유권 확인 (소유자만 수정 가능)
        validateOwnership(moment, context.getUserId());

        // 3. 필요시 추가 비즈니스 규칙
        // 예: 수정 시간 제한, 수정 횟수 제한 등
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T executeBusinessLogic(MomentOperationContext context, User user, Moment moment) {
        // 1. 도메인 메서드를 사용한 내용 업데이트
        if (hasContentUpdate(context)) {
            moment.updateContent(
                    context.getUpdatedTitle(),
                    context.getUpdatedContent(),
                    context.getUpdatedIsPublic()
            );
        }

        // 2. 장소 정보 업데이트
        if (context.getUpdatedPlaceId() != null || context.getUpdatedPlaceName() != null) {
            moment.updatePlaceInfo(context.getUpdatedPlaceId(), context.getUpdatedPlaceName());
        }

        // 3. 도메인 메서드를 사용한 이미지 교체
        if (context.getUpdatedImages() != null) {
            moment.replaceImages(context.getUpdatedImages());
        }

        // 4. 저장 (Cascade로 이미지 변경사항도 함께 저장됨)
        Moment updatedMoment = momentRepository.save(moment);

        // 5. 응답 생성
        return (T) MomentUpdateResponse.builder()
                .id(updatedMoment.getId())
                .updatedAt(updatedMoment.getUpdatedAt())
                .build();
    }

    @Override
    protected void performPostProcessing(MomentOperationContext context, User user, Moment moment, Object result) {
        // 부모 클래스의 기본 후처리 호출
        super.performPostProcessing(context, user, moment, result);

        // Moment 수정 특화 후처리
        if (result instanceof MomentUpdateResponse response) {
            log.info("Moment 수정 상세 정보: momentId={}, userId={}, 이미지수={}, 수정시간={}",
                    response.getId(),
                    context.getUserId(),
                    moment.getImageCount(),
                    response.getUpdatedAt());
        }

        // 필요시 이벤트 발행
        // eventPublisher.publishEvent(new MomentUpdatedEvent(context.getMomentId(), context.getUserId()));

        // 필요시 캐시 무효화
        // cacheService.invalidateMomentCache(context.getMomentId());
    }

    private boolean hasContentUpdate(MomentOperationContext context) {
        return context.getUpdatedTitle() != null ||
                context.getUpdatedContent() != null ||
                context.getUpdatedIsPublic() != null;
    }
}
