package com.dolpin.global.constants;

/**
 * 인증 관련 테스트에서 사용하는 상수 정의
 */
public class AuthTestConstants {

    // JWT 토큰 관련 상수
    public static final String TEST_SECRET_KEY = "test-secret-key-for-jwt-token-provider-testing-purpose";
    public static final String VALID_SECRET_KEY = "valid-secret-key-for-testing-purpose-32chars";
    public static final String DIFFERENT_SECRET_KEY = "different-secret-key-for-testing-purpose-32chars";
    public static final String SHORT_SECRET_KEY = "short";
    public static final long TEST_EXPIRATION_MS = 3600000L; // 1시간

    // 토큰 관련 상수
    public static final String VALID_TOKEN_VALUE = "valid-token-123";
    public static final String EXISTING_TOKEN_VALUE = "existing-token";
    public static final String NON_EXISTENT_TOKEN_VALUE = "non-existent-token";
    public static final String EXPIRED_TOKEN_VALUE = "expired-token";
    public static final String REVOKED_TOKEN_VALUE = "revoked-token";
    public static final String EXPIRED_REVOKED_TOKEN_VALUE = "expired-revoked-token";
    public static final String INVALID_TOKEN_FORMAT = "invalid.token.format";

    // 사용자 관련 상수
    public static final Long TEST_USER_ID = 12345L;
    public static final Long TEST_USER_ID_2 = 67890L;
    public static final Long TEST_USER_ID_3 = 33333L;
    public static final Long TEST_PROVIDER_ID = 12345L;
    public static final Long TEST_PROVIDER_ID_2 = 67890L;
    public static final String TEST_PROVIDER = "kakao";
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_USERNAME_2 = "user1";
    public static final String TEST_USERNAME_3 = "user2";

    // OAuth 관련 상수
    public static final String TEST_AUTH_CODE = "test-auth-code";
    public static final String INVALID_AUTH_CODE = "invalid-auth-code";
    public static final String TEST_REDIRECT_URI = "http://localhost:3000/auth/callback";
    public static final String OAUTH_ACCESS_TOKEN = "oauth-access-token";
    public static final String JWT_ACCESS_TOKEN = "jwt-access-token";
    public static final String NEW_ACCESS_TOKEN = "new-access-token";
    public static final String REFRESH_TOKEN_VALUE = "refresh-token";
    public static final String VALID_REFRESH_TOKEN = "valid-refresh-token";
    public static final String EXPIRED_REFRESH_TOKEN = "expired-refresh-token";
    public static final String KAKAO_LOGIN_URL = "https://kauth.kakao.com/oauth/authorize?client_id=test&redirect_uri=%s&response_type=code";
    public static final String GOOGLE_LOGIN_URL = "https://accounts.google.com/oauth/authorize?client_id=test&redirect_uri=%s&response_type=code";

    // 프로바이더 관련 상수
    public static final String KAKAO_PROVIDER = "kakao";
    public static final String GOOGLE_PROVIDER = "google";
    public static final String UNSUPPORTED_PROVIDER = "naver";

    // 에러 메시지 상수
    public static final String OAUTH_AUTH_FAILED_MESSAGE = "OAuth 인증에 실패했습니다";
    public static final String REFRESH_TOKEN_INVALID_MESSAGE = "리프레시 토큰이 유효하지 않습니다";
    public static final String REFRESH_TOKEN_EXPIRED_MESSAGE = "리프레시 토큰이 만료되었습니다";
    public static final String UNKNOWN_OAUTH_PROVIDER_MESSAGE = "Unknown OAuth provider: ";

    private AuthTestConstants() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
