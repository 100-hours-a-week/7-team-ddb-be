package com.dolpin.domain.moment.service.template;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class MomentOperationContext {
    // 공통 필드
    private MomentOperationType operationType;
    private Long userId;
    private Long momentId;

    // 생성용 필드
    private String title;
    private String content;
    private Long placeId;
    private String placeName;
    private List<String> images;
    private Boolean isPublic;

    // 수정용 필드 (MomentUpdateRequest의 모든 필드를 포함)
    private String updatedTitle;
    private String updatedContent;
    private Long updatedPlaceId;
    private String updatedPlaceName;
    private List<String> updatedImages;
    private Boolean updatedIsPublic;

    // 편의 메서드: MomentCreateRequest로부터 Context 생성
    public static MomentOperationContext fromCreateRequest(Long userId, MomentCreateRequest request) {
        return MomentOperationContext.builder()
                .operationType(MomentOperationType.CREATE)
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .placeId(request.getPlaceId())
                .placeName(request.getPlaceName())
                .images(request.getImages())
                .isPublic(request.getIsPublic())
                .build();
    }

    // 편의 메서드: MomentUpdateRequest로부터 Context 생성
    public static MomentOperationContext fromUpdateRequest(Long userId, Long momentId, MomentUpdateRequest request) {
        return MomentOperationContext.builder()
                .operationType(MomentOperationType.UPDATE)
                .userId(userId)
                .momentId(momentId)
                .updatedTitle(request.getTitle())
                .updatedContent(request.getContent())
                .updatedPlaceId(request.getPlaceId())
                .updatedPlaceName(request.getPlaceName())
                .updatedImages(request.getImages())
                .updatedIsPublic(request.getIsPublic())
                .build();
    }

    // 편의 메서드: Delete 작업을 위한 Context 생성
    public static MomentOperationContext forDelete(Long userId, Long momentId) {
        return MomentOperationContext.builder()
                .operationType(MomentOperationType.DELETE)
                .userId(userId)
                .momentId(momentId)
                .build();
    }
}
