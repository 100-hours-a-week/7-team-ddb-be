package com.dolpin.domain.user.service;

import com.dolpin.domain.auth.service.oauth.OAuthInfoResponse;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.repository.UserRepository;
import com.dolpin.domain.user.service.UserQueryService;
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

    @Override
    @Transactional
    public User createUser(OAuthInfoResponse oAuthInfo) {
        log.info("Creating new user with provider: {}, providerId: {}",
                oAuthInfo.getProvider(), oAuthInfo.getProviderId());

        String username = generateUniqueUsername(oAuthInfo.getNickname());

        User user = User.builder()
                .providerId(Long.parseLong(oAuthInfo.getProviderId()))
                .provider(oAuthInfo.getProvider())
                .username(username)
                .build();

        // 프로필 정보 업데이트
        user.updateProfile(username, oAuthInfo.getProfileImageUrl(), null);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, String username, String imageUrl, String introduction) {
        User user = userQueryService.getUserById(userId);
        user.updateProfile(username, imageUrl, introduction);
        userRepository.save(user);
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
        userRepository.delete(user);
    }

    private String generateUniqueUsername(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            nickname = "user";
        }

        // 이름이 10자를 초과하면 잘라냄 (username 필드 길이 제한 때문)
        if (nickname.length() > 8) {
            nickname = nickname.substring(0, 8);
        }

        String candidateUsername = nickname;
        int suffix = 1;

        while (userRepository.existsByUsername(candidateUsername)) {
            suffix++;
            candidateUsername = nickname + suffix;

            // 이름 + 접미사가 10자를 초과하면 이름을 더 자름
            if (candidateUsername.length() > 10) {
                nickname = nickname.substring(0, nickname.length() - 1);
                candidateUsername = nickname + suffix;
            }
        }

        return candidateUsername;
    }
}