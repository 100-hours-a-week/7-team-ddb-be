package com.dolpin.global.constants;

public final class UserTestConstants {

    private UserTestConstants() {
    }

    // === 사용자 ID 상수 ===
    public static final Long USER_ID_1 = 1L;
    public static final Long USER_ID_2 = 2L;
    public static final Long NON_EXISTENT_USER_ID = 999L;

    // === Provider 관련 상수 ===
    public static final String KAKAO_PROVIDER = "kakao";
    public static final String GOOGLE_PROVIDER = "google";
    public static final Long PROVIDER_ID_VALID = 12345L;
    public static final Long PROVIDER_ID_SHORT = 5L;
    public static final String PROVIDER_ID_VALID_STRING = "12345";
    public static final String PROVIDER_ID_SHORT_STRING = "5";

    // === 사용자명 관련 상수 ===
    public static final String USERNAME_TEST = "testuser";
    public static final String USERNAME_OLD = "olduser";
    public static final String USERNAME_NEW = "newuser";
    public static final String USERNAME_EXISTING = "existuser";
    public static final String USERNAME_NON_EXISTENT = "nouser";
    public static final String USERNAME_DUPLICATE = "중복닉네임";
    public static final String USERNAME_UPDATE = "업데이트닉네임";
    public static final String USERNAME_CURRENT = "currentuser";
    public static final String USERNAME_SAME = "sameuser";
    public static final String USERNAME_DELETE = "deleteuser";
    public static final String USERNAME_KOREAN = "테스트유저";
    public static final String USERNAME_SPECIAL = "test@123";
    public static final String USERNAME_GENERATED_BASE = "user12";
    public static final String USERNAME_GENERATED_UNIQUE = "user12a";
    public static final String USERNAME_GENERATED_SHORT = "user5";
    public static final String USERNAME_OTHER = "otheruser";

    // === 이미지 URL 상수 ===
    public static final String IMAGE_URL_OLD = "old-image.jpg";
    public static final String IMAGE_URL_NEW = "new-image.jpg";
    public static final String IMAGE_URL_PROFILE = "profile.jpg";
    public static final String IMAGE_URL_OTHER = "other.jpg";
    public static final String IMAGE_URL_EXISTING = "existing-image.jpg";
    public static final String IMAGE_URL_UPDATE = "new_profile.jpg";

    // === 소개글 상수 ===
    public static final String INTRODUCTION_OLD = "기존 소개글";
    public static final String INTRODUCTION_NEW = "새로운 소개글";
    public static final String INTRODUCTION_HELLO = "안녕하세요!";
    public static final String INTRODUCTION_OTHER = "다른사용자입니다";
    public static final String INTRODUCTION_UPDATE = "업데이트된 소개";
    public static final String INTRODUCTION_LONG = "안녕하세요! ".repeat(10);

    // === API 응답 메시지 상수 ===
    public static final String SUCCESS_MESSAGE_AGREEMENT_SAVED = "agreement_saved";
    public static final String SUCCESS_MESSAGE_USER_INFO_SAVED = "user_info_saved";
    public static final String SUCCESS_MESSAGE_RETRIEVE_SUCCESS = "retrieve_user_info_success";
    public static final String SUCCESS_MESSAGE_USER_INFO_UPDATED = "user_info_updated";
    public static final String SUCCESS_MESSAGE_USER_DELETE = "user_delete_success";

    // === 에러 메시지 상수 ===
    public static final String ERROR_MESSAGE_USER_NOT_FOUND = "기록을 찾을 수 없습니다.";
    public static final String ERROR_MESSAGE_NICKNAME_DUPLICATE = "이미 존재하는 닉네임입니다.";
    public static final String ERROR_MESSAGE_PRIVACY_AGREEMENT_REQUIRED = "개인정보 동의 여부는 필수입니다";
    public static final String ERROR_MESSAGE_INVALID_OAUTH_INFO = "유효하지 않은 소셜 로그인 정보";
    public static final String ERROR_MESSAGE_SERVER_ERROR = "서버 내부 오류";

    // === 닉네임 길이 제한 상수 ===
    public static final String NICKNAME_TOO_SHORT = "a";
    public static final String NICKNAME_MIN_LENGTH = "테스트닉네임";

    // === 쿠키 관련 상수 ===
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String TEST_REFRESH_TOKEN = "test_refresh_token";

    // === 약관 동의 관련 상수 ===
    public static final boolean PRIVACY_AGREED = true;
    public static final boolean LOCATION_AGREED = true;
    public static final boolean PRIVACY_NOT_AGREED = false;
    public static final boolean LOCATION_NOT_AGREED = false;

    // === 데이터베이스 제약조건 관련 상수 ===
    public static final String CONSTRAINT_VIOLATION_KEYWORDS = "constraint";
    public static final String UNIQUE_VIOLATION_KEYWORDS = "unique";
    public static final String DUPLICATE_VIOLATION_KEYWORDS = "duplicate";
}
