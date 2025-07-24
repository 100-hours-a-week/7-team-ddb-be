package com.dolpin.domain.auth.service.session;

import com.dolpin.domain.auth.service.token.JwtTokenProvider;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionValidationService {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 현재 세션의 유효성을 검증합니다.
     *
     * @return 세션이 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateCurrentSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나 인증되지 않은 경우
        if (!isAuthenticated(authentication)) {
            log.debug("인증되지 않은 요청 - 세션 무효");
            return false;
        }

        try {
            // UserDetails에서 사용자 ID 추출
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long userId = Long.parseLong(userDetails.getUsername());

            log.debug("세션 검증 성공 - 사용자 ID: {}", userId);
            return true;

        } catch (NumberFormatException e) {
            log.warn("잘못된 사용자 ID 형식: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("세션 검증 중 예상치 못한 오류 발생", e);
            // 예상치 못한 시스템 오류만 예외로 처리
            throw new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류입니다.");
        }
    }

    /**
     * Authentication 객체가 유효한 인증 상태인지 확인
     */
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails
                && !isAnonymousUser(authentication);
    }

    /**
     * 익명 사용자인지 확인
     */
    private boolean isAnonymousUser(Authentication authentication) {
        return "anonymousUser".equals(authentication.getPrincipal());
    }
}
