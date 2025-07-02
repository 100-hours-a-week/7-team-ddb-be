package com.dolpin.global.constants;

import java.util.Arrays;
import java.util.List;

/**
 * Moment 도메인 테스트용 상수 클래스
 */
public final class MomentTestConstants {

    // User 관련 상수
    public static final Long TEST_USER_ID = 1L;
    public static final Long OTHER_USER_ID = 999L;
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_PROFILE_IMAGE_URL = "profile.jpg";

    // Moment 관련 상수
    public static final Long TEST_MOMENT_ID = 1L;
    public static final Long TEST_PLACE_ID = 1L;
    public static final String TEST_MOMENT_TITLE = "테스트 제목";
    public static final String TEST_MOMENT_CONTENT = "테스트 내용";
    public static final String TEST_PLACE_NAME = "테스트 장소";
    public static final Boolean DEFAULT_IS_PUBLIC = true;
    public static final Long DEFAULT_VIEW_COUNT = 0L;
    public static final Long UPDATED_VIEW_COUNT = 10L;
    public static final Long DEFAULT_COMMENT_COUNT = 5L;

    // 수정용 상수
    public static final String UPDATED_MOMENT_TITLE = "수정된 제목";
    public static final String UPDATED_MOMENT_CONTENT = "수정된 내용";
    public static final Long UPDATED_PLACE_ID = 2L;
    public static final String UPDATED_PLACE_NAME = "수정된 장소";
    public static final Boolean UPDATED_IS_PUBLIC = false;

    // 새로운 생성용 상수
    public static final String NEW_MOMENT_TITLE = "새로운 제목";
    public static final String NEW_MOMENT_CONTENT = "새로운 내용";
    public static final String NEW_PLACE_NAME = "새로운 장소";

    // 이미지 관련 상수
    public static final String TEST_IMAGE_1 = "image1.jpg";
    public static final String TEST_IMAGE_2 = "image2.jpg";
    public static final String UPDATED_IMAGE = "updated_image.jpg";
    public static final List<String> TEST_IMAGES = Arrays.asList(TEST_IMAGE_1, TEST_IMAGE_2);
    public static final List<String> UPDATED_IMAGES = Arrays.asList(UPDATED_IMAGE);
    public static final int TEST_IMAGES_COUNT = 2;
    public static final int UPDATED_IMAGES_COUNT = 1;

    // 순서 테스트용 이미지
    public static final String FIRST_IMAGE = "first.jpg";
    public static final String SECOND_IMAGE = "second.jpg";
    public static final String THIRD_IMAGE = "third.jpg";
    public static final List<String> ORDERED_IMAGES = Arrays.asList(FIRST_IMAGE, SECOND_IMAGE, THIRD_IMAGE);

    // 페이징 관련 상수
    public static final int DEFAULT_PAGE_LIMIT = 10;
    public static final int MAX_PAGE_LIMIT = 50;
    public static final String TEST_CURSOR = "2024-01-01T00:00:00.000Z";

    // API 응답 메시지 상수
    public static final String MOMENT_CREATED_MESSAGE = "moment_created";
    public static final String MOMENT_UPDATED_MESSAGE = "moment_updated";
    public static final String MOMENT_DELETED_MESSAGE = "moment_deleted";
    public static final String MOMENT_DETAIL_SUCCESS_MESSAGE = "place_moment_get_success";
    public static final String MOMENT_LIST_SUCCESS_MESSAGE = "all_moment_list_get_success";
    public static final String USER_MOMENT_LIST_SUCCESS_MESSAGE = "user_moment_list_get_success";
    public static final String PLACE_MOMENT_LIST_SUCCESS_MESSAGE = "place_moment_list_get_success";

    // 에러 메시지 상수
    public static final String MOMENT_NOT_FOUND_MESSAGE = "기록을 찾을 수 없습니다.";
    public static final String ACCESS_DENIED_MESSAGE = "접근 권한이 없습니다.";
    public static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    public static final String TITLE_REQUIRED_MESSAGE = "제목은 필수입니다";
    public static final String CONTENT_REQUIRED_MESSAGE = "내용은 필수입니다";
    public static final String TITLE_LENGTH_MESSAGE = "제목은 50자 이내여야 합니다";
    public static final String CONTENT_LENGTH_MESSAGE = "내용은 1000자 이내여야 합니다";
    public static final String PLACE_NAME_LENGTH_MESSAGE = "장소명은 100자 이내여야 합니다";

    // API 경로 상수
    public static final String MOMENTS_BASE_PATH = "/api/v1/users/moments";
    public static final String MOMENT_DETAIL_PATH = "/api/v1/moments/{moment_id}";
    public static final String MY_MOMENTS_PATH = "/api/v1/users/me/moments";
    public static final String USER_MOMENTS_PATH = "/api/v1/users/{user_id}/moments";
    public static final String PLACE_MOMENTS_PATH = "/api/v1/places/{place_id}/moments";

    // 비즈니스 로직 관련 상수
    public static final boolean IS_OWNER = true;
    public static final boolean IS_NOT_OWNER = false;
    public static final boolean HAS_NEXT = true;
    public static final boolean NO_NEXT = false;

    // 유효성 검증 관련 상수
    public static final int MAX_TITLE_LENGTH = 50;
    public static final int MAX_CONTENT_LENGTH = 1000;
    public static final int MAX_PLACE_NAME_LENGTH = 100;

    // 테스트용 긴 문자열 생성
    public static final String LONG_TITLE = "A".repeat(MAX_TITLE_LENGTH + 1);
    public static final String LONG_CONTENT = "A".repeat(MAX_CONTENT_LENGTH + 1);
    public static final String LONG_PLACE_NAME = "A".repeat(MAX_PLACE_NAME_LENGTH + 1);

    private MomentTestConstants() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }
}
