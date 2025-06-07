package com.dolpin.domain.user.service;

import com.dolpin.domain.auth.repository.TokenRepository;
import com.dolpin.domain.auth.service.oauth.OAuthInfoResponse;
import com.dolpin.domain.auth.service.token.TokenService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.repository.UserRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCommandService 테스트")
class UserCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UserCommandServiceImpl userCommandService;

    @Nested
    @DisplayName("createUser 메서드 테스트")
    class CreateUserTest {

        @Test
        @DisplayName("유효한 OAuth 정보로 사용자를 정상적으로 생성한다")
        void createUser_WithValidOAuthInfo_CreatesUserSuccessfully() {
            // given
            OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
            given(oAuthInfo.getProviderId()).willReturn("12345");
            given(oAuthInfo.getProvider()).willReturn("kakao");

            // user + 2자 = "user12"
            given(userRepository.existsByUsername("user12")).willReturn(false);

            User savedUser = User.builder()
                    .id(1L)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("user12")
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userCommandService.createUser(oAuthInfo);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getProviderId()).isEqualTo(12345L);
            assertThat(result.getProvider()).isEqualTo("kakao");
            assertThat(result.getUsername()).isEqualTo("user12");

            verify(userRepository).existsByUsername("user12");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("null ProviderId로 사용자 생성 시 BusinessException을 발생시킨다")
        void createUser_WithNullProviderId_ThrowsBusinessException() {
            // given
            OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
            given(oAuthInfo.getProviderId()).willReturn(null);
            // provider는 사용되지 않으므로 stubbing 제거

            // when & then
            assertThatThrownBy(() -> userCommandService.createUser(oAuthInfo))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 소셜 로그인 정보")
                    .extracting("responseStatus")
                    .extracting("httpStatus")
                    .isEqualTo(ResponseStatus.INVALID_PARAMETER.getHttpStatus());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("빈 ProviderId로 사용자 생성 시 BusinessException을 발생시킨다")
        void createUser_WithEmptyProviderId_ThrowsBusinessException() {
            // given
            OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
            given(oAuthInfo.getProviderId()).willReturn("");
            // provider는 사용되지 않으므로 stubbing 제거

            // when & then
            assertThatThrownBy(() -> userCommandService.createUser(oAuthInfo))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 소셜 로그인 정보");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("중복되는 사용자명이 있을 때 고유한 사용자명을 생성한다")
        void createUser_WithDuplicateUsername_GeneratesUniqueUsername() {
            // given
            OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
            given(oAuthInfo.getProviderId()).willReturn("12345");
            given(oAuthInfo.getProvider()).willReturn("kakao");

            // user12는 중복, user12a는 고유
            given(userRepository.existsByUsername("user12")).willReturn(true);
            given(userRepository.existsByUsername("user12a")).willReturn(false);

            User savedUser = User.builder()
                    .id(1L)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("user12a")
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userCommandService.createUser(oAuthInfo);

            // then
            assertThat(result.getUsername()).isEqualTo("user12a");

            verify(userRepository).existsByUsername("user12");
            verify(userRepository).existsByUsername("user12a");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("짧은 ProviderId도 적절히 처리한다")
        void createUser_WithShortProviderId_HandlesCorrectly() {
            // given
            OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
            given(oAuthInfo.getProviderId()).willReturn("5");
            given(oAuthInfo.getProvider()).willReturn("kakao");

            given(userRepository.existsByUsername("user5")).willReturn(false);

            User savedUser = User.builder()
                    .id(1L)
                    .providerId(5L)
                    .provider("kakao")
                    .username("user5")
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userCommandService.createUser(oAuthInfo);

            // then
            assertThat(result.getProviderId()).isEqualTo(5L);
            assertThat(result.getUsername()).isEqualTo("user5");

            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("registerUser 메서드 테스트")
    class RegisterUserTest {

        @Test
        @DisplayName("사용자 등록을 정상적으로 수행한다")
        void registerUser_WithValidData_RegistersSuccessfully() {
            // given
            Long userId = 1L;
            String nickname = "newuser";
            String profileImage = "profile.jpg";
            String introduction = "안녕하세요!";

            User existingUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("olduser")
                    .build();

            given(userQueryService.getUserById(userId)).willReturn(existingUser);
            given(userRepository.existsByUsername(nickname)).willReturn(false);

            // when
            userCommandService.registerUser(userId, nickname, profileImage, introduction);

            // then
            verify(userQueryService).getUserById(userId);
            verify(userRepository).existsByUsername(nickname);
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("중복된 닉네임으로 등록 시 BusinessException을 발생시킨다")
        void registerUser_WithDuplicateNickname_ThrowsBusinessException() {
            // given
            Long userId = 1L;
            String duplicateNickname = "existinguser";

            User existingUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("olduser")
                    .build();

            given(userQueryService.getUserById(userId)).willReturn(existingUser);
            given(userRepository.existsByUsername(duplicateNickname)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userCommandService.registerUser(userId, duplicateNickname, "profile.jpg", "intro"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.NICKNAME_DUPLICATE);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("현재 사용자명과 동일한 닉네임으로는 등록할 수 있다")
        void registerUser_WithSameCurrentUsername_RegistersSuccessfully() {
            // given
            Long userId = 1L;
            String currentUsername = "currentuser";

            User existingUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username(currentUsername)
                    .build();

            given(userQueryService.getUserById(userId)).willReturn(existingUser);
            given(userRepository.existsByUsername(currentUsername)).willReturn(true); // 자신의 닉네임은 존재

            // when
            userCommandService.registerUser(userId, currentUsername, "new-profile.jpg", "new intro");

            // then
            verify(userRepository).save(existingUser);
        }
    }

    @Nested
    @DisplayName("updateProfile 메서드 테스트")
    class UpdateProfileTest {

        @Test
        @DisplayName("프로필을 정상적으로 업데이트한다")
        void updateProfile_WithValidData_UpdatesSuccessfully() {
            // given
            Long userId = 1L;
            String newUsername = "newusername";
            String newImageUrl = "new-image.jpg";
            String newIntroduction = "새로운 소개글";

            User existingUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("olduser")
                    .imageUrl("old-image.jpg")
                    .introduction("기존 소개글")
                    .build();

            given(userQueryService.getUserById(userId)).willReturn(existingUser);
            given(userRepository.existsByUsername(newUsername)).willReturn(false);

            // when
            User result = userCommandService.updateProfile(userId, newUsername, newImageUrl, newIntroduction);

            // then
            assertThat(result.getId()).isEqualTo(userId);
            verify(userQueryService).getUserById(userId);
            verify(userRepository).existsByUsername(newUsername);
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("중복된 닉네임으로 업데이트 시 BusinessException을 발생시킨다")
        void updateProfile_WithDuplicateUsername_ThrowsBusinessException() {
            // given
            Long userId = 1L;
            String duplicateUsername = "existinguser";

            User existingUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("currentuser")
                    .build();

            given(userQueryService.getUserById(userId)).willReturn(existingUser);
            given(userRepository.existsByUsername(duplicateUsername)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userCommandService.updateProfile(userId, duplicateUsername, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("이미 존재하는 닉네임입니다.")
                    .extracting("responseStatus")
                    .extracting("httpStatus")
                    .isEqualTo(ResponseStatus.NICKNAME_DUPLICATE.getHttpStatus());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("현재 사용자명과 동일한 경우 중복 검사를 통과한다")
        void updateProfile_WithSameCurrentUsername_UpdatesSuccessfully() {
            // given
            Long userId = 1L;
            String currentUsername = "currentuser";

            User existingUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username(currentUsername)
                    .build();

            given(userQueryService.getUserById(userId)).willReturn(existingUser);
            // existsByUsername 호출되지 않음 (같은 사용자명이므로)

            // when
            User result = userCommandService.updateProfile(userId, currentUsername, "new-image.jpg", "new intro");

            // then
            assertThat(result.getId()).isEqualTo(userId);
            verify(userRepository, never()).existsByUsername(anyString());
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("null 사용자명으로 업데이트 시 중복 검사를 하지 않는다")
        void updateProfile_WithNullUsername_SkipsDuplicateCheck() {
            // given
            Long userId = 1L;
            User existingUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("currentuser")
                    .build();

            given(userQueryService.getUserById(userId)).willReturn(existingUser);

            // when
            User result = userCommandService.updateProfile(userId, null, "new-image.jpg", "new intro");

            // then
            assertThat(result.getId()).isEqualTo(userId);
            verify(userRepository, never()).existsByUsername(anyString());
            verify(userRepository).save(existingUser);
        }
    }

    @Nested
    @DisplayName("agreeToTerms 메서드 테스트")
    class AgreeToTermsTest {

        @Test
        @DisplayName("약관 동의를 정상적으로 처리한다")
        void agreeToTerms_WithValidData_AgreesSuccessfully() {
            // given
            Long userId = 1L;
            boolean isPrivacyAgreed = true;
            boolean isLocationAgreed = true;

            User existingUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .build();

            given(userQueryService.getUserById(userId)).willReturn(existingUser);

            // when
            userCommandService.agreeToTerms(userId, isPrivacyAgreed, isLocationAgreed);

            // then
            verify(userQueryService).getUserById(userId);
            verify(userRepository).save(existingUser);
        }
    }

    @Nested
    @DisplayName("deleteUser 메서드 테스트")
    class DeleteUserTest {

        @Test
        @DisplayName("사용자를 정상적으로 삭제한다")
        void deleteUser_WithValidId_DeletesSuccessfully() {
            // given
            Long userId = 1L;
            User existingUser = User.builder()
                    .id(userId)
                    .providerId(12345L)
                    .provider("kakao")
                    .username("testuser")
                    .build();

            given(userQueryService.getUserById(userId)).willReturn(existingUser);

            // when
            userCommandService.deleteUser(userId);

            // then
            verify(userQueryService).getUserById(userId);
            verify(tokenService).invalidateUserTokens(existingUser);
            verify(tokenRepository).deleteAllByUser(existingUser);
            verify(userRepository).delete(existingUser);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 삭제 시 getUserById에서 예외가 발생한다")
        void deleteUser_WithNonExistentId_ThrowsExceptionFromGetUserById() {
            // given
            Long nonExistentId = 999L;
            given(userQueryService.getUserById(nonExistentId))
                    .willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> userCommandService.deleteUser(nonExistentId))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.USER_NOT_FOUND);

            verify(userQueryService).getUserById(nonExistentId);
            verify(tokenService, never()).invalidateUserTokens(any(User.class));
            verify(tokenRepository, never()).deleteAllByUser(any(User.class));
            verify(userRepository, never()).delete(any(User.class));
        }
    }

    @Nested
    @DisplayName("generateUniqueUsername private 메서드 간접 테스트")
    class GenerateUniqueUsernameTest {

        @Test
        @DisplayName("ProviderId 앞 2자를 사용하여 베이스 사용자명을 생성한다")
        void createUser_WithProviderId_GeneratesBaseUsername() {
            // given
            OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
            given(oAuthInfo.getProviderId()).willReturn("123456789");
            given(oAuthInfo.getProvider()).willReturn("kakao");

            // user + 2자 = "user12" (6자)
            given(userRepository.existsByUsername("user12")).willReturn(false);

            User savedUser = User.builder()
                    .id(1L)
                    .providerId(123456789L)
                    .provider("kakao")
                    .username("user12")
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userCommandService.createUser(oAuthInfo);

            // then
            assertThat(result.getUsername()).isEqualTo("user12");
            verify(userRepository).existsByUsername("user12");
        }

        @Test
        @DisplayName("중복된 사용자명이 있을 때 알파벳 접미사로 고유 사용자명을 생성한다")
        void createUser_WithDuplicateUsername_GeneratesUniqueUsernameWithAlphabetSuffix() {
            // given
            OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
            given(oAuthInfo.getProviderId()).willReturn("123456");
            given(oAuthInfo.getProvider()).willReturn("kakao");

            // user + 2자 = "user12"
            given(userRepository.existsByUsername("user12")).willReturn(true);   // 베이스 중복
            given(userRepository.existsByUsername("user12a")).willReturn(false); // 첫 번째 접미사 성공

            User savedUser = User.builder()
                    .id(1L)
                    .providerId(123456L)
                    .provider("kakao")
                    .username("user12a")
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userCommandService.createUser(oAuthInfo);

            // then
            assertThat(result.getUsername()).isEqualTo("user12a");
            verify(userRepository).existsByUsername("user12");
            verify(userRepository).existsByUsername("user12a");
        }

        @Test
        @DisplayName("많은 중복 상황에서도 적절한 알파벳 접미사를 생성한다")
        void createUser_WithManyDuplicates_GeneratesCorrectAlphabetSuffix() {
            // given
            OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
            given(oAuthInfo.getProviderId()).willReturn("123456");
            given(oAuthInfo.getProvider()).willReturn("kakao");

            // 여러 중복 상황 시뮬레이션
            given(userRepository.existsByUsername("user12")).willReturn(true);   // 베이스
            given(userRepository.existsByUsername("user12a")).willReturn(true);  // 1번째 시도
            given(userRepository.existsByUsername("user12b")).willReturn(true);  // 2번째 시도
            given(userRepository.existsByUsername("user12c")).willReturn(false); // 3번째 시도 성공

            User savedUser = User.builder()
                    .id(1L)
                    .providerId(123456L)
                    .provider("kakao")
                    .username("user12c")
                    .build();
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userCommandService.createUser(oAuthInfo);

            // then
            assertThat(result.getUsername()).isEqualTo("user12c");
            verify(userRepository).existsByUsername("user12");
            verify(userRepository).existsByUsername("user12a");
            verify(userRepository).existsByUsername("user12b");
            verify(userRepository).existsByUsername("user12c");
        }
    }
}
