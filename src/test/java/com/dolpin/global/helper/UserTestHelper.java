package com.dolpin.global.helper;

import com.dolpin.domain.user.entity.User;
import com.dolpin.global.constants.UserTestConstants;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class UserTestHelper {

    // === 객체 생성 메서드 (영속화하지 않음) ===
    public User createUser(Long providerId, String provider, String username) {
        return User.builder()
                .providerId(providerId)
                .provider(provider)
                .username(username)
                .build();
    }

    public User createUser(Long providerId, String provider, String username, String imageUrl, String introduction) {
        return User.builder()
                .providerId(providerId)
                .provider(provider)
                .username(username)
                .imageUrl(imageUrl)
                .introduction(introduction)
                .build();
    }

    public User createUserWithAllFields(Long providerId, String provider, String username,
                                        String imageUrl, String introduction, boolean privacyAgreed, boolean locationAgreed) {
        return User.builder()
                .providerId(providerId)
                .provider(provider)
                .username(username)
                .imageUrl(imageUrl)
                .introduction(introduction)
                .isPrivacyAgreed(privacyAgreed)
                .isLocationAgreed(locationAgreed)
                .build();
    }

    // === 영속화 메서드 (테스트에서 필요시 호출) ===
    public User createAndSaveUser(TestEntityManager entityManager, Long providerId, String provider, String username) {
        User user = createUser(providerId, provider, username);
        return entityManager.persistAndFlush(user);
    }

    public User createAndSaveUser(TestEntityManager entityManager, Long providerId, String provider,
                                  String username, String imageUrl, String introduction) {
        User user = createUser(providerId, provider, username, imageUrl, introduction);
        return entityManager.persistAndFlush(user);
    }

    // === 검증 헬퍼 메서드들 ===
    public void assertUserBasicInfo(User user, Long expectedProviderId, String expectedProvider, String expectedUsername) {
        assertThat(user.getProviderId()).isEqualTo(expectedProviderId);
        assertThat(user.getProvider()).isEqualTo(expectedProvider);
        assertThat(user.getUsername()).isEqualTo(expectedUsername);
    }

    public void assertUserDetails(User user, String expectedUsername, String expectedImageUrl, String expectedIntroduction) {
        assertThat(user.getUsername()).isEqualTo(expectedUsername);
        assertThat(user.getImageUrl()).isEqualTo(expectedImageUrl);
        assertThat(user.getIntroduction()).isEqualTo(expectedIntroduction);
    }

    public void assertUserAgreements(User user, boolean expectedPrivacyAgreed, boolean expectedLocationAgreed) {
        assertThat(user.isPrivacyAgreed()).isEqualTo(expectedPrivacyAgreed);
        assertThat(user.isLocationAgreed()).isEqualTo(expectedLocationAgreed);
    }

    public void assertConstraintViolation(Exception exception) {
        assertThat(exception.getMessage()).containsAnyOf(
                UserTestConstants.CONSTRAINT_VIOLATION_KEYWORDS,
                UserTestConstants.UNIQUE_VIOLATION_KEYWORDS,
                UserTestConstants.DUPLICATE_VIOLATION_KEYWORDS);
    }

    public void assertTimestampsSame(User user) {
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isEqualTo(user.getUpdatedAt());
    }

    public void assertUpdatedAtChanged(User user, java.time.LocalDateTime originalUpdatedAt) {
        assertThat(user.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    public void assertPrivacyAgreedAtSet(User user, java.time.LocalDateTime beforeAgreement) {
        assertThat(user.getPrivacyAgreedAt()).isNotNull();
        assertThat(user.getPrivacyAgreedAt()).isAfter(beforeAgreement);
    }
}
