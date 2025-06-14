package com.dolpin.domain.auth.entity;

import com.dolpin.domain.auth.entity.enums.TokenStatus;
import com.dolpin.global.helper.AuthTestHelper;
import com.dolpin.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.dolpin.global.constants.AuthTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Token 엔티티 테스트")
class TokenTest {

    @Test
    @DisplayName("Token 생성 시 모든 필드가 올바르게 설정된다")
    void tokenCreation_SetsAllFields() {
        // given
        User user = AuthTestHelper.createUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(14);

        // when
        Token token = Token.builder()
                .user(user)
                .status(TokenStatus.ACTIVE)
                .token(VALID_TOKEN_VALUE)
                .createdAt(now)
                .expiredAt(expiredAt)
                .isRevoked(false)
                .build();

        // then
        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getStatus()).isEqualTo(TokenStatus.ACTIVE);
        assertThat(token.getToken()).isEqualTo(VALID_TOKEN_VALUE);
        assertThat(token.getCreatedAt()).isEqualTo(now);
        assertThat(token.getExpiredAt()).isEqualTo(expiredAt);
        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("토큰이 만료되지 않았고 취소되지 않은 경우 isExpired는 false를 반환한다")
    void isExpired_WhenTokenIsValid_ReturnsFalse() {
        // given
        LocalDateTime futureExpiry = LocalDateTime.now().plusDays(1);
        Token token = Token.builder()
                .expiredAt(futureExpiry)
                .isRevoked(false)
                .build();

        // when
        boolean isExpired = token.isExpired();

        // then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("토큰이 만료된 경우 isExpired는 true를 반환한다")
    void isExpired_WhenTokenIsExpired_ReturnsTrue() {
        // given
        LocalDateTime pastExpiry = LocalDateTime.now().minusDays(1);
        Token token = Token.builder()
                .expiredAt(pastExpiry)
                .isRevoked(false)
                .build();

        // when
        boolean isExpired = token.isExpired();

        // then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("토큰이 취소된 경우 isExpired는 true를 반환한다")
    void isExpired_WhenTokenIsRevoked_ReturnsTrue() {
        // given
        LocalDateTime futureExpiry = LocalDateTime.now().plusDays(1);
        Token token = Token.builder()
                .expiredAt(futureExpiry)
                .isRevoked(true)
                .build();

        // when
        boolean isExpired = token.isExpired();

        // then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("토큰이 만료되고 취소된 경우 isExpired는 true를 반환한다")
    void isExpired_WhenTokenIsExpiredAndRevoked_ReturnsTrue() {
        // given
        LocalDateTime pastExpiry = LocalDateTime.now().minusDays(1);
        Token token = Token.builder()
                .expiredAt(pastExpiry)
                .isRevoked(true)
                .build();

        // when
        boolean isExpired = token.isExpired();

        // then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("revoke 호출 시 isRevoked가 true로 설정된다")
    void revoke_SetsIsRevokedToTrue() {
        // given
        Token token = Token.builder()
                .isRevoked(false)
                .build();

        // when
        token.revoke();

        // then
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("이미 취소된 토큰에 revoke 호출 시에도 정상 작동한다")
    void revoke_WhenAlreadyRevoked_WorksNormally() {
        // given
        Token token = Token.builder()
                .isRevoked(true)
                .build();

        // when
        token.revoke();

        // then
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Helper를 사용한 Token 생성이 정상 동작한다")
    void createTokenWithHelper_WorksCorrectly() {
        // given
        User user = AuthTestHelper.createUser();
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(7);

        // when
        Token validToken = AuthTestHelper.createValidToken(user, VALID_TOKEN_VALUE);
        Token expiredToken = AuthTestHelper.createExpiredToken(user, EXPIRED_TOKEN_VALUE);
        Token revokedToken = AuthTestHelper.createRevokedToken(user, REVOKED_TOKEN_VALUE);

        // then
        assertThat(validToken.isExpired()).isFalse();
        assertThat(expiredToken.isExpired()).isTrue();
        assertThat(revokedToken.isExpired()).isTrue();

        assertThat(validToken.getToken()).isEqualTo(VALID_TOKEN_VALUE);
        assertThat(expiredToken.getToken()).isEqualTo(EXPIRED_TOKEN_VALUE);
        assertThat(revokedToken.getToken()).isEqualTo(REVOKED_TOKEN_VALUE);
    }
}
