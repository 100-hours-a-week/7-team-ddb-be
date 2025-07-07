package com.dolpin.global.fixture;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.*;
import com.dolpin.global.constants.MomentTestConstants;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Moment 도메인 테스트 픽스처
 * 테스트에 필요한 데이터 생성을 담당
 */
public class MomentTestFixture {

    // ==================== Request 객체 생성 ====================

    public MomentCreateRequest createMomentCreateRequest() {
        return MomentCreateRequest.builder()
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();
    }

    public MomentCreateRequest createMomentCreateRequestWithoutPlace() {
        return MomentCreateRequest.builder()
                .title(MomentTestConstants.NEW_MOMENT_TITLE)
                .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();
    }

    public MomentCreateRequest createMomentCreateRequestWithoutImages() {
        return MomentCreateRequest.builder()
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();
    }

    public MomentCreateRequest createPrivateMomentCreateRequest() {
        return MomentCreateRequest.builder()
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(false)
                .build();
    }

    public MomentCreateRequest createInvalidTitleMomentCreateRequest() {
        return MomentCreateRequest.builder()
                .title("") // 빈 제목
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();
    }

    public MomentCreateRequest createInvalidContentMomentCreateRequest() {
        return MomentCreateRequest.builder()
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content("") // 빈 내용
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();
    }

