package com.dolpin.domain.moment.service.command;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
import com.dolpin.domain.moment.dto.response.MomentUpdateResponse;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.entity.MomentImage;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.moment.repository.MomentImageRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentCommandServiceImpl implements MomentCommandService {

    private final MomentRepository momentRepository;
    private final MomentImageRepository momentImageRepository;

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

        // Moment 저장
        Moment savedMoment = momentRepository.save(moment);

        // 이미지가 있는 경우 처리
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            createMomentImages(savedMoment, request.getImages());
        }

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

        // 기본 정보 업데이트
        moment.updateContent(request.getTitle(), request.getContent(), request.getIsPublic());

        // 장소 정보 업데이트 (필요한 경우)
        if (request.getPlaceId() != null || request.getPlaceName() != null) {
            moment.updatePlaceInfo(request.getPlaceId(), request.getPlaceName());
        }

        // 이미지 업데이트
        if (request.getImages() != null) {
            updateMomentImages(moment, request.getImages());
        }

        Moment updatedMoment = momentRepository.save(moment);

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
                .orElseThrow(() -> new BusinessException(ResponseStatus.USER_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")));

        validateOwnership(moment, userId);

        // Moment 삭제 (Cascade로 관련 이미지도 함께 삭제됨)
        momentRepository.delete(moment);

        log.info("Moment deleted: momentId={}, userId={}", momentId, userId);
    }

    private void createMomentImages(Moment moment, List<String> imageUrls) {
        List<MomentImage> images = IntStream.range(0, imageUrls.size())
                .mapToObj(i -> MomentImage.builder()
                        .moment(moment)
                        .imageUrl(imageUrls.get(i))
                        .imageSequence(i)
                        .build())
                .toList();

        momentImageRepository.saveAll(images);
    }

    private void updateMomentImages(Moment moment, List<String> newImageUrls) {
        moment.getImages().clear();

        momentImageRepository.deleteByMomentId(moment.getId());

        if (!newImageUrls.isEmpty()) {
            List<MomentImage> newImages = IntStream.range(0, newImageUrls.size())
                    .mapToObj(i -> MomentImage.builder()
                            .moment(moment)
                            .imageUrl(newImageUrls.get(i))
                            .imageSequence(i)
                            .build())
                    .toList();
            moment.getImages().addAll(newImages);
            momentImageRepository.saveAll(newImages);
        }
    }

    private void validateOwnership(Moment moment, Long userId) {
        if (!moment.getUserId().equals(userId)) {
            throw new BusinessException(ResponseStatus.FORBIDDEN.withMessage("접근 권한이 없습니다."));
        }
    }
}
