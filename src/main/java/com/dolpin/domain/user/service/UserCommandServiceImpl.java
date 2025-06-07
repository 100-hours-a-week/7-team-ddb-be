package com.dolpin.domain.user.service;

import com.dolpin.domain.auth.repository.TokenRepository;
import com.dolpin.domain.auth.service.oauth.OAuthInfoResponse;
import com.dolpin.domain.auth.service.token.TokenService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.repository.UserRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final UserQueryService userQueryService;
    private final TokenRepository tokenRepository;
    private final TokenService tokenService;

    @Override
    @Transactional
    public User createUser(OAuthInfoResponse oAuthInfo) {

        // 유효한 providerId 확인
        if (oAuthInfo.getProviderId() == null || oAuthInfo.getProviderId().isEmpty()) {
            throw new BusinessException(ResponseStatus.INVALID_PARAMETER.withMessage("유효하지 않은 소셜 로그인 정보"));
        }

        // 2자로 설정하여 베이스명이 6자가 되도록 조정
        String tempUsername = "user" + oAuthInfo.getProviderId().substring(0, Math.min(2, oAuthInfo.getProviderId().length()));
        String username = generateUniqueUsername(tempUsername);

        User user = User.builder()
                .providerId(Long.parseLong(oAuthInfo.getProviderId()))
                .provider(oAuthInfo.getProvider())
                .username(username)
                .build();

        user.updateProfile(username, null, null);

        return userRepository.save(user);
    }

    @Transactional
    public void registerUser(Long userId, String nickname, String profileImage, String introduction) {
        User user = userQueryService.getUserById(userId);

        // 닉네임 중복 검사
        if (userRepository.existsByUsername(nickname) && !user.getUsername().equals(nickname)) {
            throw new BusinessException(ResponseStatus.NICKNAME_DUPLICATE);
        }

        // 프로필 정보 업데이트
        user.updateProfile(nickname, profileImage, introduction);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, String username, String imageUrl, String introduction) {
        User user = userQueryService.getUserById(userId);

        // 닉네임 변경 시 중복 검사
        if (username != null && !username.equals(user.getUsername()) &&
                userRepository.existsByUsername(username)) {
            throw new BusinessException(
                    ResponseStatus.NICKNAME_DUPLICATE.withMessage("이미 존재하는 닉네임입니다."));
        }

        user.updateProfile(username, imageUrl, introduction);
        userRepository.save(user);

        return user;
    }

    @Override
    @Transactional
    public void agreeToTerms(Long userId, boolean isPrivacyAgreed, boolean isLocationAgreed) {
        User user = userQueryService.getUserById(userId);
        user.agreeToTerms(isPrivacyAgreed, isLocationAgreed);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userQueryService.getUserById(userId);

        tokenService.invalidateUserTokens(user);

        tokenRepository.deleteAllByUser(user);

        userRepository.delete(user);
    }

    private String generateUniqueUsername(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            nickname = "user";
        }

        // 6자로 늘려서 더 많은 조합 가능 (접미사 공간 4자 확보)
        if (nickname.length() > 6) {
            nickname = nickname.substring(0, 6);
        }

        String candidateUsername = nickname;
        String suffix = "";
        int attempt = 0;

        while (userRepository.existsByUsername(candidateUsername)) {
            attempt++;
            suffix = generateAlphanumericSuffix(attempt);

            // 기본명 + 접미사가 10자를 초과하면 기본명을 줄임
            while (nickname.length() + suffix.length() > 10) {
                nickname = nickname.substring(0, nickname.length() - 1);
            }

            candidateUsername = nickname + suffix;
        }

        return candidateUsername;
    }

    private String generateAlphanumericSuffix(int number) {
        StringBuilder suffix = new StringBuilder();

        while (number > 0) {
            number--;
            suffix.insert(0, (char)('a' + (number % 26)));
            number /= 26;
        }

        return suffix.toString();
    }
}
