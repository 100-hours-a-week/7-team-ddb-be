package com.dolpin.global.redis.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringJoiner;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheKeyUtil {

    // 환경별 키 분리
    private static final String ENV_PREFIX = getEnvironmentPrefix();

    private static String getEnvironmentPrefix() {
        String profile = System.getProperty("spring.profiles.active", "local");
        return "dolpin:" + profile + ":";
    }


    // 장소 관련 
    public static String placeCategories() {
        return ENV_PREFIX + "place:categories:all";
    }

    public static String placeRegion(String category, double lat, double lng) {
        int latGrid = (int) (lat * 100);
        int lngGrid = (int) (lng * 100);
        return ENV_PREFIX + String.format("place:region:%s:%d:%d", category, latGrid, lngGrid);
    }

    // 북마크 관련 
    public static String bookmarkStatus(Long userId, Long placeId) {
        return ENV_PREFIX + String.format("bookmark:status:%d:%d", userId, placeId);
    }

    // 댓글/조회수 관련 
    public static String commentCount(Long momentId) {
        return ENV_PREFIX + "comment:count:" + momentId;
    }

    public static String momentViewCount(Long momentId) {
        return ENV_PREFIX + "moment:view_count:" + momentId;
    }

    // 토큰 관련 
    public static String refreshToken(String tokenHash) {
        return ENV_PREFIX + "refresh_token:" + tokenHash;
    }

    public static String userTokens(Long userId) {
        return ENV_PREFIX + "user_tokens:" + userId;
    }

    public static String blacklistToken(String tokenHash) {
        return ENV_PREFIX + "blacklist_token:" + tokenHash;
    }
    

    /**
     * 복합 키 생성 (북마크 목록 캐시 등에서 사용)
     */
    public static String generateCompositeKey(String prefix, Object... parts) {
        StringJoiner joiner = new StringJoiner(":");
        joiner.add(ENV_PREFIX + prefix);

        for (Object part : parts) {
            if (part != null) {
                joiner.add(part.toString());
            } else {
                joiner.add("null");
            }
        }

        return joiner.toString();
    }

    /**
     * 문자열을 SHA-256으로 해시화 (토큰 해시용)
     */
    public static String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().substring(0, 16); // 16자리만 사용
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            return String.valueOf(input.hashCode());
        }
    }

}
