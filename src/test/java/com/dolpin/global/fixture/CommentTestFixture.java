package com.dolpin.global.fixture;

import com.dolpin.domain.comment.dto.request.CommentCreateRequest;
import com.dolpin.domain.comment.dto.response.CommentCreateResponse;
import com.dolpin.domain.comment.dto.response.CommentListResponse;
import com.dolpin.global.constants.CommentTestConstants;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Comment 도메인 테스트 픽스처
 * 테스트에 필요한 데이터 생성을 담당
 */
public class CommentTestFixture {

    // ==================== Request 객체 생성 ====================

    public CommentCreateRequest createCommentCreateRequest() {
        return new CommentCreateRequest(CommentTestConstants.TEST_COMMENT_CONTENT, null);
    }

    public CommentCreateRequest createReplyCommentCreateRequest() {
        return new CommentCreateRequest(
                CommentTestConstants.REPLY_CONTENT,
                CommentTestConstants.PARENT_COMMENT_ID
        );
    }

    public CommentCreateRequest createInvalidCommentCreateRequest() {
        return new CommentCreateRequest(CommentTestConstants.EMPTY_CONTENT, null);
    }

    public CommentCreateRequest createLongContentCommentCreateRequest() {
        return new CommentCreateRequest(CommentTestConstants.LONG_CONTENT, null);
    }

    public CommentCreateRequest createInvalidParentCommentCreateRequest() {
        return new CommentCreateRequest(
                CommentTestConstants.TEST_COMMENT_CONTENT,
                CommentTestConstants.INVALID_PARENT_COMMENT_ID
        );
    }

    public CommentCreateRequest createBlankContentCommentCreateRequest() {
        return new CommentCreateRequest(CommentTestConstants.BLANK_CONTENT, null);
    }

    // ==================== Response 객체 생성 ====================

    public CommentCreateResponse createCommentCreateResponse() {
        return CommentCreateResponse.builder()
                .id(CommentTestConstants.TEST_COMMENT_ID)
                .user(createUserDtoForCreate())
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .parentCommentId(null)
                .createdAt(LocalDateTime.now())
                .isOwner(CommentTestConstants.IS_OWNER)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .build();
    }

    public CommentCreateResponse createReplyCommentCreateResponse() {
        return CommentCreateResponse.builder()
                .id(CommentTestConstants.REPLY_COMMENT_ID)
                .user(createUserDtoForCreate())
                .content(CommentTestConstants.REPLY_CONTENT)
                .depth(CommentTestConstants.REPLY_COMMENT_DEPTH)
                .parentCommentId(CommentTestConstants.PARENT_COMMENT_ID)
                .createdAt(LocalDateTime.now())
                .isOwner(CommentTestConstants.IS_OWNER)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .build();
    }

