package com.dolpin.domain.auth.service.token;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String testSecretKey = "test-secret-key-for-jwt-token-provider-testing-purpose";
    private final long testExpirationMs = 3600000L; // 1시간

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", testExpirationMs);
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
            ReflectionTestUtils.setField(provider, "secretKey", "valid-secret-key-for-testing-purpose-32chars");
            ReflectionTestUtils.setField(provider, "expirationMs", 3600000L);

            // when & then
            provider.init(); // 예외가 발생하지 않아야 함
        }

        @Test
        @DisplayName("짧은 비밀키도 32바이트로 패딩되어 초기화된다")
        void init_WithShortSecretKey_PadsTo32Bytes() {
            // given
            JwtTokenProvider provider = new JwtTokenProvider();
            ReflectionTestUtils.setField(provider, "secretKey", "short");
            ReflectionTestUtils.setField(provider, "expirationMs", 3600000L);

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
            Long userId = 12345L;

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
            Long userId = 12345L;

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
            Long userId = 12345L;
            long beforeGeneration = System.currentTimeMillis();

            // when
            String token = jwtTokenProvider.generateToken(userId);
            long afterGeneration = System.currentTimeMillis();

            // then
            SecretKey key = Keys.hmacShaKeyFor(testSecretKey.getBytes());
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            Date issuedAt = claims.getIssuedAt();

            assertThat(expiration.getTime() - issuedAt.getTime()).isEqualTo(testExpirationMs);

            assertThat(issuedAt.getTime()).isBetween(beforeGeneration - 1000, afterGeneration + 1000);

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
            Long expectedUserId = 12345L;
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
            String invalidToken = "invalid.token.format";

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken(invalidToken))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰에서 사용자 ID 추출 시 예외가 발생한다")
        void getUserIdFromToken_WithTokenSignedByDifferentKey_ThrowsException() {
            // given
            SecretKey differentKey = Keys.hmacShaKeyFor("different-secret-key-for-testing-purpose-32chars".getBytes());
            String tokenWithDifferentKey = Jwts.builder()
                    .subject("12345")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + testExpirationMs))
                    .signWith(differentKey)
                    .compact();

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
            Long userId = 12345L;
            String token = jwtTokenProvider.generateToken(userId);

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰 검증 시 false를 반환한다")
        void validateToken_WithInvalidFormatToken_ReturnsFalse() {
            // given
            String invalidToken = "invalid.token.format";

            // when
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰 검증 시 false를 반환한다")
        void validateToken_WithTokenSignedByDifferentKey_ReturnsFalse() {
            // given
            SecretKey differentKey = Keys.hmacShaKeyFor("different-secret-key-for-testing-purpose-32chars".getBytes());
            String tokenWithDifferentKey = Jwts.builder()
                    .subject("12345")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + testExpirationMs))
                    .signWith(differentKey)
                    .compact();

            // when
            boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentKey);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰 검증 시 false를 반환한다")
        void validateToken_WithExpiredToken_ReturnsFalse() {
            // given
            SecretKey key = Keys.hmacShaKeyFor(testSecretKey.getBytes());
            String expiredToken = Jwts.builder()
                    .subject("12345")
                    .issuedAt(new Date(System.currentTimeMillis() - testExpirationMs - 1000))
                    .expiration(new Date(System.currentTimeMillis() - 1000)) // 1초 전에 만료
                    .signWith(key)
                    .compact();

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
            assertThat(expirationMs).isEqualTo(testExpirationMs);
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("토큰 생성부터 검증까지 전체 플로우가 정상 동작한다")
        void fullTokenFlow_WorksCorrectly() {
            // given
            Long userId = 12345L;

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
            Long userId1 = 11111L;
            Long userId2 = 22222L;
            Long userId3 = 33333L;

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
