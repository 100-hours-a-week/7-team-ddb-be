package com.dolpin.domain.auth.service.token;

import com.dolpin.global.helper.AuthTestHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static com.dolpin.global.constants.AuthTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", TEST_SECRET_KEY);
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", TEST_EXPIRATION_MS);
        jwtTokenProvider.init();
    }

    @Nested
    @DisplayName("init 메서드 테스트")
    class InitTest {

        @Test
        @DisplayName("정상적인 비밀키로 초기화가 성공한다")
        void init_WithValidSecretKey_InitializesSuccessfully() {
            // given
            JwtTokenProvider provider = new JwtTokenProvider();
            ReflectionTestUtils.setField(provider, "secretKey", VALID_SECRET_KEY);
            ReflectionTestUtils.setField(provider, "expirationMs", TEST_EXPIRATION_MS);

            // when & then
            provider.init(); // 예외가 발생하지 않아야 함
        }

        @Test
        @DisplayName("짧은 비밀키도 32바이트로 패딩되어 초기화된다")
        void init_WithShortSecretKey_PadsTo32Bytes() {
            // given
            JwtTokenProvider provider = new JwtTokenProvider();
            ReflectionTestUtils.setField(provider, "secretKey", SHORT_SECRET_KEY);
            ReflectionTestUtils.setField(provider, "expirationMs", TEST_EXPIRATION_MS);

            // when & then
            provider.init();
        }
    }

    @Nested
    @DisplayName("generateToken 메서드 테스트")
    class GenerateTokenTest {

        @Test
        @DisplayName("사용자 ID로 JWT 토큰을 정상적으로 생성한다")
        void generateToken_WithValidUserId_ReturnsToken() {
            // given
            Long userId = TEST_USER_ID;

            // when
            String token = jwtTokenProvider.generateToken(userId);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT는 3개 부분으로 구성
        }

        @Test
        @DisplayName("같은 사용자 ID로 생성된 토큰의 subject는 동일하다")
        void generateToken_WithSameUserId_HasSameSubject() {
            // given
            Long userId = TEST_USER_ID;

            // when
            String token1 = jwtTokenProvider.generateToken(userId);
            String token2 = jwtTokenProvider.generateToken(userId);

            // then
            Long extractedUserId1 = jwtTokenProvider.getUserIdFromToken(token1);
            Long extractedUserId2 = jwtTokenProvider.getUserIdFromToken(token2);

            assertThat(extractedUserId1).isEqualTo(userId);
            assertThat(extractedUserId2).isEqualTo(userId);
            assertThat(extractedUserId1).isEqualTo(extractedUserId2);
        }

        @Test
        @DisplayName("토큰에 만료 시간이 올바르게 설정된다")
        void generateToken_SetsCorrectExpirationTime() {
            // given
            Long userId = TEST_USER_ID;
            long beforeGeneration = System.currentTimeMillis();

            // when
            String token = jwtTokenProvider.generateToken(userId);
            long afterGeneration = System.currentTimeMillis();

            // then
            SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            Date issuedAt = claims.getIssuedAt();

            assertThat(expiration.getTime() - issuedAt.getTime()).isEqualTo(TEST_EXPIRATION_MS);
            assertThat(AuthTestHelper.isWithinTimeRange(issuedAt.getTime(), beforeGeneration, 1000)).isTrue();
            assertThat(expiration.getTime()).isGreaterThan(System.currentTimeMillis());
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken 메서드 테스트")
    class GetUserIdFromTokenTest {

        @Test
        @DisplayName("유효한 토큰에서 사용자 ID를 정상적으로 추출한다")
        void getUserIdFromToken_WithValidToken_ReturnsUserId() {
            // given
            Long expectedUserId = TEST_USER_ID;
            String token = jwtTokenProvider.generateToken(expectedUserId);

            // when
            Long actualUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(actualUserId).isEqualTo(expectedUserId);
        }

        @Test
        @DisplayName("잘못된 형식의 토큰에서 사용자 ID 추출 시 예외가 발생한다")
        void getUserIdFromToken_WithInvalidToken_ThrowsException() {
            // given
            String invalidToken = INVALID_TOKEN_FORMAT;

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken(invalidToken))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰에서 사용자 ID 추출 시 예외가 발생한다")
        void getUserIdFromToken_WithTokenSignedByDifferentKey_ThrowsException() {
            // given
            String tokenWithDifferentKey = AuthTestHelper.createTokenWithDifferentKey(TEST_EXPIRATION_MS);

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken(tokenWithDifferentKey))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("validateToken 메서드 테스트")
    class ValidateTokenTest {

        @Test
        @DisplayName("유효한 토큰 검증 시 true를 반환한다")
        void validateToken_WithValidToken_ReturnsTrue() {
            // given
            String token = jwtTokenProvider.generateToken(TEST_USER_ID);

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰 검증 시 false를 반환한다")
        void validateToken_WithInvalidFormatToken_ReturnsFalse() {
            // given
            String invalidToken = INVALID_TOKEN_FORMAT;

            // when
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰 검증 시 false를 반환한다")
        void validateToken_WithTokenSignedByDifferentKey_ReturnsFalse() {
            // given
            String tokenWithDifferentKey = AuthTestHelper.createTokenWithDifferentKey(TEST_EXPIRATION_MS);

            // when
            boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentKey);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰 검증 시 false를 반환한다")
        void validateToken_WithExpiredToken_ReturnsFalse() {
            // given
            String expiredToken = AuthTestHelper.createExpiredJwtToken(TEST_SECRET_KEY);

            // when
            boolean isValid = jwtTokenProvider.validateToken(expiredToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("null 토큰 검증 시 false를 반환한다")
        void validateToken_WithNullToken_ReturnsFalse() {
            // when
            boolean isValid = jwtTokenProvider.validateToken(null);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 토큰 검증 시 false를 반환한다")
        void validateToken_WithEmptyToken_ReturnsFalse() {
            // when
            boolean isValid = jwtTokenProvider.validateToken("");

            // then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("getExpirationMs 메서드 테스트")
    class GetExpirationMsTest {

        @Test
        @DisplayName("설정된 만료 시간을 정상적으로 반환한다")
        void getExpirationMs_ReturnsConfiguredExpiration() {
            // when
            long expirationMs = jwtTokenProvider.getExpirationMs();

            // then
            assertThat(expirationMs).isEqualTo(TEST_EXPIRATION_MS);
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("토큰 생성부터 검증까지 전체 플로우가 정상 동작한다")
        void fullTokenFlow_WorksCorrectly() {
            // given
            Long userId = TEST_USER_ID;

            // when
            String token = jwtTokenProvider.generateToken(userId);
            boolean isValid = jwtTokenProvider.validateToken(token);
            Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(isValid).isTrue();
            assertThat(extractedUserId).isEqualTo(userId);
        }

        @Test
        @DisplayName("여러 사용자의 토큰을 동시에 처리해도 정상 동작한다")
        void multipleUserTokens_WorkCorrectly() {
            // given
            Long userId1 = TEST_USER_ID;
            Long userId2 = TEST_USER_ID_2;
            Long userId3 = TEST_USER_ID_3;

            // when
            String token1 = jwtTokenProvider.generateToken(userId1);
            String token2 = jwtTokenProvider.generateToken(userId2);
            String token3 = jwtTokenProvider.generateToken(userId3);

            // then
            assertThat(jwtTokenProvider.validateToken(token1)).isTrue();
            assertThat(jwtTokenProvider.validateToken(token2)).isTrue();
            assertThat(jwtTokenProvider.validateToken(token3)).isTrue();

            assertThat(jwtTokenProvider.getUserIdFromToken(token1)).isEqualTo(userId1);
            assertThat(jwtTokenProvider.getUserIdFromToken(token2)).isEqualTo(userId2);
            assertThat(jwtTokenProvider.getUserIdFromToken(token3)).isEqualTo(userId3);
        }
    }
}