    public CommentCreateResponse createOtherUserCommentCreateResponse() {
        return CommentCreateResponse.builder()
                .id(CommentTestConstants.TEST_COMMENT_ID)
                .user(createOtherUserDtoForCreate())
                .content(CommentTestConstants.OTHER_USER_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .parentCommentId(null)
                .createdAt(LocalDateTime.now())
                .isOwner(CommentTestConstants.IS_NOT_OWNER)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .build();
    }

    // ==================== List Response 객체 생성 ====================

    public CommentListResponse createCommentListResponse() {
        List<CommentListResponse.CommentDto> comments = List.of(
                createCommentDto(),
                createReplyCommentDto()
        );

        return CommentListResponse.builder()
                .comments(comments)
                .meta(createMetaDto(CommentTestConstants.NO_NEXT, null))
                .links(createLinksDto(CommentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    public CommentListResponse createEmptyCommentListResponse() {
        return CommentListResponse.builder()
                .comments(Collections.emptyList())
                .meta(createMetaDto(CommentTestConstants.NO_NEXT, null))
                .links(createLinksDto(CommentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    public CommentListResponse createCommentListResponseWithCursor() {
        List<CommentListResponse.CommentDto> comments = List.of(createCommentDto());

        return CommentListResponse.builder()
                .comments(comments)
                .meta(createCustomMetaDto(CommentTestConstants.CUSTOM_PAGE_LIMIT, CommentTestConstants.HAS_NEXT, CommentTestConstants.NEXT_CURSOR))
                .links(createLinksDto(CommentTestConstants.CUSTOM_PAGE_LIMIT, CommentTestConstants.TEST_CURSOR))
                .build();
    }

    public CommentListResponse createSingleCommentListResponse() {
        List<CommentListResponse.CommentDto> comments = List.of(createCommentDto());

        return CommentListResponse.builder()
                .comments(comments)
                .meta(createMetaDto(CommentTestConstants.NO_NEXT, null))
                .links(createLinksDto(CommentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    public CommentListResponse createMultipleCommentsListResponse() {
        List<CommentListResponse.CommentDto> comments = List.of(
                createCommentDto(),
                createReplyCommentDto(),
                createOtherUserCommentDto()
        );

        return CommentListResponse.builder()
                .comments(comments)
                .meta(createMetaDto(CommentTestConstants.HAS_NEXT, CommentTestConstants.NEXT_CURSOR))
                .links(createLinksDto(CommentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    // ==================== Comment DTO 생성 ====================

    public CommentListResponse.CommentDto createCommentDto() {
        return CommentListResponse.CommentDto.builder()
                .id(CommentTestConstants.TEST_COMMENT_ID)
                .user(createUserDto())
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .parentCommentId(null)
                .createdAt(LocalDateTime.now())
                .isOwner(CommentTestConstants.IS_OWNER)
                .build();
    }

    public CommentListResponse.CommentDto createReplyCommentDto() {
        return CommentListResponse.CommentDto.builder()
                .id(CommentTestConstants.REPLY_COMMENT_ID)
                .user(createUserDto())
                .content(CommentTestConstants.REPLY_CONTENT)
                .depth(CommentTestConstants.REPLY_COMMENT_DEPTH)
                .parentCommentId(CommentTestConstants.PARENT_COMMENT_ID)
                .createdAt(LocalDateTime.now())
                .isOwner(CommentTestConstants.IS_OWNER)
                .build();
    }

    public CommentListResponse.CommentDto createOtherUserCommentDto() {
        return CommentListResponse.CommentDto.builder()
                .id(CommentTestConstants.TEST_COMMENT_ID + 100)
                .user(createOtherUserDto())
                .content(CommentTestConstants.OTHER_USER_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .parentCommentId(null)
                .createdAt(LocalDateTime.now())
                .isOwner(CommentTestConstants.IS_NOT_OWNER)
                .build();
    }

    public CommentListResponse.CommentDto createDeletedCommentDto() {
        return CommentListResponse.CommentDto.builder()
                .id(CommentTestConstants.DELETED_COMMENT_ID)
                .user(createUserDto())
                .content("삭제된 댓글입니다")
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .parentCommentId(null)
                .createdAt(LocalDateTime.now())
                .isOwner(CommentTestConstants.IS_OWNER)
                .build();
    }

    // ==================== User DTO 생성 ====================

    public CommentListResponse.UserDto createUserDto() {
        return CommentListResponse.UserDto.builder()
                .id(CommentTestConstants.TEST_USER_ID)
                .nickname(CommentTestConstants.TEST_USERNAME)
                .profileImage(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    public CommentCreateResponse.UserDto createUserDtoForCreate() {
        return CommentCreateResponse.UserDto.builder()
                .id(CommentTestConstants.TEST_USER_ID)
                .nickname(CommentTestConstants.TEST_USERNAME)
                .profileImage(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    public CommentListResponse.UserDto createOtherUserDto() {
        return CommentListResponse.UserDto.builder()
                .id(CommentTestConstants.OTHER_USER_ID)
                .nickname(CommentTestConstants.OTHER_USERNAME)
                .profileImage(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    public CommentCreateResponse.UserDto createOtherUserDtoForCreate() {
        return CommentCreateResponse.UserDto.builder()
                .id(CommentTestConstants.OTHER_USER_ID)
                .nickname(CommentTestConstants.OTHER_USERNAME)
                .profileImage(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    // ==================== Meta 및 Links 객체 생성 ====================

    public CommentListResponse.MetaDto createMetaDto(boolean hasNext, String nextCursor) {
        return CommentListResponse.MetaDto.builder()
                .pagination(CommentListResponse.PaginationDto.builder()
                        .limit(CommentTestConstants.DEFAULT_PAGE_LIMIT)
                        .nextCursor(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    public CommentListResponse.MetaDto createCustomMetaDto(int limit, boolean hasNext, String nextCursor) {
        return CommentListResponse.MetaDto.builder()
                .pagination(CommentListResponse.PaginationDto.builder()
                        .limit(limit)
                        .nextCursor(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    public CommentListResponse.LinksDto createLinksDto(int limit, String cursor) {
        String selfHref = buildHref(limit, cursor);
        String nextHref = cursor != null ? buildHref(limit, CommentTestConstants.NEXT_CURSOR) : null;

        return CommentListResponse.LinksDto.builder()
                .self(CommentListResponse.LinkDto.builder().href(selfHref).build())
                .next(nextHref != null ? CommentListResponse.LinkDto.builder().href(nextHref).build() : null)
                .build();
    }

    // ==================== 락 키 생성 ====================

    public String createLockKey() {
        return CommentTestConstants.TEST_USER_ID + ":createComment:" + CommentTestConstants.TEST_MOMENT_ID;
    }

    public String createOtherUserLockKey() {
        return CommentTestConstants.OTHER_USER_ID + ":createComment:" + CommentTestConstants.TEST_MOMENT_ID;
    }

    public String createCustomLockKey(Long userId, String action, Long resourceId) {
        return userId + ":" + action + ":" + resourceId;
    }

    // ==================== Helper 메서드 ====================

    private String buildHref(int limit, String cursor) {
        String baseUrl = "/api/v1/moments/" + CommentTestConstants.TEST_MOMENT_ID + "/comments";
        if (cursor != null) {
            return baseUrl + "?limit=" + limit + "&cursor=" + cursor;
        }
        return baseUrl + "?limit=" + limit;
    }

    // ==================== 테스트용 시나리오 데이터 생성 ====================

    /**
     * 페이징 테스트용 댓글 목록 생성
     */
    public CommentListResponse createPaginatedCommentListResponse(int size, boolean hasNext) {
        List<CommentListResponse.CommentDto> comments = Collections.nCopies(size, createCommentDto());
        String nextCursor = hasNext ? CommentTestConstants.NEXT_CURSOR : null;

        return CommentListResponse.builder()
                .comments(comments)
                .meta(createCustomMetaDto(size, hasNext, nextCursor)) // size를 limit으로 사용
                .links(createLinksDto(size, null)) // size를 limit으로 사용
                .build();
    }

    /**
     * 스레드 구조 테스트용 댓글 목록 생성 (부모-자식 관계)
     */
    public CommentListResponse createThreadedCommentListResponse() {
        List<CommentListResponse.CommentDto> comments = List.of(
                createCommentDto(),  // 부모 댓글
                createReplyCommentDto(),  // 자식 댓글
                createCommentDto()  // 또 다른 부모 댓글
        );

        return CommentListResponse.builder()
                .comments(comments)
                .meta(createMetaDto(CommentTestConstants.NO_NEXT, null))
                .links(createLinksDto(CommentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }

    /**
     * 권한별 댓글 목록 생성 (소유자/비소유자 혼합)
     */
    public CommentListResponse createMixedOwnershipCommentListResponse() {
        List<CommentListResponse.CommentDto> comments = List.of(
                createCommentDto(),  // 내 댓글
                createOtherUserCommentDto()  // 다른 사용자 댓글
        );

        return CommentListResponse.builder()
                .comments(comments)
                .meta(createMetaDto(CommentTestConstants.NO_NEXT, null))
                .links(createLinksDto(CommentTestConstants.DEFAULT_PAGE_LIMIT, null))
                .build();
    }
}
