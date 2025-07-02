package com.dolpin.global.constants;

/**
 * Comment 도메인 테스트용 상수 클래스
 */
public final class CommentTestConstants {

    // User 관련 상수
    public static final Long TEST_USER_ID = 1L;
    public static final Long OTHER_USER_ID = 999L;
    public static final String TEST_USERNAME = "testuser";
    public static final String OTHER_USERNAME = "otheruser";
    public static final String TEST_PROFILE_IMAGE_URL = "profile.jpg";

    // Moment 관련 상수
    public static final Long TEST_MOMENT_ID = 1L;
    public static final Long DELETED_MOMENT_ID = 999L;
    public static final String PUBLIC_MOMENT_TITLE = "공개 기록";
    public static final String PRIVATE_MOMENT_TITLE = "비공개 기록";
    public static final String OTHER_USER_PRIVATE_MOMENT_TITLE = "타인의 비공개 기록";
    public static final String MOMENT_CONTENT = "기록 내용";
    public static final String PRIVATE_MOMENT_CONTENT = "비공개 내용";
    public static final Long DEFAULT_VIEW_COUNT = 0L;

    // Comment 관련 상수
    public static final Long TEST_COMMENT_ID = 1L;
    public static final Long PARENT_COMMENT_ID = 2L;
    public static final Long REPLY_COMMENT_ID = 3L;
    public static final Long DELETED_COMMENT_ID = 4L;
    public static final Long NON_EXISTENT_COMMENT_ID = 999L;
    public static final Long INVALID_PARENT_COMMENT_ID = 888L;
    public static final String TEST_COMMENT_CONTENT = "테스트 댓글";
    public static final String NEW_COMMENT_CONTENT = "새로운 댓글";
    public static final String REPLY_CONTENT = "대댓글";
    public static final String PARENT_COMMENT_CONTENT = "부모 댓글";
    public static final String OTHER_USER_COMMENT_CONTENT = "다른 사용자 댓글";
    public static final String UPDATED_COMMENT_CONTENT = "수정된 댓글";

    // Comment depth 관련 상수
    public static final Integer ROOT_COMMENT_DEPTH = 0;
    public static final Integer REPLY_COMMENT_DEPTH = 1;

    // 페이징 관련 상수
    public static final int DEFAULT_PAGE_LIMIT = 10;
    public static final int MAX_PAGE_LIMIT = 50;
    public static final int CUSTOM_PAGE_LIMIT = 5;
    public static final int OVER_MAX_LIMIT = 100;
    public static final int NEGATIVE_LIMIT = -1;
    public static final int ZERO_LIMIT = 0;
    public static final String TEST_CURSOR = "2024-01-01T00:00:00.000Z";
    public static final String INVALID_CURSOR = "invalid-cursor";
    public static final String NEXT_CURSOR = "2024-01-02T00:00:00.000Z";
    public static final int TEST_OFFSET = 0;

    // API 응답 메시지 상수
    public static final String COMMENT_CREATED_MESSAGE = "comment_created";
    public static final String COMMENT_DELETED_MESSAGE = "comment_deleted_success";
    public static final String GET_COMMENT_SUCCESS_MESSAGE = "get_comment_success";

    // 에러 메시지 상수
    public static final String COMMENT_NOT_FOUND_MESSAGE = "댓글을 찾을 수 없습니다.";
    public static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    public static final String MOMENT_NOT_FOUND_MESSAGE = "기록을 찾을 수 없습니다.";
    public static final String ACCESS_DENIED_MESSAGE = "접근 권한이 없습니다.";
    public static final String DELETE_PERMISSION_DENIED_MESSAGE = "댓글을 삭제할 권한이 없습니다.";
    public static final String PRIVATE_MOMENT_COMMENT_DENIED_MESSAGE = "다른 사용자의 비공개 기록에는 댓글을 작성할 수 없습니다.";
    public static final String INVALID_PARENT_COMMENT_MESSAGE = "유효하지 않은 부모 댓글입니다.";
    public static final String CONTENT_REQUIRED_MESSAGE = "댓글 내용은 필수입니다";
    public static final String CONTENT_LENGTH_MESSAGE = "댓글은 1000자 이내여야 합니다";

    // API 경로 상수
    public static final String COMMENTS_BASE_PATH = "/api/v1/moments/{moment_id}/comments";
    public static final String COMMENT_DELETE_PATH = "/api/v1/moments/{moment_id}/comments/{comment_id}";

    // 비즈니스 로직 관련 상수
    public static final boolean IS_OWNER = true;
    public static final boolean IS_NOT_OWNER = false;
    public static final boolean IS_PUBLIC = true;
    public static final boolean IS_PRIVATE = false;
    public static final boolean IS_REPLY = true;
    public static final boolean IS_NOT_REPLY = false;
    public static final boolean HAS_NEXT = true;
    public static final boolean NO_NEXT = false;
    public static final boolean IS_DELETED = true;
    public static final boolean IS_NOT_DELETED = false;

    // 유효성 검증 관련 상수
    public static final int MAX_CONTENT_LENGTH = 1000;
    public static final String LONG_CONTENT = "A".repeat(MAX_CONTENT_LENGTH + 1);
    public static final String EMPTY_CONTENT = "";
    public static final String BLANK_CONTENT = "   ";
    public static final String NULL_CONTENT = null;

    // 요청 파라미터 상수
    public static final String LIMIT_PARAM = "limit";
    public static final String CURSOR_PARAM = "cursor";

    // JSON Path 상수
    public static final String MESSAGE_JSON_PATH = "$.message";
    public static final String DATA_JSON_PATH = "$.data";
    public static final String COMMENT_ID_JSON_PATH = "$.data.id";
    public static final String COMMENT_CONTENT_JSON_PATH = "$.data.content";
    public static final String COMMENT_DEPTH_JSON_PATH = "$.data.depth";
    public static final String PARENT_COMMENT_ID_JSON_PATH = "$.data.parentCommentId";
    public static final String IS_OWNER_JSON_PATH = "$.data.isOwner";
    public static final String COMMENTS_JSON_PATH = "$.data.comments";
    public static final String META_JSON_PATH = "$.data.meta";
    public static final String PAGINATION_JSON_PATH = "$.data.meta.pagination";
    public static final String LIMIT_JSON_PATH = "$.data.meta.pagination.limit";
    public static final String HAS_NEXT_JSON_PATH = "$.data.meta.pagination.hasNext";
    public static final String NEXT_CURSOR_JSON_PATH = "$.data.meta.pagination.nextCursor";

    // HTTP 상태 코드
    public static final int HTTP_STATUS_OK = 200;
    public static final int HTTP_STATUS_CREATED = 201;
    public static final int HTTP_STATUS_NO_CONTENT = 204;
    public static final int HTTP_STATUS_BAD_REQUEST = 400;
    public static final int HTTP_STATUS_FORBIDDEN = 403;
    public static final int HTTP_STATUS_NOT_FOUND = 404;

    // 테스트 데이터 크기
    public static final int COMMENT_LIST_SIZE = 5;
    public static final int EMPTY_COMMENT_LIST_SIZE = 0;
    public static final int SINGLE_COMMENT_SIZE = 1;

    // Comment count 관련 상수
    public static final Long ZERO_COMMENT_COUNT = 0L;
    public static final Long SINGLE_COMMENT_COUNT = 1L;
    public static final Long MULTIPLE_COMMENT_COUNT = 5L;

    private CommentTestConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }
}
