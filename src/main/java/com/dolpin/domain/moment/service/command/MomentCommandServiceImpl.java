package com.dolpin.domain.moment.service.command;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
import com.dolpin.domain.moment.dto.response.MomentUpdateResponse;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentCommandServiceImpl implements MomentCommandService {

    private final MomentRepository momentRepository;

    @Override
    @Transactional
    public MomentCreateResponse createMoment(Long userId, MomentCreateRequest request) {
        // Moment 엔티티 생성
        Moment moment = Moment.builder()
                .userId(userId)
                .placeId(request.getPlaceId())
                .placeName(request.getPlaceName())
                .title(request.getTitle())
                .content(request.getContent())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .build();

        // 도메인 메서드를 사용한 이미지 추가
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            moment.addImages(request.getImages());
        }

        // Moment 저장 (Cascade로 이미지도 함께 저장됨)
        Moment savedMoment = momentRepository.save(moment);

        log.info("Moment created: momentId={}, userId={}, imageCount={}",
                savedMoment.getId(), userId, savedMoment.getImageCount());

        return MomentCreateResponse.builder()
                .id(savedMoment.getId())
                .createdAt(savedMoment.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public MomentUpdateResponse updateMoment(Long userId, Long momentId, MomentUpdateRequest request) {
        // 기존 Moment 조회 및 권한 검증
        Moment moment = momentRepository.findByIdWithImages(momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));

        validateOwnership(moment, userId);

        // 도메인 메서드를 사용한 업데이트
        moment.updateContent(request.getTitle(), request.getContent(), request.getIsPublic());

        // 장소 정보 업데이트
        if (request.getPlaceId() != null || request.getPlaceName() != null) {
            moment.updatePlaceInfo(request.getPlaceId(), request.getPlaceName());
        }

        // 도메인 메서드를 사용한 이미지 교체
        if (request.getImages() != null) {
            moment.replaceImages(request.getImages());
        }

        // 저장 (Cascade로 이미지 변경사항도 함께 저장됨)
        Moment updatedMoment = momentRepository.save(moment);

        log.info("Moment updated: momentId={}, userId={}, imageCount={}",
                momentId, userId, updatedMoment.getImageCount());

        return MomentUpdateResponse.builder()
                .id(updatedMoment.getId())
                .updatedAt(updatedMoment.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteMoment(Long userId, Long momentId) {
        // Moment 조회 및 권한 검증
        Moment moment = momentRepository.findBasicMomentById(momentId)
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("사용자를 찾을 수 없습니다.")));

        validateOwnership(moment, userId);

        // Moment 삭제 (Cascade로 관련 이미지도 함께 삭제됨)
        momentRepository.delete(moment);

        log.info("Moment deleted: momentId={}, userId={}", momentId, userId);
    }

    private void validateOwnership(Moment moment, Long userId) {
        if (!moment.isOwnedBy(userId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }
    }
}
