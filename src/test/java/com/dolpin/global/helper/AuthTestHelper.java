package com.dolpin.global.helper;

import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.auth.entity.enums.TokenStatus;
import com.dolpin.domain.user.entity.User;
import com.dolpin.global.constants.AuthTestConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 인증 관련 테스트에서 사용하는 헬퍼 메서드 모음
 */
public class AuthTestHelper {

    /**
     * 테스트용 User 객체 생성 (영속화하지 않음)
     */
    public static User createUser() {
        return User.builder()
                .providerId(AuthTestConstants.TEST_PROVIDER_ID)
                .provider(AuthTestConstants.TEST_PROVIDER)
                .username(AuthTestConstants.TEST_USERNAME)
                .build();
    }

    /**
     * 테스트용 User 객체 생성 (커스텀 값 사용, 영속화하지 않음)
     */
    public static User createUser(Long id, String username, Long providerId) {
        return User.builder()
                .id(id)
                .providerId(providerId)
                .provider(AuthTestConstants.TEST_PROVIDER)
                .username(username)
                .build();
    }

    /**
     * 테스트용 Token 객체 생성 (영속화하지 않음)
     */
    public static Token createToken(User user, String tokenValue, LocalDateTime expiredAt, boolean isRevoked) {
        return Token.builder()
                .user(user)
                .status(TokenStatus.ACTIVE)
                .token(tokenValue)
                .createdAt(LocalDateTime.now())
                .expiredAt(expiredAt)
                .isRevoked(isRevoked)
                .build();
    }

    /**
     * 유효한 테스트용 Token 객체 생성 (영속화하지 않음)
     */
    public static Token createValidToken(User user, String tokenValue) {
        return createToken(user, tokenValue, LocalDateTime.now().plusDays(7), false);
    }

    /**
     * 만료된 테스트용 Token 객체 생성 (영속화하지 않음)
     */
    public static Token createExpiredToken(User user, String tokenValue) {
        return createToken(user, tokenValue, LocalDateTime.now().minusDays(1), false);
    }

    /**
     * 취소된 테스트용 Token 객체 생성 (영속화하지 않음)
     */
    public static Token createRevokedToken(User user, String tokenValue) {
        return createToken(user, tokenValue, LocalDateTime.now().plusDays(1), true);
    }

    /**
     * 다른 키로 서명된 JWT 토큰 생성
     */
    public static String createTokenWithDifferentKey(long expirationMs) {
        SecretKey differentKey = Keys.hmacShaKeyFor(AuthTestConstants.DIFFERENT_SECRET_KEY.getBytes());
        return Jwts.builder()
                .subject(AuthTestConstants.TEST_USER_ID.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(differentKey)
                .compact();
    }

    /**
     * 만료된 JWT 토큰 생성
     */
    public static String createExpiredJwtToken(String secretKey) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        return Jwts.builder()
                .subject(AuthTestConstants.TEST_USER_ID.toString())
                .issuedAt(new Date(System.currentTimeMillis() - AuthTestConstants.TEST_EXPIRATION_MS - 1000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();
    }

    /**
     * 시간 범위 검증 헬퍼
     */
    public static boolean isWithinTimeRange(long actual, long expected, long toleranceMs) {
        return Math.abs(actual - expected) <= toleranceMs;
    }

    private AuthTestHelper() {
        // 유틸리티 클래스는 인스턴스화 방지
    }
}
