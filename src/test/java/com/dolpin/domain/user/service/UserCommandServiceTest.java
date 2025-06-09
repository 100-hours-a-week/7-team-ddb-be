package com.dolpin.domain.user.service;

import com.dolpin.domain.auth.repository.TokenRepository;
import com.dolpin.domain.auth.service.oauth.OAuthInfoResponse;
import com.dolpin.domain.auth.service.token.TokenService;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.repository.UserRepository;
import com.dolpin.global.constants.UserTestConstants;
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
            OAuthInfoResponse oAuthInfo = createOAuthInfoStub(UserTestConstants.PROVIDER_ID_VALID_STRING, UserTestConstants.KAKAO_PROVIDER);

            given(userRepository.existsByUsername(UserTestConstants.USERNAME_GENERATED_BASE)).willReturn(false);

            User savedUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_GENERATED_BASE);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userCommandService.createUser(oAuthInfo);

            // then
            assertThat(result.getId()).isEqualTo(UserTestConstants.USER_ID_1);
            assertThat(result.getProviderId()).isEqualTo(UserTestConstants.PROVIDER_ID_VALID);
            assertThat(result.getProvider()).isEqualTo(UserTestConstants.KAKAO_PROVIDER);
            assertThat(result.getUsername()).isEqualTo(UserTestConstants.USERNAME_GENERATED_BASE);

            verify(userRepository).existsByUsername(UserTestConstants.USERNAME_GENERATED_BASE);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("null ProviderId로 사용자 생성 시 BusinessException을 발생시킨다")
        void createUser_WithNullProviderId_ThrowsBusinessException() {
            // given
            OAuthInfoResponse oAuthInfo = createOAuthInfoStubWithProviderIdOnly(null);

            // when & then
            assertThatThrownBy(() -> userCommandService.createUser(oAuthInfo))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserTestConstants.ERROR_MESSAGE_INVALID_OAUTH_INFO)
                    .extracting("responseStatus")
                    .extracting("httpStatus")
                    .isEqualTo(ResponseStatus.INVALID_PARAMETER.getHttpStatus());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("빈 ProviderId로 사용자 생성 시 BusinessException을 발생시킨다")
        void createUser_WithEmptyProviderId_ThrowsBusinessException() {
            // given
            OAuthInfoResponse oAuthInfo = createOAuthInfoStubWithProviderIdOnly("");

            // when & then
            assertThatThrownBy(() -> userCommandService.createUser(oAuthInfo))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserTestConstants.ERROR_MESSAGE_INVALID_OAUTH_INFO);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("중복되는 사용자명이 있을 때 고유한 사용자명을 생성한다")
        void createUser_WithDuplicateUsername_GeneratesUniqueUsername() {
            // given
            OAuthInfoResponse oAuthInfo = createOAuthInfoStub(UserTestConstants.PROVIDER_ID_VALID_STRING, UserTestConstants.KAKAO_PROVIDER);

            given(userRepository.existsByUsername(UserTestConstants.USERNAME_GENERATED_BASE)).willReturn(true);
            given(userRepository.existsByUsername(UserTestConstants.USERNAME_GENERATED_UNIQUE)).willReturn(false);

            User savedUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_GENERATED_UNIQUE);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userCommandService.createUser(oAuthInfo);

            // then
            assertThat(result.getUsername()).isEqualTo(UserTestConstants.USERNAME_GENERATED_UNIQUE);

            verify(userRepository).existsByUsername(UserTestConstants.USERNAME_GENERATED_BASE);
            verify(userRepository).existsByUsername(UserTestConstants.USERNAME_GENERATED_UNIQUE);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("짧은 ProviderId도 적절히 처리한다")
        void createUser_WithShortProviderId_HandlesCorrectly() {
            // given
            OAuthInfoResponse oAuthInfo = createOAuthInfoStub(UserTestConstants.PROVIDER_ID_SHORT_STRING, UserTestConstants.KAKAO_PROVIDER);

            given(userRepository.existsByUsername(UserTestConstants.USERNAME_GENERATED_SHORT)).willReturn(false);

            User savedUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_SHORT,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_GENERATED_SHORT);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // when
            User result = userCommandService.createUser(oAuthInfo);

            // then
            assertThat(result.getProviderId()).isEqualTo(UserTestConstants.PROVIDER_ID_SHORT);
            assertThat(result.getUsername()).isEqualTo(UserTestConstants.USERNAME_GENERATED_SHORT);

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
            User existingUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_OLD);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(existingUser);
            given(userRepository.existsByUsername(UserTestConstants.USERNAME_NEW)).willReturn(false);

            // when
            userCommandService.registerUser(UserTestConstants.USER_ID_1, UserTestConstants.USERNAME_NEW,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            // then
            verify(userQueryService).getUserById(UserTestConstants.USER_ID_1);
            verify(userRepository).existsByUsername(UserTestConstants.USERNAME_NEW);
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("중복된 닉네임으로 등록 시 BusinessException을 발생시킨다")
        void registerUser_WithDuplicateNickname_ThrowsBusinessException() {
            // given
            User existingUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_OLD);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(existingUser);
            given(userRepository.existsByUsername(UserTestConstants.USERNAME_DUPLICATE)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userCommandService.registerUser(UserTestConstants.USER_ID_1,
                    UserTestConstants.USERNAME_DUPLICATE, UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.NICKNAME_DUPLICATE);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("현재 사용자명과 동일한 닉네임으로는 등록할 수 있다")
        void registerUser_WithSameCurrentUsername_RegistersSuccessfully() {
            // given
            User existingUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_CURRENT);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(existingUser);
            given(userRepository.existsByUsername(UserTestConstants.USERNAME_CURRENT)).willReturn(true);

            // when
            userCommandService.registerUser(UserTestConstants.USER_ID_1, UserTestConstants.USERNAME_CURRENT,
                    UserTestConstants.IMAGE_URL_NEW, UserTestConstants.INTRODUCTION_NEW);

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
            User existingUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_OLD);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(existingUser);
            given(userRepository.existsByUsername(UserTestConstants.USERNAME_NEW)).willReturn(false);

            // when
            User result = userCommandService.updateProfile(UserTestConstants.USER_ID_1, UserTestConstants.USERNAME_NEW,
                    UserTestConstants.IMAGE_URL_NEW, UserTestConstants.INTRODUCTION_NEW);

            // then
            assertThat(result.getId()).isEqualTo(UserTestConstants.USER_ID_1);
            verify(userQueryService).getUserById(UserTestConstants.USER_ID_1);
            verify(userRepository).existsByUsername(UserTestConstants.USERNAME_NEW);
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("중복된 닉네임으로 업데이트 시 BusinessException을 발생시킨다")
        void updateProfile_WithDuplicateUsername_ThrowsBusinessException() {
            // given
            User existingUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_CURRENT);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(existingUser);
            given(userRepository.existsByUsername(UserTestConstants.USERNAME_EXISTING)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userCommandService.updateProfile(UserTestConstants.USER_ID_1,
                    UserTestConstants.USERNAME_EXISTING, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(UserTestConstants.ERROR_MESSAGE_NICKNAME_DUPLICATE)
                    .extracting("responseStatus")
                    .extracting("httpStatus")
                    .isEqualTo(ResponseStatus.NICKNAME_DUPLICATE.getHttpStatus());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("현재 사용자명과 동일한 경우 중복 검사를 통과한다")
        void updateProfile_WithSameCurrentUsername_UpdatesSuccessfully() {
            // given
            User existingUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_CURRENT);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(existingUser);

            // when
            User result = userCommandService.updateProfile(UserTestConstants.USER_ID_1, UserTestConstants.USERNAME_CURRENT,
                    UserTestConstants.IMAGE_URL_NEW, UserTestConstants.INTRODUCTION_NEW);

            // then
            assertThat(result.getId()).isEqualTo(UserTestConstants.USER_ID_1);
            verify(userRepository, never()).existsByUsername(anyString());
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("null 사용자명으로 업데이트 시 중복 검사를 하지 않는다")
        void updateProfile_WithNullUsername_SkipsDuplicateCheck() {
            // given
            User existingUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_CURRENT);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(existingUser);

            // when
            User result = userCommandService.updateProfile(UserTestConstants.USER_ID_1, null,
                    UserTestConstants.IMAGE_URL_NEW, UserTestConstants.INTRODUCTION_NEW);

            // then
            assertThat(result.getId()).isEqualTo(UserTestConstants.USER_ID_1);
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
            User existingUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(existingUser);

            // when
            userCommandService.agreeToTerms(UserTestConstants.USER_ID_1,
                    UserTestConstants.PRIVACY_AGREED, UserTestConstants.LOCATION_AGREED);

            // then
            verify(userQueryService).getUserById(UserTestConstants.USER_ID_1);
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
            User existingUser = createUserStub(UserTestConstants.USER_ID_1, UserTestConstants.PROVIDER_ID_VALID,
                    UserTestConstants.KAKAO_PROVIDER, UserTestConstants.USERNAME_TEST);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(existingUser);

            // when
            userCommandService.deleteUser(UserTestConstants.USER_ID_1);

            // then
            verify(userQueryService).getUserById(UserTestConstants.USER_ID_1);
            verify(tokenService).invalidateUserTokens(existingUser);
            verify(tokenRepository).deleteAllByUser(existingUser);
            verify(userRepository).delete(existingUser);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 삭제 시 getUserById에서 예외가 발생한다")
        void deleteUser_WithNonExistentId_ThrowsExceptionFromGetUserById() {
            // given
            given(userQueryService.getUserById(UserTestConstants.NON_EXISTENT_USER_ID))
                    .willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> userCommandService.deleteUser(UserTestConstants.NON_EXISTENT_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .extracting("responseStatus")
                    .isEqualTo(ResponseStatus.USER_NOT_FOUND);

            verify(userQueryService).getUserById(UserTestConstants.NON_EXISTENT_USER_ID);
            verify(tokenService, never()).invalidateUserTokens(any(User.class));
            verify(tokenRepository, never()).deleteAllByUser(any(User.class));
            verify(userRepository, never()).delete(any(User.class));
        }
    }

    // === 테스트 헬퍼 메서드들 ===
    private OAuthInfoResponse createOAuthInfoStub(String providerId, String provider) {
        OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
        given(oAuthInfo.getProviderId()).willReturn(providerId);
        given(oAuthInfo.getProvider()).willReturn(provider);
        return oAuthInfo;
    }

    private OAuthInfoResponse createOAuthInfoStubWithProviderIdOnly(String providerId) {
        OAuthInfoResponse oAuthInfo = mock(OAuthInfoResponse.class);
        given(oAuthInfo.getProviderId()).willReturn(providerId);
        return oAuthInfo;
    }

    private User createUserStub(Long id, Long providerId, String provider, String username) {
        return User.builder()
                .id(id)
                .providerId(providerId)
                .provider(provider)
                .username(username)
                .build();
    }
}
