package com.dolpin.domain.auth.service.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:dolpinsecretkey}")
    private String secretKey;

    @Value("${jwt.expiration:3600000}")
    private long expirationMs; // 기본값 1시간

    private SecretKey key; // Key 대신 SecretKey 사용

    @PostConstruct
    public void init() {
        try {
            // 비밀키가 최소 32바이트(256비트) 이상이 되도록 보장
            if (secretKey.length() < 32) {
                secretKey = String.format("%-32s", secretKey).replace(' ', 'x');
            }
            // SecretKey로 변환
            this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        } catch (Exception e) {
            log.error("JWT token provider initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException("JWT token provider initialization failed", e);
        }
    }

    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key) // 이제 SecretKey로 정상 동작할 것입니다
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key) // 이제 SecretKey로 정상 동작할 것입니다
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT token validation error: {}", e.getMessage());
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}