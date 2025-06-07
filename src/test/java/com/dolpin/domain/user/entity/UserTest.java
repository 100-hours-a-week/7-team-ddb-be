package com.dolpin.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 엔티티 테스트")
class UserTest {

    @Nested
    @DisplayName("엔티티 생성 및 초기화 테스트")
    class EntityCreationTest {

        @Test
        @DisplayName("User 생성 시 createdAt, updatedAt이 자동 설정된다")
        void prePersist_SetsTimestamps() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .build();

            // when
            user.prePersist();

            // then
            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isNotNull();
            assertThat(user.getCreatedAt()).isEqualTo(user.getUpdatedAt());
            assertThat(user.isPrivacyAgreed()).isFalse();
            assertThat(user.isLocationAgreed()).isFalse();
        }

        @Test
        @DisplayName("User 수정 시 updatedAt이 갱신된다")
        void preUpdate_UpdatesTimestamp() throws InterruptedException {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .build();
            user.prePersist();
            LocalDateTime originalUpdatedAt = user.getUpdatedAt();

            Thread.sleep(1);

            // when
            user.preUpdate();

            // then
            assertThat(user.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("프로필 업데이트 테스트")
    class UpdateProfileTest {

        @Test
        @DisplayName("모든 프로필 정보를 정상적으로 업데이트한다")
        void updateProfile_WithAllFields_UpdatesSuccessfully() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("olduser")
                    .imageUrl("old-image.jpg")
                    .introduction("기존 소개글")
                    .build();

            // when
            user.updateProfile("newuser", "new-image.jpg", "새로운 소개글");

            // then
            assertThat(user.getUsername()).isEqualTo("newuser");
            assertThat(user.getImageUrl()).isEqualTo("new-image.jpg");
            assertThat(user.getIntroduction()).isEqualTo("새로운 소개글");
        }

        @Test
        @DisplayName("닉네임만 업데이트할 수 있다")
        void updateProfile_WithUsernameOnly_UpdatesUsernameOnly() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("olduser")
                    .imageUrl("old-image.jpg")
                    .introduction("기존 소개글")
                    .build();

            // when
            user.updateProfile("newuser", null, null);

            // then
            assertThat(user.getUsername()).isEqualTo("newuser");
            assertThat(user.getImageUrl()).isEqualTo("old-image.jpg");
            assertThat(user.getIntroduction()).isEqualTo("기존 소개글");
        }

        @Test
        @DisplayName("이미지 URL만 업데이트할 수 있다")
        void updateProfile_WithImageUrlOnly_UpdatesImageUrlOnly() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .imageUrl("old-image.jpg")
                    .introduction("기존 소개글")
                    .build();

            // when
            user.updateProfile(null, "new-image.jpg", null);

            // then
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getImageUrl()).isEqualTo("new-image.jpg");
            assertThat(user.getIntroduction()).isEqualTo("기존 소개글");
        }

        @Test
        @DisplayName("소개글만 업데이트할 수 있다")
        void updateProfile_WithIntroductionOnly_UpdatesIntroductionOnly() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .imageUrl("old-image.jpg")
                    .introduction("기존 소개글")
                    .build();

            // when
            user.updateProfile(null, null, "새로운 소개글");

            // then
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getImageUrl()).isEqualTo("old-image.jpg");
            assertThat(user.getIntroduction()).isEqualTo("새로운 소개글");
        }

        @Test
        @DisplayName("null 값들은 기존 값을 유지한다")
        void updateProfile_WithNullValues_MaintainsExistingValues() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .imageUrl("existing-image.jpg")
                    .introduction("기존 소개글")
                    .build();

            // when
            user.updateProfile(null, null, null);

            // then
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getImageUrl()).isEqualTo("existing-image.jpg");
            assertThat(user.getIntroduction()).isEqualTo("기존 소개글");
        }
    }

    @Nested
    @DisplayName("약관 동의 테스트")
    class AgreeToTermsTest {

        @Test
        @DisplayName("개인정보 및 위치정보 동의 시 정상적으로 설정된다")
        void agreeToTerms_WithBothAgreements_SetsAgreementsCorrectly() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .build();
            user.prePersist(); // 초기 상태 설정

            LocalDateTime beforeAgreement = LocalDateTime.now().minusSeconds(1);

            // when
            user.agreeToTerms(true, true);

            // then
            assertThat(user.isPrivacyAgreed()).isTrue();
            assertThat(user.isLocationAgreed()).isTrue();
            assertThat(user.getPrivacyAgreedAt()).isNotNull();
            assertThat(user.getPrivacyAgreedAt()).isAfter(beforeAgreement);
        }
    }

    @Nested
    @DisplayName("Builder 패턴 테스트")
    class BuilderTest {

        @Test
        @DisplayName("Builder를 통해 User 객체를 정상적으로 생성할 수 있다")
        void builder_CreatesUserSuccessfully() {
            // given & when
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .imageUrl("profile.jpg")
                    .introduction("안녕하세요!")
                    .build();

            // then
            assertThat(user.getProviderId()).isEqualTo(12345L);
            assertThat(user.getProvider()).isEqualTo("kakao");
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getImageUrl()).isEqualTo("profile.jpg");
            assertThat(user.getIntroduction()).isEqualTo("안녕하세요!");
        }

        @Test
        @DisplayName("Builder로 최소 필수 정보만으로 User를 생성할 수 있다")
        void builder_WithMinimalInfo_CreatesUserSuccessfully() {
            // given & when
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .build();

            // then
            assertThat(user.getProviderId()).isEqualTo(12345L);
            assertThat(user.getProvider()).isEqualTo("kakao");
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getImageUrl()).isNull();
            assertThat(user.getIntroduction()).isNull();
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("매우 긴 소개글도 정상적으로 처리된다")
        void updateProfile_WithLongIntroduction_HandlesCorrectly() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .build();

            String longIntroduction = "안녕하세요! ".repeat(10);

            // when
            user.updateProfile(null, null, longIntroduction);

            // then
            assertThat(user.getIntroduction()).isEqualTo(longIntroduction);
        }

        @Test
        @DisplayName("특수문자가 포함된 닉네임도 정상적으로 처리된다")
        void updateProfile_WithSpecialCharacters_HandlesCorrectly() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("olduser")
                    .build();

            String specialUsername = "test@123";

            // when
            user.updateProfile(specialUsername, null, null);

            // then
            assertThat(user.getUsername()).isEqualTo(specialUsername);
        }

        @Test
        @DisplayName("한글 닉네임도 정상적으로 처리된다")
        void updateProfile_WithKoreanUsername_HandlesCorrectly() {
            // given
            User user = User.builder()
                    .providerId(12345L)
                    .provider("kakao")
                    .username("olduser")
                    .build();

            String koreanUsername = "테스트유저";

            // when
            user.updateProfile(koreanUsername, null, null);

            // then
            assertThat(user.getUsername()).isEqualTo(koreanUsername);
        }
    }
}
