package com.dolpin.domain.auth.service.token;

import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.auth.entity.enums.TokenStatus;
import com.dolpin.domain.auth.repository.TokenRepository;
import com.dolpin.domain.auth.dto.response.RefreshTokenResponse;
import com.dolpin.domain.user.entity.User;
import com.dolpin.global.exception.BusinessException;
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

    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Token createRefreshToken(User user) {
        // 1. 먼저 유효한 기존 토큰이 있는지 확인
        List<Token> validTokens = tokenRepository.findValidTokensByUserId(user.getId());

        if (!validTokens.isEmpty()) {
            // 유효한 토큰이 있으면 재사용
            Token existingToken = validTokens.get(0);
            log.info("기존 유효한 리프레시 토큰 재사용 - 사용자: {}, 만료일: {}",
                    user.getId(), existingToken.getExpiredAt());
            return existingToken;
        }

        // 2. 유효한 토큰이 없을 때만 새로 생성
        // 만료된 토큰들만 정리
        cleanupExpiredTokens(user);

        // 새 리프레시 토큰 생성
        String refreshToken = jwtTokenProvider.generateToken(user.getId());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(14); // 리프레시 토큰 유효기간 2주

        Token token = Token.builder()
                .user(user)
                .token(refreshToken)
                .status(TokenStatus.ACTIVE)
                .createdAt(now)
                .expiredAt(expiryDate)
                .isRevoked(false)
                .build();

        log.info("새 리프레시 토큰 생성 - 사용자: {}, 만료일: {}", user.getId(), expiryDate);
        return tokenRepository.save(token);
    }

    // 만료된 토큰만 정리하는 메서드
    @Transactional
    public void cleanupExpiredTokens(User user) {
        List<Token> expiredTokens = tokenRepository.findAllByUser(user)
                .stream()
                .filter(Token::isExpired)
                .collect(Collectors.toList());

        if (!expiredTokens.isEmpty()) {
            expiredTokens.forEach(Token::revoke);
            tokenRepository.saveAll(expiredTokens);
            log.info("만료된 토큰 {}개 정리 완료 - 사용자: {}", expiredTokens.size(), user.getId());
        }
    }

    // 모든 토큰 무효화 (로그아웃, 계정 삭제 시에만 사용)
    @Transactional
    public void invalidateUserTokens(User user) {
        List<Token> userTokens = tokenRepository.findAllByUser(user);
        userTokens.forEach(Token::revoke);
        tokenRepository.saveAll(userTokens);
        log.info("사용자 모든 토큰 무효화 완료 - 사용자: {}, 토큰 수: {}", user.getId(), userTokens.size());
    }

    @Transactional
    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        // 리프레시 토큰 조회
        Token token = tokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(
                        ResponseStatus.UNAUTHORIZED.withMessage("리프레시 토큰이 유효하지 않습니다.")));

        // 만료 또는 취소된 토큰 체크
        if (token.isExpired()) {
            throw new BusinessException(
                    ResponseStatus.UNAUTHORIZED.withMessage("리프레시 토큰이 만료되었습니다."));
        }

        // 새 액세스 토큰 생성
        String newAccessToken = jwtTokenProvider.generateToken(token.getUser().getId());

        log.info("액세스 토큰 갱신 완료 - 사용자: {}", token.getUser().getId());
        return RefreshTokenResponse.builder()
                .newAccessToken(newAccessToken)
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .build();
    }

    @Transactional
    public void invalidateRefreshToken(String refreshToken) {
        Token token = tokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(
                        ResponseStatus.UNAUTHORIZED.withMessage("리프레시 토큰이 유효하지 않습니다.")));

        token.revoke();
        tokenRepository.save(token);
        log.info("리프레시 토큰 무효화 완료 - 사용자: {}", token.getUser().getId());
    }

    @Transactional(readOnly = true)
    public boolean validateRefreshToken(String refreshToken) {
        return tokenRepository.findByToken(refreshToken)
                .map(token -> !token.isExpired())
                .orElse(false);
    }
}
