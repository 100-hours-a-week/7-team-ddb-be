package com.dolpin.domain.user.controller;

import com.dolpin.domain.auth.service.cookie.CookieService;
import com.dolpin.domain.user.dto.request.AgreementRequest;
import com.dolpin.domain.user.dto.request.UserProfileUpdateRequest;
import com.dolpin.domain.user.dto.request.UserRegisterRequest;
import com.dolpin.domain.user.dto.response.MyProfileResponse;
import com.dolpin.domain.user.dto.response.UserProfileResponse;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserCommandService;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.constants.UserTestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ApiResponse;
import com.dolpin.global.response.ResponseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@DisplayName("UserController 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserCommandService userCommandService;

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private CookieService cookieService;

    @Nested
    @DisplayName("약관 동의 API 테스트")
    class SaveAgreementTest {

        @Test
        @DisplayName("정상적인 약관 동의가 성공한다")
        void saveAgreement_WithValidRequest_ReturnsSuccessResponse() throws Exception {
            // given
            AgreementRequest request = new AgreementRequest(UserTestConstants.PRIVACY_AGREED, UserTestConstants.LOCATION_AGREED);

            willDoNothing().given(userCommandService).agreeToTerms(UserTestConstants.USER_ID_1,
                    UserTestConstants.PRIVACY_AGREED, UserTestConstants.LOCATION_AGREED);

            // when & then
            MvcResult result = performPostRequest("/api/v1/users/agreement", request)
                    .andExpect(status().isCreated())
                    .andReturn();

            ApiResponse<Void> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Void>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.SUCCESS_MESSAGE_AGREEMENT_SAVED);
            assertThat(apiResponse.getData()).isNull();

            verify(userCommandService).agreeToTerms(UserTestConstants.USER_ID_1,
                    UserTestConstants.PRIVACY_AGREED, UserTestConstants.LOCATION_AGREED);
        }

        @Test
        @DisplayName("필수 동의 항목이 null인 경우 400 에러가 발생한다")
        void saveAgreement_WithNullPrivacyAgreed_Returns400Error() throws Exception {
            // given
            AgreementRequest request = new AgreementRequest(null, UserTestConstants.LOCATION_AGREED);

            // when & then
            MvcResult result = performPostRequest("/api/v1/users/agreement", request)
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ApiResponse<Object> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Object>>() {});
            assertThat(apiResponse.getMessage()).contains(UserTestConstants.ERROR_MESSAGE_PRIVACY_AGREEMENT_REQUIRED);

            verifyNoInteractions(userCommandService);
        }

        @Test
        @DisplayName("존재하지 않는 사용자에 대해 404 에러가 발생한다")
        void saveAgreement_WithNonExistentUser_Returns404Error() throws Exception {
            // given
            AgreementRequest request = new AgreementRequest(UserTestConstants.PRIVACY_AGREED, UserTestConstants.LOCATION_AGREED);

            willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND))
                    .given(userCommandService).agreeToTerms(UserTestConstants.USER_ID_1,
                            UserTestConstants.PRIVACY_AGREED, UserTestConstants.LOCATION_AGREED);

            // when & then
            MvcResult result = performPostRequest("/api/v1/users/agreement", request)
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiResponse<Object> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Object>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.ERROR_MESSAGE_USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("사용자 등록 API 테스트")
    class RegisterUserTest {

        @Test
        @DisplayName("정상적인 사용자 등록이 성공한다")
        void registerUser_WithValidRequest_ReturnsSuccessResponse() throws Exception {
            // given
            UserRegisterRequest request = new UserRegisterRequest(UserTestConstants.NICKNAME_MIN_LENGTH,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            willDoNothing().given(userCommandService).registerUser(UserTestConstants.USER_ID_1,
                    UserTestConstants.NICKNAME_MIN_LENGTH, UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            // when & then
            MvcResult result = performPostRequest("/api/v1/users", request)
                    .andExpect(status().isCreated())
                    .andReturn();

            ApiResponse<Void> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Void>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.SUCCESS_MESSAGE_USER_INFO_SAVED);

            verify(userCommandService).registerUser(UserTestConstants.USER_ID_1,
                    UserTestConstants.NICKNAME_MIN_LENGTH, UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);
        }

        @Test
        @DisplayName("닉네임이 너무 짧은 경우 400 에러가 발생한다")
        void registerUser_WithTooShortNickname_Returns400Error() throws Exception {
            // given
            UserRegisterRequest request = new UserRegisterRequest(UserTestConstants.NICKNAME_TOO_SHORT,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            // when & then
            performPostRequest("/api/v1/users", request)
                    .andExpect(status().isBadRequest());

            verify(userCommandService, never()).registerUser(any(), any(), any(), any());
        }

        @Test
        @DisplayName("중복된 닉네임에 대해 409 에러가 발생한다")
        void registerUser_WithDuplicateNickname_Returns409Error() throws Exception {
            // given
            UserRegisterRequest request = new UserRegisterRequest(UserTestConstants.USERNAME_DUPLICATE,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            willThrow(new BusinessException(ResponseStatus.NICKNAME_DUPLICATE))
                    .given(userCommandService).registerUser(UserTestConstants.USER_ID_1,
                            UserTestConstants.USERNAME_DUPLICATE, UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            // when & then
            MvcResult result = performPostRequest("/api/v1/users", request)
                    .andExpect(status().isConflict())
                    .andReturn();

            ApiResponse<Object> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Object>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.ERROR_MESSAGE_NICKNAME_DUPLICATE);
        }
    }

    @Nested
    @DisplayName("내 프로필 조회 API 테스트")
    class GetMyProfileTest {

        @Test
        @DisplayName("정상적인 내 프로필 조회가 성공한다")
        void getMyProfile_WithValidUser_ReturnsSuccessResponse() throws Exception {
            // given
            User mockUser = createMockUser(UserTestConstants.USER_ID_1, UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(mockUser);

            // when & then
            MvcResult result = performGetRequest("/api/v1/users/me")
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<MyProfileResponse> apiResponse = parseResponse(result, new TypeReference<ApiResponse<MyProfileResponse>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.SUCCESS_MESSAGE_RETRIEVE_SUCCESS);
            assertThat(apiResponse.getData().getId()).isEqualTo(UserTestConstants.USER_ID_1);
            assertThat(apiResponse.getData().getUsername()).isEqualTo(UserTestConstants.USERNAME_TEST);
            assertThat(apiResponse.getData().getProfileImage()).isEqualTo(UserTestConstants.IMAGE_URL_PROFILE);

            verify(userQueryService).getUserById(UserTestConstants.USER_ID_1);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 404 에러가 발생한다")
        void getMyProfile_WithNonExistentUser_Returns404Error() throws Exception {
            // given
            given(userQueryService.getUserById(UserTestConstants.NON_EXISTENT_USER_ID))
                    .willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND));

            // when & then
            MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                            .with(user(UserTestConstants.NON_EXISTENT_USER_ID.toString()))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiResponse<Object> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Object>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.ERROR_MESSAGE_USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("사용자 프로필 조회 API 테스트")
    class GetUserProfileTest {

        @Test
        @DisplayName("정상적인 사용자 프로필 조회가 성공한다")
        void getUserProfile_WithValidUserId_ReturnsSuccessResponse() throws Exception {
            // given
            User mockUser = createMockUser(UserTestConstants.USER_ID_1, UserTestConstants.USERNAME_OTHER,
                    UserTestConstants.IMAGE_URL_OTHER, UserTestConstants.INTRODUCTION_OTHER);

            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(mockUser);

            // when & then
            MvcResult result = mockMvc.perform(get("/api/v1/users/{userId}", UserTestConstants.USER_ID_1)
                            .with(user("1"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<UserProfileResponse> apiResponse = parseResponse(result, new TypeReference<ApiResponse<UserProfileResponse>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.SUCCESS_MESSAGE_RETRIEVE_SUCCESS);
            assertThat(apiResponse.getData().getId()).isEqualTo(UserTestConstants.USER_ID_1);
            assertThat(apiResponse.getData().getUsername()).isEqualTo(UserTestConstants.USERNAME_OTHER);
            assertThat(apiResponse.getData().getProfileImage()).isEqualTo(UserTestConstants.IMAGE_URL_OTHER);

            verify(userQueryService).getUserById(UserTestConstants.USER_ID_1);
        }

        @Test
        @DisplayName("잘못된 형식의 userId에 대해 400 에러가 발생한다")
        void getUserProfile_WithInvalidUserId_Returns400Error() throws Exception {
            // given
            String invalidUserId = "invalid";

            // when & then
            mockMvc.perform(get("/api/v1/users/{userId}", invalidUserId)
                            .with(user("1"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("프로필 업데이트 API 테스트")
    class UpdateUserProfileTest {

        @Test
        @DisplayName("정상적인 프로필 업데이트가 성공한다")
        void updateUserProfile_WithValidRequest_ReturnsSuccessResponse() throws Exception {
            // given
            UserProfileUpdateRequest request = new UserProfileUpdateRequest(UserTestConstants.USERNAME_UPDATE,
                    UserTestConstants.IMAGE_URL_UPDATE, UserTestConstants.INTRODUCTION_UPDATE);
            User updatedUser = createMockUser(UserTestConstants.USER_ID_1, UserTestConstants.USERNAME_UPDATE,
                    UserTestConstants.IMAGE_URL_UPDATE, UserTestConstants.INTRODUCTION_UPDATE);

            given(userCommandService.updateProfile(UserTestConstants.USER_ID_1,
                    UserTestConstants.USERNAME_UPDATE, UserTestConstants.IMAGE_URL_UPDATE, UserTestConstants.INTRODUCTION_UPDATE))
                    .willReturn(updatedUser);

            // when & then
            MvcResult result = performPatchRequest("/api/v1/users", request)
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<UserProfileResponse> apiResponse = parseResponse(result, new TypeReference<ApiResponse<UserProfileResponse>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.SUCCESS_MESSAGE_USER_INFO_UPDATED);
            assertThat(apiResponse.getData().getUsername()).isEqualTo(UserTestConstants.USERNAME_UPDATE);
            assertThat(apiResponse.getData().getProfileImage()).isEqualTo(UserTestConstants.IMAGE_URL_UPDATE);

            verify(userCommandService).updateProfile(UserTestConstants.USER_ID_1,
                    UserTestConstants.USERNAME_UPDATE, UserTestConstants.IMAGE_URL_UPDATE, UserTestConstants.INTRODUCTION_UPDATE);
        }

        @Test
        @DisplayName("중복된 닉네임으로 업데이트 시 409 에러가 발생한다")
        void updateUserProfile_WithDuplicateNickname_Returns409Error() throws Exception {
            // given
            UserProfileUpdateRequest request = new UserProfileUpdateRequest(UserTestConstants.USERNAME_DUPLICATE,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);

            given(userCommandService.updateProfile(UserTestConstants.USER_ID_1,
                    UserTestConstants.USERNAME_DUPLICATE, UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO))
                    .willThrow(new BusinessException(ResponseStatus.NICKNAME_DUPLICATE));

            // when & then
            MvcResult result = performPatchRequest("/api/v1/users", request)
                    .andExpect(status().isConflict())
                    .andReturn();

            ApiResponse<Object> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Object>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.ERROR_MESSAGE_NICKNAME_DUPLICATE);
        }
    }

    @Nested
    @DisplayName("사용자 삭제 API 테스트")
    class DeleteUserTest {

        @Test
        @DisplayName("정상적인 사용자 삭제가 성공한다")
        void deleteUser_WithValidUser_ReturnsSuccessResponse() throws Exception {
            // given
            willDoNothing().given(userCommandService).deleteUser(UserTestConstants.USER_ID_1);
            willDoNothing().given(cookieService).deleteAccessTokenCookie(any());
            willDoNothing().given(cookieService).deleteRefreshTokenCookie(any());

            // when & then
            MvcResult result = mockMvc.perform(delete("/api/v1/users")
                            .with(user("1"))
                            .with(csrf())
                            .cookie(new jakarta.servlet.http.Cookie(UserTestConstants.REFRESH_TOKEN_COOKIE_NAME,
                                    UserTestConstants.TEST_REFRESH_TOKEN))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<Void> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Void>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.SUCCESS_MESSAGE_USER_DELETE);
            assertThat(apiResponse.getData()).isNull();

            verify(userCommandService).deleteUser(UserTestConstants.USER_ID_1);
            verify(cookieService).deleteAccessTokenCookie(any());
            verify(cookieService).deleteRefreshTokenCookie(any());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 삭제 시 404 에러가 발생한다")
        void deleteUser_WithNonExistentUser_Returns404Error() throws Exception {
            // given
            willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND))
                    .given(userCommandService).deleteUser(UserTestConstants.NON_EXISTENT_USER_ID);

            // when & then
            MvcResult result = mockMvc.perform(delete("/api/v1/users")
                            .with(user(UserTestConstants.NON_EXISTENT_USER_ID.toString()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ApiResponse<Object> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Object>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.ERROR_MESSAGE_USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("공통 응답 구조 검증")
    class CommonResponseTest {

        @Test
        @DisplayName("모든 성공 응답은 올바른 ApiResponse 구조를 가진다")
        void allSuccessResponses_HaveCorrectApiResponseStructure() throws Exception {
            // given
            User mockUser = createMockUser(UserTestConstants.USER_ID_1, UserTestConstants.USERNAME_TEST,
                    UserTestConstants.IMAGE_URL_PROFILE, UserTestConstants.INTRODUCTION_HELLO);
            given(userQueryService.getUserById(UserTestConstants.USER_ID_1)).willReturn(mockUser);

            // when & then
            MvcResult result = performGetRequest("/api/v1/users/me")
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<MyProfileResponse> apiResponse = parseResponse(result, new TypeReference<ApiResponse<MyProfileResponse>>() {});
            assertThat(apiResponse.getMessage()).isNotNull();
            assertThat(apiResponse.getData()).isNotNull();
        }

        @Test
        @DisplayName("서비스 예외 발생 시 적절한 에러 응답을 반환한다")
        void serviceException_ReturnsProperErrorResponse() throws Exception {
            // given
            given(userQueryService.getUserById(UserTestConstants.USER_ID_1))
                    .willThrow(new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, UserTestConstants.ERROR_MESSAGE_SERVER_ERROR));

            // when & then
            MvcResult result = performGetRequest("/api/v1/users/me")
                    .andExpect(status().isInternalServerError())
                    .andReturn();

            ApiResponse<Object> apiResponse = parseResponse(result, new TypeReference<ApiResponse<Object>>() {});
            assertThat(apiResponse.getMessage()).isEqualTo(UserTestConstants.ERROR_MESSAGE_SERVER_ERROR);
            assertThat(apiResponse.getData()).isNull();
        }
    }

    // === 테스트 헬퍼 메서드들 (반복 코드 제거) ===
    private User createMockUser(Long id, String username, String imageUrl, String introduction) {
        return User.builder()
                .id(id)
                .providerId(UserTestConstants.PROVIDER_ID_VALID)
                .provider(UserTestConstants.KAKAO_PROVIDER)
                .username(username)
                .imageUrl(imageUrl)
                .introduction(introduction)
                .isPrivacyAgreed(UserTestConstants.PRIVACY_AGREED)
                .isLocationAgreed(UserTestConstants.LOCATION_AGREED)
                .privacyAgreedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions performPostRequest(String url, Object request) throws Exception {
        return mockMvc.perform(post(url)
                .with(user("1"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions performGetRequest(String url) throws Exception {
        return mockMvc.perform(get(url)
                .with(user("1"))
                .contentType(MediaType.APPLICATION_JSON));
    }

    private org.springframework.test.web.servlet.ResultActions performPatchRequest(String url, Object request) throws Exception {
        return mockMvc.perform(patch(url)
                .with(user("1"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private <T> T parseResponse(MvcResult result, TypeReference<T> typeReference) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, typeReference);
    }
}
