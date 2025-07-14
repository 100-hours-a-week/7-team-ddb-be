package com.dolpin.domain.auth.service.token;

import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.auth.entity.enums.TokenStatus;
import com.dolpin.domain.auth.repository.TokenRepository;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.auth.service.cache.RefreshTokenCacheService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.redis.util.CacheKeyUtil;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository; // 기존 DB 레포지토리는 유지 (마이그레이션용)
    private final RefreshTokenCacheService refreshTokenCacheService; // 새로 추가
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Token createRefreshToken(User user) {
        // 1. 기존 유효한 토큰 확인 (Redis에서)
        cleanupExpiredTokensForUser(user.getId());

        // 2. 새 리프레시 토큰 생성
        String refreshToken = jwtTokenProvider.generateToken(user.getId());
        String tokenHash = generateTokenHash(refreshToken);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(14);

        // 3. Redis에 저장
        RefreshTokenCacheService.RefreshTokenData tokenData =
                RefreshTokenCacheService.RefreshTokenData.builder()
                        .userId(user.getId())
                        .token(refreshToken)
                        .createdAt(now)
                        .expiredAt(expiryDate)
                        .isRevoked(false)
                        .build();

        refreshTokenCacheService.saveRefreshToken(tokenHash, tokenData);

        // 4. DB에도 저장 (기존 호환성 유지 - 나중에 제거 가능)
        Token dbToken = Token.builder()
                .user(user)
                .token(refreshToken)
                .status(TokenStatus.ACTIVE)
                .createdAt(now)
                .expiredAt(expiryDate)
                .isRevoked(false)
                .build();

        log.info("새 리프레시 토큰 생성: userId={}", user.getId());
        return tokenRepository.save(dbToken);
    }

    @Transactional
    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        String tokenHash = generateTokenHash(refreshToken);

        // Redis에서 토큰 검증
        if (!refreshTokenCacheService.isValidToken(tokenHash)) {
            throw new BusinessException(
                    ResponseStatus.UNAUTHORIZED.withMessage("리프레시 토큰이 유효하지 않습니다."));
        }

        RefreshTokenCacheService.RefreshTokenData tokenData =
                refreshTokenCacheService.getRefreshToken(tokenHash);

        if (tokenData == null) {
            throw new BusinessException(
                    ResponseStatus.UNAUTHORIZED.withMessage("리프레시 토큰을 찾을 수 없습니다."));
        }

        // 새 액세스 토큰 생성
        String newAccessToken = jwtTokenProvider.generateToken(tokenData.getUserId());

        log.info("액세스 토큰 갱신 완료: userId={}", tokenData.getUserId());
        return RefreshTokenResponse.builder()
                .newAccessToken(newAccessToken)
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .build();
    }

    @Transactional
    public void invalidateRefreshToken(String refreshToken) {
        String tokenHash = generateTokenHash(refreshToken);

        // Redis에서 토큰 무효화
        refreshTokenCacheService.blacklistToken(tokenHash);
        refreshTokenCacheService.deleteRefreshToken(tokenHash);

        // DB에서도 무효화 (기존 호환성)
        tokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.revoke();
            tokenRepository.save(token);
        });

        log.info("리프레시 토큰 무효화 완료");
    }

    @Transactional
    public void invalidateUserTokens(User user) {
        // Redis에서 사용자의 모든 토큰 무효화
        refreshTokenCacheService.invalidateUserTokens(user.getId());

        // DB에서도 무효화 (기존 호환성)
        List<Token> userTokens = tokenRepository.findAllByUser(user);
        userTokens.forEach(Token::revoke);
        tokenRepository.saveAll(userTokens);

        log.info("사용자 모든 토큰 무효화 완료: userId={}", user.getId());
    }

    // ===================== 헬퍼 메서드 =====================

    private String generateTokenHash(String token) {
        return CacheKeyUtil.generateHash(token);
    }

    private void cleanupExpiredTokensForUser(Long userId) {
        // Redis에서 만료된 토큰 정리는 자동으로 처리됨 (TTL)
        // 필요시 수동 정리 로직 추가 가능
    }

    @Transactional(readOnly = true)
    public boolean validateRefreshToken(String refreshToken) {
        String tokenHash = generateTokenHash(refreshToken);
        return refreshTokenCacheService.isValidToken(tokenHash);
    }
}