    public MomentCreateRequest createLongTitleMomentCreateRequest() {
        return MomentCreateRequest.builder()
                .title(MomentTestConstants.LONG_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();
    }

    public MomentCreateRequest createLongContentMomentCreateRequest() {
        return MomentCreateRequest.builder()
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.LONG_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();
    }

    public MomentUpdateRequest createMomentUpdateRequest() {
        return MomentUpdateRequest.builder()
                .title(MomentTestConstants.UPDATED_MOMENT_TITLE)
                .content(MomentTestConstants.UPDATED_MOMENT_CONTENT)
                .placeId(MomentTestConstants.UPDATED_PLACE_ID)
                .placeName(MomentTestConstants.UPDATED_PLACE_NAME)
                .images(MomentTestConstants.UPDATED_IMAGES)
                .isPublic(MomentTestConstants.UPDATED_IS_PUBLIC)
                .build();
    }

    public MomentUpdateRequest createPartialMomentUpdateRequest() {
        return MomentUpdateRequest.builder()
                .title(MomentTestConstants.UPDATED_MOMENT_TITLE)
                .isPublic(MomentTestConstants.UPDATED_IS_PUBLIC)
                .build();
    }

    public MomentUpdateRequest createInvalidUpdateRequest() {
        return MomentUpdateRequest.builder()
                .title(MomentTestConstants.LONG_TITLE)
                .content(MomentTestConstants.LONG_CONTENT)
                .build();
    }

    // ==================== Response 객체 생성 ====================

    public MomentCreateResponse createMomentCreateResponse() {
        return MomentCreateResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public MomentUpdateResponse createMomentUpdateResponse() {
        return MomentUpdateResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public MomentDetailResponse createMomentDetailResponse() {
        return MomentDetailResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .images(MomentTestConstants.TEST_IMAGES)
                .place(createPlaceDetailDto())
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .isOwner(MomentTestConstants.IS_OWNER)
                .createdAt(LocalDateTime.now())
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .author(createAuthorDtoForDetail()) // ✅ Detail용 AuthorDto 사용
                .build();
    }

    public MomentDetailResponse createOtherUserMomentDetailResponse() {
        return MomentDetailResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .images(MomentTestConstants.TEST_IMAGES)
                .place(createPlaceDetailDto())
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .isOwner(MomentTestConstants.IS_NOT_OWNER)
                .createdAt(LocalDateTime.now())
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .viewCount(MomentTestConstants.UPDATED_VIEW_COUNT)
                .author(createOtherAuthorDtoForDetail()) // ✅ Detail용 AuthorDto 사용
                .build();
    }

    public MomentListResponse createMomentListResponse() {
        List<MomentListResponse.MomentSummaryDto> moments = List.of(
                createMomentSummaryDto(),
                createOtherMomentSummaryDto()
        );

        return MomentListResponse.builder()
                .moments(moments)
                .meta(createMetaDto(MomentTestConstants.NO_NEXT, null))
                .links(createLinksDto(MomentTestConstants.MOMENTS_BASE_PATH, MomentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    public MomentListResponse createEmptyMomentListResponse() {
        return MomentListResponse.builder()
                .moments(Collections.emptyList())
                .meta(createMetaDto(MomentTestConstants.NO_NEXT, null))
                .links(createLinksDto(MomentTestConstants.MOMENTS_BASE_PATH, MomentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    public MomentListResponse createMyMomentListResponse() {
        List<MomentListResponse.MomentSummaryDto> moments = List.of(
                createMomentSummaryDto(),
                createPrivateMomentSummaryDto()
        );

        return MomentListResponse.builder()
                .moments(moments)
                .meta(createMetaDto(MomentTestConstants.NO_NEXT, null))
                .links(createLinksDto(MomentTestConstants.MY_MOMENTS_PATH, MomentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    public MomentListResponse createUserMomentListResponse() {
        List<MomentListResponse.MomentSummaryDto> moments = List.of(
                createOtherMomentSummaryDto()
        );

        return MomentListResponse.builder()
                .moments(moments)
                .meta(createMetaDto(MomentTestConstants.NO_NEXT, null))
                .links(createLinksDto("/api/v1/users/" + MomentTestConstants.OTHER_USER_ID + "/moments", MomentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    public MomentListResponse createPlaceMomentListResponse() {
        List<MomentListResponse.MomentSummaryDto> moments = List.of(
                createMomentSummaryDto()
        );

        return MomentListResponse.builder()
                .moments(moments)
                .meta(createMetaDto(MomentTestConstants.NO_NEXT, null))
                .links(createLinksDto("/api/v1/places/" + MomentTestConstants.TEST_PLACE_ID + "/moments", MomentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    public MomentListResponse createPaginatedMomentListResponse(int limit, boolean hasNext) {
        List<MomentListResponse.MomentSummaryDto> moments = Collections.nCopies(limit, createMomentSummaryDto());
        String nextCursor = hasNext ? MomentTestConstants.TEST_CURSOR : null;

        return MomentListResponse.builder()
                .moments(moments)
                .meta(createCustomMetaDto(limit, hasNext, nextCursor))
                .links(createLinksDto(MomentTestConstants.MOMENTS_BASE_PATH, limit, null))
                .build();
    }

    // ==================== DTO 생성 - List용과 Detail용 분리 ====================

    public MomentListResponse.MomentSummaryDto createMomentSummaryDto() {
        return MomentListResponse.MomentSummaryDto.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .thumbnail(MomentTestConstants.TEST_IMAGE_1)
                .imagesCount(MomentTestConstants.TEST_IMAGES_COUNT)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .createdAt(LocalDateTime.now())
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .author(createAuthorDtoForList()) // ✅ List용 AuthorDto 사용
                .build();
    }

    public MomentListResponse.MomentSummaryDto createOtherMomentSummaryDto() {
        return MomentListResponse.MomentSummaryDto.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID + 1)
                .title("다른 사용자 기록")
                .content("다른 사용자의 내용")
                .thumbnail(MomentTestConstants.TEST_IMAGE_2)
                .imagesCount(MomentTestConstants.TEST_IMAGES_COUNT)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .createdAt(LocalDateTime.now())
                .viewCount(MomentTestConstants.UPDATED_VIEW_COUNT)
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .author(createOtherAuthorDtoForList()) // ✅ List용 AuthorDto 사용
                .build();
    }

    public MomentListResponse.MomentSummaryDto createPrivateMomentSummaryDto() {
        return MomentListResponse.MomentSummaryDto.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID + 2)
                .title("비공개 기록")
                .content("비공개 내용")
                .thumbnail(MomentTestConstants.TEST_IMAGE_1)
                .imagesCount(MomentTestConstants.TEST_IMAGES_COUNT)
                .isPublic(false)
                .createdAt(LocalDateTime.now())
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .author(createAuthorDtoForList()) // ✅ List용 AuthorDto 사용
                .build();
    }

    public MomentDetailResponse.PlaceDetailDto createPlaceDetailDto() {
        return MomentDetailResponse.PlaceDetailDto.builder()
                .id(MomentTestConstants.TEST_PLACE_ID)
                .name(MomentTestConstants.TEST_PLACE_NAME)
                .build();
    }

    // ==================== Detail용 AuthorDto 생성 ====================

    public MomentDetailResponse.AuthorDto createAuthorDtoForDetail() {
        return MomentDetailResponse.AuthorDto.builder()
                .id(MomentTestConstants.TEST_USER_ID)
                .nickname(MomentTestConstants.TEST_USERNAME)
                .profileImage(MomentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    public MomentDetailResponse.AuthorDto createOtherAuthorDtoForDetail() {
        return MomentDetailResponse.AuthorDto.builder()
                .id(MomentTestConstants.OTHER_USER_ID)
                .nickname("다른사용자")
                .profileImage(MomentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    // ==================== List용 AuthorDto 생성 ====================

    public MomentListResponse.AuthorDto createAuthorDtoForList() {
        return MomentListResponse.AuthorDto.builder()
                .id(MomentTestConstants.TEST_USER_ID)
                .nickname(MomentTestConstants.TEST_USERNAME)
                .profileImage(MomentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    public MomentListResponse.AuthorDto createOtherAuthorDtoForList() {
        return MomentListResponse.AuthorDto.builder()
                .id(MomentTestConstants.OTHER_USER_ID)
                .nickname("다른사용자")
                .profileImage(MomentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    // ==================== Deprecated - 하위 호환성 유지 ====================

    @Deprecated
    public MomentDetailResponse.AuthorDto createAuthorDto() {
        return createAuthorDtoForDetail();
    }

    @Deprecated
    public MomentDetailResponse.AuthorDto createOtherAuthorDto() {
        return createOtherAuthorDtoForDetail();
    }

    // ==================== Meta 및 Links 객체 생성 ====================

    public MomentListResponse.MetaDto createMetaDto(boolean hasNext, String nextCursor) {
        return MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(MomentTestConstants.DEFAULT_PAGE_LIMIT)
                        .nextCursor(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    public MomentListResponse.MetaDto createCustomMetaDto(int limit, boolean hasNext, String nextCursor) {
        return MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(limit)
                        .nextCursor(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    public MomentListResponse.LinksDto createLinksDto(String basePath, int limit, String cursor) {
        String selfHref = buildHref(basePath, limit, cursor);
        String nextHref = cursor != null ? buildHref(basePath, limit, MomentTestConstants.TEST_CURSOR) : null;

        return MomentListResponse.LinksDto.builder()
                .self(MomentListResponse.LinkDto.builder().href(selfHref).build())
                .next(nextHref != null ? MomentListResponse.LinkDto.builder().href(nextHref).build() : null)
                .build();
    }

    // ==================== 락 키 생성 ====================

    public String createMomentCreateLockKey() {
        return MomentTestConstants.TEST_USER_ID + ":createMoment";
    }

    public String createMomentUpdateLockKey() {
        return MomentTestConstants.TEST_USER_ID + ":updateMoment:" + MomentTestConstants.TEST_MOMENT_ID;
    }

    public String createMomentDeleteLockKey() {
        return MomentTestConstants.TEST_USER_ID + ":deleteMoment:" + MomentTestConstants.TEST_MOMENT_ID;
    }

    public String createOtherUserLockKey(String action) {
        return MomentTestConstants.OTHER_USER_ID + ":" + action;
    }

    public String createCustomLockKey(Long userId, String action, Long resourceId) {
        if (resourceId != null) {
            return userId + ":" + action + ":" + resourceId;
        }
        return userId + ":" + action;
    }

    // ==================== Helper 메서드 ====================

    private String buildHref(String basePath, int limit, String cursor) {
        if (cursor != null) {
            return basePath + "?limit=" + limit + "&cursor=" + cursor;
        }
        return basePath + "?limit=" + limit;
    }

    // ==================== 테스트용 시나리오 데이터 생성 ====================

    /**
     * 이미지 순서 테스트용 생성 요청
     */
    public MomentCreateRequest createOrderedImagesMomentCreateRequest() {
        return MomentCreateRequest.builder()
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .images(MomentTestConstants.ORDERED_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();
    }

    /**
     * 이미지 업데이트 테스트용 수정 요청
     */
    public MomentUpdateRequest createImageUpdateRequest() {
        return MomentUpdateRequest.builder()
                .images(MomentTestConstants.UPDATED_IMAGES)
                .build();
    }

    /**
     * 장소 정보만 업데이트하는 요청
     */
    public MomentUpdateRequest createPlaceUpdateRequest() {
        return MomentUpdateRequest.builder()
                .placeId(MomentTestConstants.UPDATED_PLACE_ID)
                .placeName(MomentTestConstants.UPDATED_PLACE_NAME)
                .build();
    }

    /**
     * 공개 설정만 변경하는 요청
     */
    public MomentUpdateRequest createVisibilityUpdateRequest() {
        return MomentUpdateRequest.builder()
                .isPublic(false)
                .build();
    }
}
