package com.dolpin.domain.moment.service.template;

import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
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
public class MomentCreateOperation extends MomentOperationTemplate {

    public MomentCreateOperation(MomentRepository momentRepository,
                                 UserQueryService userQueryService) {
        super(momentRepository, userQueryService);
    }

    @Override
    protected void validateInputParameters(MomentOperationContext context) {
        // 부모 클래스의 기본 검증 호출
        super.validateInputParameters(context);

        // Moment 생성 특화 검증
        if (context.getTitle() == null || context.getTitle().trim().isEmpty()) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("제목이 필요합니다."));
        }

        if (context.getTitle().length() > 100) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("제목은 100자 이내여야 합니다."));
        }

        if (context.getContent() != null && context.getContent().length() > 5000) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("내용은 5000자 이내여야 합니다."));
        }

        // 장소 정보 검증
        if ((context.getPlaceId() != null && context.getPlaceName() == null) ||
                (context.getPlaceId() == null && context.getPlaceName() != null)) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("장소 ID와 장소명은 함께 제공되어야 합니다."));
        }
    }

    @Override
    protected void validateBusinessPermissions(MomentOperationContext context, User user, Moment moment) {
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T executeBusinessLogic(MomentOperationContext context, User user, Moment moment) {
        // 1. Moment 엔티티 생성
        Moment newMoment = Moment.builder()
                .userId(context.getUserId())
                .placeId(context.getPlaceId())
                .placeName(context.getPlaceName())
                .title(context.getTitle())
                .content(context.getContent())
                .isPublic(context.getIsPublic() != null ? context.getIsPublic() : true)
                .build();

        // 2. 도메인 메서드를 사용한 이미지 추가
        if (context.getImages() != null && !context.getImages().isEmpty()) {
            newMoment.addImages(context.getImages());
        }

        // 3. Moment 저장 (Cascade로 이미지도 함께 저장됨)
        Moment savedMoment = momentRepository.save(newMoment);

        // 4. 응답 생성
        return (T) MomentCreateResponse.builder()
                .id(savedMoment.getId())
                .createdAt(savedMoment.getCreatedAt())
                .build();
    }

    @Override
    protected void performPostProcessing(MomentOperationContext context, User user, Moment moment, Object result) {
        // 부모 클래스의 기본 후처리 호출
        super.performPostProcessing(context, user, moment, result);

        // Moment 생성 특화 후처리
        if (result instanceof MomentCreateResponse response) {
            log.info("Moment 생성 상세 정보: momentId={}, userId={}, title={}, imageCount={}",
                    response.getId(),
                    context.getUserId(),
                    context.getTitle(),
                    context.getImages() != null ? context.getImages().size() : 0);
        }
    }
}
