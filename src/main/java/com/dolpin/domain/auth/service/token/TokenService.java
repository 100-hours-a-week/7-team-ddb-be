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

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Token createRefreshToken(User user) {
        // 기존 리프레시 토큰 무효화
        //invalidateUserTokens(user);

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

        return tokenRepository.save(token);
    }

    @Transactional
    public void invalidateUserTokens(User user) {
        List<Token> userTokens = tokenRepository.findAllByUser(user);
        userTokens.forEach(Token::revoke);
        tokenRepository.saveAll(userTokens);
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
        log.info("Refresh token invalidated for user: {}", token.getUser().getId());
    }

    @Transactional(readOnly = true)
    public boolean validateRefreshToken(String refreshToken) {
        return tokenRepository.findByToken(refreshToken)
                .map(token -> !token.isExpired())
                .orElse(false);
    }
}
