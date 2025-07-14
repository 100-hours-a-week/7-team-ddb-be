package com.dolpin.domain.auth.service.cache;

import com.dolpin.global.redis.service.RedisService;
import com.dolpin.global.redis.util.CacheKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenCacheService {

    private final RedisService redisService;

    // TTL 설정
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);    // 리프레시 토큰 14일
    private static final Duration USER_TOKENS_TTL = Duration.ofDays(15);      // 사용자 토큰 목록 15일
    private static final Duration BLACKLIST_TTL = Duration.ofDays(1);         // 블랙리스트 1일

    // ===================== 리프레시 토큰 관리 =====================

    /**
     * 리프레시 토큰 저장
     */
    public void saveRefreshToken(String tokenHash, RefreshTokenData tokenData) {
        String key = CacheKeyUtil.refreshToken(tokenHash);
        try {
            redisService.set(key, tokenData, REFRESH_TOKEN_TTL);

            // 사용자별 토큰 목록에도 추가
            addToUserTokens(tokenData.getUserId(), tokenHash);

            log.debug("리프레시 토큰 저장: userId={}, tokenHash={}",
                    tokenData.getUserId(), tokenHash.substring(0, 8) + "...");
        } catch (Exception e) {
            log.error("리프레시 토큰 저장 실패: tokenHash={}", tokenHash, e);
            throw new RuntimeException("리프레시 토큰 저장 실패", e);
        }
    }

    /**
     * 리프레시 토큰 조회
     */
    public RefreshTokenData getRefreshToken(String tokenHash) {
        String key = CacheKeyUtil.refreshToken(tokenHash);
        try {
            RefreshTokenData tokenData = redisService.get(key, RefreshTokenData.class);
            if (tokenData != null) {
                log.debug("리프레시 토큰 조회 성공: userId={}, tokenHash={}",
                        tokenData.getUserId(), tokenHash.substring(0, 8) + "...");
            } else {
                log.debug("리프레시 토큰 조회 실패: tokenHash={}", tokenHash.substring(0, 8) + "...");
            }
            return tokenData;
        } catch (Exception e) {
            log.warn("리프레시 토큰 조회 실패: tokenHash={}", tokenHash, e);
            return null;
        }
    }

    /**
     * 리프레시 토큰 삭제
     */
    public void deleteRefreshToken(String tokenHash) {
        String key = CacheKeyUtil.refreshToken(tokenHash);
        try {
            // 먼저 토큰 데이터 조회 (사용자 ID 필요)
            RefreshTokenData tokenData = getRefreshToken(tokenHash);

            // 토큰 삭제
            redisService.delete(key);

            // 사용자 토큰 목록에서도 제거
            if (tokenData != null) {
                removeFromUserTokens(tokenData.getUserId(), tokenHash);
            }

            log.debug("리프레시 토큰 삭제: tokenHash={}", tokenHash.substring(0, 8) + "...");
        } catch (Exception e) {
            log.warn("리프레시 토큰 삭제 실패: tokenHash={}", tokenHash, e);
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean isValidToken(String tokenHash) {
        try {
            // 1. 블랙리스트 체크
            if (isBlacklisted(tokenHash)) {
                return false;
            }

            // 2. 토큰 데이터 조회
            RefreshTokenData tokenData = getRefreshToken(tokenHash);
            if (tokenData == null) {
                return false;
            }

            // 3. 만료 시간 체크
            if (tokenData.getExpiredAt().isBefore(LocalDateTime.now())) {
                // 만료된 토큰은 비동기로 정리
                deleteRefreshToken(tokenHash);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("토큰 검증 실패: {}", tokenHash.substring(0, 8) + "...", e);
            return false;
        }
    }

    // ===================== 사용자별 토큰 관리 =====================

    /**
     * 사용자의 모든 토큰 무효화
     */
    public void invalidateUserTokens(Long userId) {
        try {
            Set<String> userTokenHashes = getUserTokens(userId);

            // 각 토큰을 블랙리스트에 추가
            for (String tokenHash : userTokenHashes) {
                blacklistToken(tokenHash);
                deleteRefreshToken(tokenHash);
            }

            // 사용자 토큰 목록 삭제
            String userTokensKey = CacheKeyUtil.userTokens(userId);
            redisService.delete(userTokensKey);

            log.info("사용자 모든 토큰 무효화: userId={}, tokenCount={}", userId, userTokenHashes.size());
        } catch (Exception e) {
            log.error("사용자 토큰 무효화 실패: userId={}", userId, e);
        }
    }

    /**
     * 사용자 토큰 목록 조회
     */
    @SuppressWarnings("unchecked")
    private Set<String> getUserTokens(Long userId) {
        String key = CacheKeyUtil.userTokens(userId);
        try {
            Set<Object> tokens = redisService.getSetMembers(key);
            return tokens.stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("사용자 토큰 목록 조회 실패: userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    /**
     * 사용자 토큰 목록에 추가
     */
    private void addToUserTokens(Long userId, String tokenHash) {
        String key = CacheKeyUtil.userTokens(userId);
        try {
            redisService.addToSet(key, tokenHash);
            redisService.expire(key, USER_TOKENS_TTL);
        } catch (Exception e) {
            log.warn("사용자 토큰 목록 추가 실패: userId={}, tokenHash={}", userId, tokenHash, e);
        }
    }

    /**
     * 사용자 토큰 목록에서 제거
     */
    private void removeFromUserTokens(Long userId, String tokenHash) {
        String key = CacheKeyUtil.userTokens(userId);
        try {
            redisService.removeFromSet(key, tokenHash);
        } catch (Exception e) {
            log.warn("사용자 토큰 목록 제거 실패: userId={}, tokenHash={}", userId, tokenHash, e);
        }
    }

    // ===================== 블랙리스트 관리 =====================

    /**
     * 토큰을 블랙리스트에 추가
     */
    public void blacklistToken(String tokenHash) {
        String key = CacheKeyUtil.blacklistToken(tokenHash);
        try {
            redisService.set(key, true, BLACKLIST_TTL);
            log.debug("토큰 블랙리스트 추가: tokenHash={}", tokenHash.substring(0, 8) + "...");
        } catch (Exception e) {
            log.warn("토큰 블랙리스트 추가 실패: tokenHash={}", tokenHash, e);
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String tokenHash) {
        String key = CacheKeyUtil.blacklistToken(tokenHash);
        try {
            return redisService.exists(key);
        } catch (Exception e) {
            log.warn("블랙리스트 확인 실패: tokenHash={}", tokenHash, e);
            return false;
        }
    }

    // ===================== 정리 작업 =====================

    /**
     * 만료된 토큰 정리 (스케줄러에서 호출)
     */
    @Async("bookmarkCacheExecutor")
    public void cleanupExpiredTokens() {
        try {
            Set<String> tokenKeys = redisService.getKeysByPattern("refresh_token:*");
            int cleanedCount = 0;

            for (String key : tokenKeys) {
                RefreshTokenData tokenData = redisService.get(key, RefreshTokenData.class);
                if (tokenData != null && tokenData.getExpiredAt().isBefore(LocalDateTime.now())) {
                    redisService.delete(key);
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0) {
                log.info("만료된 리프레시 토큰 정리 완료: {}개", cleanedCount);
            }
        } catch (Exception e) {
            log.error("만료된 토큰 정리 실패", e);
        }
    }

    // ===================== 데이터 클래스 =====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RefreshTokenData {
        private Long userId;
        private String token;
        private LocalDateTime createdAt;
        private LocalDateTime expiredAt;
        private boolean isRevoked;
    }
}
