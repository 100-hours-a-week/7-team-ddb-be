package com.dolpin.domain.user.entity;

import com.dolpin.global.constants.UserTestConstants;
import com.dolpin.global.helper.UserTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 엔티티 테스트")
class UserTest {

    private final UserTestHelper userTestHelper = new UserTestHelper();

    @Nested
    @DisplayName("엔티티 생성 및 초기화 테스트")
    class EntityCreationTest {

        @Test
        @DisplayName("User 생성 시 createdAt, updatedAt이 자동 설정된다")
        void prePersist_SetsTimestamps() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);

            // when
            user.prePersist();

            // then
            userTestHelper.assertTimestampsSame(user);
            userTestHelper.assertUserAgreements(user, UserTestConstants.PRIVACY_NOT_AGREED, UserTestConstants.LOCATION_NOT_AGREED);
        }

        @Test
        @DisplayName("User 수정 시 updatedAt이 갱신된다")
        void preUpdate_UpdatesTimestamp() throws InterruptedException {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);
            user.prePersist();
            LocalDateTime originalUpdatedAt = user.getUpdatedAt();

            Thread.sleep(1);

            // when
            user.preUpdate();

            // then
            userTestHelper.assertUpdatedAtChanged(user, originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("프로필 업데이트 테스트")
    class UpdateProfileTest {

        @Test
        @DisplayName("모든 프로필 정보를 정상적으로 업데이트한다")
        void updateProfile_WithAllFields_UpdatesSuccessfully() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_OLD,
                    UserTestConstants.IMAGE_URL_OLD, UserTestConstants.INTRODUCTION_OLD);

            // when
            user.updateProfile(UserTestConstants.USERNAME_NEW, UserTestConstants.IMAGE_URL_NEW, UserTestConstants.INTRODUCTION_NEW);

            // then
            userTestHelper.assertUserDetails(user, UserTestConstants.USERNAME_NEW,
                    UserTestConstants.IMAGE_URL_NEW, UserTestConstants.INTRODUCTION_NEW);
        }

        @Test
        @DisplayName("닉네임만 업데이트할 수 있다")
        void updateProfile_WithUsernameOnly_UpdatesUsernameOnly() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_OLD,
                    UserTestConstants.IMAGE_URL_OLD, UserTestConstants.INTRODUCTION_OLD);

            // when
            user.updateProfile(UserTestConstants.USERNAME_NEW, null, null);

            // then
            assertThat(user.getUsername()).isEqualTo(UserTestConstants.USERNAME_NEW);
            assertThat(user.getImageUrl()).isEqualTo(UserTestConstants.IMAGE_URL_OLD);
            assertThat(user.getIntroduction()).isEqualTo(UserTestConstants.INTRODUCTION_OLD);
        }

        @Test
        @DisplayName("이미지 URL만 업데이트할 수 있다")
        void updateProfile_WithImageUrlOnly_UpdatesImageUrlOnly() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_OLD, UserTestConstants.INTRODUCTION_OLD);

            // when
            user.updateProfile(null, UserTestConstants.IMAGE_URL_NEW, null);

            // then
            assertThat(user.getUsername()).isEqualTo(UserTestConstants.USERNAME_TEST);
            assertThat(user.getImageUrl()).isEqualTo(UserTestConstants.IMAGE_URL_NEW);
            assertThat(user.getIntroduction()).isEqualTo(UserTestConstants.INTRODUCTION_OLD);
        }

        @Test
        @DisplayName("소개글만 업데이트할 수 있다")
        void updateProfile_WithIntroductionOnly_UpdatesIntroductionOnly() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_OLD, UserTestConstants.INTRODUCTION_OLD);

            // when
            user.updateProfile(null, null, UserTestConstants.INTRODUCTION_NEW);

            // then
            assertThat(user.getUsername()).isEqualTo(UserTestConstants.USERNAME_TEST);
            assertThat(user.getImageUrl()).isEqualTo(UserTestConstants.IMAGE_URL_OLD);
            assertThat(user.getIntroduction()).isEqualTo(UserTestConstants.INTRODUCTION_NEW);
        }

        @Test
        @DisplayName("null 값들은 기존 값을 유지한다")
        void updateProfile_WithNullValues_MaintainsExistingValues() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_EXISTING, UserTestConstants.INTRODUCTION_OLD);

            // when
            user.updateProfile(null, null, null);

            // then
            userTestHelper.assertUserDetails(user, UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_EXISTING, UserTestConstants.INTRODUCTION_OLD);
        }
    }

    @Nested
    @DisplayName("약관 동의 테스트")
    class AgreeToTermsTest {

        @Test
        @DisplayName("개인정보 및 위치정보 동의 시 정상적으로 설정된다")
        void agreeToTerms_WithBothAgreements_SetsAgreementsCorrectly() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);
            user.prePersist();

            LocalDateTime beforeAgreement = LocalDateTime.now().minusSeconds(1);

            // when
            user.agreeToTerms(UserTestConstants.PRIVACY_AGREED, UserTestConstants.LOCATION_AGREED);

            // then
            userTestHelper.assertUserAgreements(user, UserTestConstants.PRIVACY_AGREED, UserTestConstants.LOCATION_AGREED);
            userTestHelper.assertPrivacyAgreedAtSet(user, beforeAgreement);
        }
    }

    @Nested
    @DisplayName("Builder 패턴 테스트")
    class BuilderTest {

        @Test
        @DisplayName("Builder를 통해 User 객체를 정상적으로 생성할 수 있다")
        void builder_CreatesUserSuccessfully() {
            // given & when
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            // then
            userTestHelper.assertUserBasicInfo(user, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);
            userTestHelper.assertUserDetails(user, UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);
        }

        @Test
        @DisplayName("Builder로 최소 필수 정보만으로 User를 생성할 수 있다")
        void builder_WithMinimalInfo_CreatesUserSuccessfully() {
            // given & when
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);

            // then
            userTestHelper.assertUserBasicInfo(user, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);
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
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);

            // when
            user.updateProfile(null, null, UserTestConstants.INTRODUCTION_LONG);

            // then
            assertThat(user.getIntroduction()).isEqualTo(UserTestConstants.INTRODUCTION_LONG);
        }

        @Test
        @DisplayName("특수문자가 포함된 닉네임도 정상적으로 처리된다")
        void updateProfile_WithSpecialCharacters_HandlesCorrectly() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_OLD);

            // when
            user.updateProfile(UserTestConstants.USERNAME_SPECIAL, null, null);

            // then
            assertThat(user.getUsername()).isEqualTo(UserTestConstants.USERNAME_SPECIAL);
        }

        @Test
        @DisplayName("한글 닉네임도 정상적으로 처리된다")
        void updateProfile_WithKoreanUsername_HandlesCorrectly() {
            // given
            User user = userTestHelper.createUser(UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_OLD);

            // when
            user.updateProfile(UserTestConstants.USERNAME_KOREAN, null, null);

            // then
            assertThat(user.getUsername()).isEqualTo(UserTestConstants.USERNAME_KOREAN);
        }
    }
}
