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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
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
            AgreementRequest request = new AgreementRequest(true, true);
            Long userId = 1L;

            willDoNothing().given(userCommandService).agreeToTerms(userId, true, true);

            // when & then
            MvcResult result = mockMvc.perform(post("/api/v1/users/agreement")
                            .with(user("1"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Void> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Void>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("agreement_saved");
            assertThat(apiResponse.getData()).isNull();

            verify(userCommandService).agreeToTerms(userId, true, true);
        }

        @Test
        @DisplayName("필수 동의 항목이 null인 경우 400 에러가 발생한다")
        void saveAgreement_WithNullPrivacyAgreed_Returns400Error() throws Exception {
            // given
            AgreementRequest request = new AgreementRequest(null, true);

            // when & then
            MvcResult result = mockMvc.perform(post("/api/v1/users/agreement")
                            .with(user("1"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).contains("개인정보 동의 여부는 필수입니다");

            // 유효성 검증 실패로 서비스 메서드가 호출되지 않음을 확인
            verifyNoInteractions(userCommandService);
        }

        @Test
        @DisplayName("존재하지 않는 사용자에 대해 404 에러가 발생한다")
        void saveAgreement_WithNonExistentUser_Returns404Error() throws Exception {
            // given
            AgreementRequest request = new AgreementRequest(true, true);
            Long userId = 1L;

            willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND))
                    .given(userCommandService).agreeToTerms(userId, true, true);

            // when & then
            MvcResult result = mockMvc.perform(post("/api/v1/users/agreement")
                            .with(user("1"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
            assertThat(apiResponse.getData()).isNull();
        }
    }

    @Nested
    @DisplayName("사용자 등록 API 테스트")
    class RegisterUserTest {

        @Test
        @DisplayName("정상적인 사용자 등록이 성공한다")
        void registerUser_WithValidRequest_ReturnsSuccessResponse() throws Exception {
            // given
            UserRegisterRequest request = new UserRegisterRequest("테스트닉네임", "profile.jpg", "안녕하세요!");
            Long userId = 1L;

            willDoNothing().given(userCommandService).registerUser(
                    userId, "테스트닉네임", "profile.jpg", "안녕하세요!");

            // when & then
            MvcResult result = mockMvc.perform(post("/api/v1/users")
                            .with(user("1"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Void> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Void>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("user_info_saved");
            assertThat(apiResponse.getData()).isNull();

            verify(userCommandService).registerUser(userId, "테스트닉네임", "profile.jpg", "안녕하세요!");
        }

        @Test
        @DisplayName("닉네임이 너무 짧은 경우 400 에러가 발생한다")
        void registerUser_WithTooShortNickname_Returns400Error() throws Exception {
            // given
            UserRegisterRequest request = new UserRegisterRequest("a", "profile.jpg", "안녕하세요!");

            // when & then
            mockMvc.perform(post("/api/v1/users")
                            .with(user("1"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userCommandService, never()).registerUser(any(), any(), any(), any());
        }

        @Test
        @DisplayName("중복된 닉네임에 대해 409 에러가 발생한다")
        void registerUser_WithDuplicateNickname_Returns409Error() throws Exception {
            // given
            UserRegisterRequest request = new UserRegisterRequest("중복닉네임", "profile.jpg", "안녕하세요!");
            Long userId = 1L;

            willThrow(new BusinessException(ResponseStatus.NICKNAME_DUPLICATE))
                    .given(userCommandService).registerUser(userId, "중복닉네임", "profile.jpg", "안녕하세요!");

            // when & then
            MvcResult result = mockMvc.perform(post("/api/v1/users")
                            .with(user("1"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("이미 존재하는 닉네임입니다.");
        }
    }

    @Nested
    @DisplayName("내 프로필 조회 API 테스트")
    class GetMyProfileTest {

        @Test
        @DisplayName("정상적인 내 프로필 조회가 성공한다")
        void getMyProfile_WithValidUser_ReturnsSuccessResponse() throws Exception {
            // given
            Long userId = 1L;
            User mockUser = createMockUser(userId, "테스트사용자", "profile.jpg", "안녕하세요!");
            MyProfileResponse mockResponse = MyProfileResponse.from(mockUser);

            given(userQueryService.getUserById(userId)).willReturn(mockUser);

            // when & then
            MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                            .with(user("1"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<MyProfileResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<MyProfileResponse>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("retrieve_user_info_success");
            assertThat(apiResponse.getData().getId()).isEqualTo(userId);
            assertThat(apiResponse.getData().getUsername()).isEqualTo("테스트사용자");
            assertThat(apiResponse.getData().getProfileImage()).isEqualTo("profile.jpg");

            verify(userQueryService).getUserById(userId);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 404 에러가 발생한다")
        void getMyProfile_WithNonExistentUser_Returns404Error() throws Exception {
            // given
            Long nonExistentUserId = 999L;
            given(userQueryService.getUserById(nonExistentUserId))
                    .willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND));

            // when & then
            MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                            .with(user("999"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("사용자 프로필 조회 API 테스트")
    class GetUserProfileTest {

        @Test
        @DisplayName("정상적인 사용자 프로필 조회가 성공한다")
        void getUserProfile_WithValidUserId_ReturnsSuccessResponse() throws Exception {
            // given
            Long userId = 1L;
            User mockUser = createMockUser(userId, "다른사용자", "other.jpg", "다른사용자입니다");
            UserProfileResponse mockResponse = UserProfileResponse.from(mockUser);

            given(userQueryService.getUserById(userId)).willReturn(mockUser);

            // when & then
            MvcResult result = mockMvc.perform(get("/api/v1/users/{userId}", userId)
                            .with(user("1"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<UserProfileResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<UserProfileResponse>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("retrieve_user_info_success");
            assertThat(apiResponse.getData().getId()).isEqualTo(userId);
            assertThat(apiResponse.getData().getUsername()).isEqualTo("다른사용자");
            assertThat(apiResponse.getData().getProfileImage()).isEqualTo("other.jpg");

            verify(userQueryService).getUserById(userId);
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
            Long userId = 1L;
            UserProfileUpdateRequest request = new UserProfileUpdateRequest("업데이트닉네임", "new_profile.jpg", "업데이트된 소개");
            User updatedUser = createMockUser(userId, "업데이트닉네임", "new_profile.jpg", "업데이트된 소개");

            given(userCommandService.updateProfile(userId, "업데이트닉네임", "new_profile.jpg", "업데이트된 소개"))
                    .willReturn(updatedUser);

            // when & then
            MvcResult result = mockMvc.perform(patch("/api/v1/users")
                            .with(user("1"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<UserProfileResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<UserProfileResponse>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("user_info_updated");
            assertThat(apiResponse.getData().getUsername()).isEqualTo("업데이트닉네임");
            assertThat(apiResponse.getData().getProfileImage()).isEqualTo("new_profile.jpg");

            verify(userCommandService).updateProfile(userId, "업데이트닉네임", "new_profile.jpg", "업데이트된 소개");
        }

        @Test
        @DisplayName("중복된 닉네임으로 업데이트 시 409 에러가 발생한다")
        void updateUserProfile_WithDuplicateNickname_Returns409Error() throws Exception {
            // given
            Long userId = 1L;
            UserProfileUpdateRequest request = new UserProfileUpdateRequest("중복닉네임", "profile.jpg", "소개");

            given(userCommandService.updateProfile(userId, "중복닉네임", "profile.jpg", "소개"))
                    .willThrow(new BusinessException(ResponseStatus.NICKNAME_DUPLICATE));

            // when & then
            MvcResult result = mockMvc.perform(patch("/api/v1/users")
                            .with(user("1"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("이미 존재하는 닉네임입니다.");
        }
    }

    @Nested
    @DisplayName("사용자 삭제 API 테스트")
    class DeleteUserTest {

        @Test
        @DisplayName("정상적인 사용자 삭제가 성공한다")
        void deleteUser_WithValidUser_ReturnsSuccessResponse() throws Exception {
            // given
            Long userId = 1L;

            willDoNothing().given(userCommandService).deleteUser(userId);
            willDoNothing().given(cookieService).deleteAccessTokenCookie(any());
            willDoNothing().given(cookieService).deleteRefreshTokenCookie(any());

            // when & then
            MvcResult result = mockMvc.perform(delete("/api/v1/users")
                            .with(user("1"))
                            .with(csrf())
                            .cookie(new jakarta.servlet.http.Cookie("refresh_token", "test_refresh_token"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Void> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Void>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("user_delete_success");
            assertThat(apiResponse.getData()).isNull();

            verify(userCommandService).deleteUser(userId);
            verify(cookieService).deleteAccessTokenCookie(any());
            verify(cookieService).deleteRefreshTokenCookie(any());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 삭제 시 404 에러가 발생한다")
        void deleteUser_WithNonExistentUser_Returns404Error() throws Exception {
            // given
            Long nonExistentUserId = 999L;

            willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND))
                    .given(userCommandService).deleteUser(nonExistentUserId);

            // when & then
            MvcResult result = mockMvc.perform(delete("/api/v1/users")
                            .with(user("999"))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("공통 응답 구조 검증")
    class CommonResponseTest {

        @Test
        @DisplayName("모든 성공 응답은 올바른 ApiResponse 구조를 가진다")
        void allSuccessResponses_HaveCorrectApiResponseStructure() throws Exception {
            // given
            Long userId = 1L;
            User mockUser = createMockUser(userId, "테스트", "test.jpg", "테스트");
            given(userQueryService.getUserById(userId)).willReturn(mockUser);

            // when & then
            MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                            .with(user("1"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<MyProfileResponse> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<MyProfileResponse>>() {});

            // ApiResponse 구조 검증
            assertThat(apiResponse.getMessage()).isNotNull();
            assertThat(apiResponse.getData()).isNotNull();
        }

        @Test
        @DisplayName("서비스 예외 발생 시 적절한 에러 응답을 반환한다")
        void serviceException_ReturnsProperErrorResponse() throws Exception {
            // given
            Long userId = 1L;
            given(userQueryService.getUserById(userId))
                    .willThrow(new BusinessException(ResponseStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류"));

            // when & then
            MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                            .with(user("1"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(
                    responseBody, new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getMessage()).isEqualTo("서버 내부 오류");
            assertThat(apiResponse.getData()).isNull();
        }
    }

    // 테스트용 Mock User 생성 헬퍼 메서드
    private User createMockUser(Long id, String username, String imageUrl, String introduction) {
        return User.builder()
                .id(id)
                .providerId(12345L)
                .provider("kakao")
                .username(username)
                .imageUrl(imageUrl)
                .introduction(introduction)
                .isPrivacyAgreed(true)
                .isLocationAgreed(true)
                .privacyAgreedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
