package com.dolpin.domain.user.controller;

import com.dolpin.domain.auth.service.cookie.CookieService;
import com.dolpin.domain.user.dto.request.AgreementRequest;
import com.dolpin.domain.user.dto.request.UserProfileUpdateRequest;
import com.dolpin.domain.user.dto.request.UserRegisterRequest;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserCommandService;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.redis.service.DuplicatePreventionService;
import com.dolpin.global.response.ResponseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

import static com.dolpin.global.constants.UserTestConstants.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
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

    @MockitoBean
    private DuplicatePreventionService duplicatePreventionService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        // Mock 초기화
        reset(userCommandService, userQueryService, cookieService, duplicatePreventionService);

        mockUser = User.builder()
                .id(USER_ID_1)
                .providerId(PROVIDER_ID_VALID)
                .provider(KAKAO_PROVIDER)
                .username(USERNAME_TEST)
                .imageUrl(IMAGE_URL_PROFILE)
                .introduction(INTRODUCTION_HELLO)
                .isPrivacyAgreed(PRIVACY_AGREED)
                .isLocationAgreed(LOCATION_AGREED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("약관 동의 성공")
    void saveAgreement_Success() throws Exception {
        // given
        AgreementRequest request = new AgreementRequest(PRIVACY_AGREED, LOCATION_AGREED);

        // when & then
        mockMvc.perform(post("/api/v1/users/agreement")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE_AGREEMENT_SAVED));

        verify(userCommandService).agreeToTerms(USER_ID_1, PRIVACY_AGREED, LOCATION_AGREED);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("개인정보 동의 누락으로 실패")
    void saveAgreement_Fail_PrivacyAgreementMissing() throws Exception {
        // given
        AgreementRequest request = new AgreementRequest(null, LOCATION_AGREED);

        // when & then
        mockMvc.perform(post("/api/v1/users/agreement")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("개인정보 동의 여부는 필수입니다"));

        verify(userCommandService, never()).agreeToTerms(any(Long.class), any(Boolean.class), any(Boolean.class));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("사용자 등록 성공")
    void registerUser_Success() throws Exception {
        // given
        UserRegisterRequest request = new UserRegisterRequest(USERNAME_TEST, IMAGE_URL_PROFILE, INTRODUCTION_HELLO);
        String lockKey = USER_ID_1 + ":registerUser";

        given(duplicatePreventionService.generateKey(USER_ID_1, "registerUser")).willReturn(lockKey);
        given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(5), any()))
                .willAnswer(invocation -> {
                    DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                    return action.execute();
                });

        // when & then
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE_USER_INFO_SAVED));

        verify(userCommandService).registerUser(USER_ID_1, USERNAME_TEST, IMAGE_URL_PROFILE, INTRODUCTION_HELLO);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("닉네임 중복으로 사용자 등록 실패")
    void registerUser_Fail_DuplicateNickname() throws Exception {
        // given
        UserRegisterRequest request = new UserRegisterRequest(USERNAME_DUPLICATE, IMAGE_URL_PROFILE, INTRODUCTION_HELLO);
        String lockKey = USER_ID_1 + ":registerUser";

        given(duplicatePreventionService.generateKey(USER_ID_1, "registerUser")).willReturn(lockKey);
        given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(5), any()))
                .willAnswer(invocation -> {
                    DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                    return action.execute();
                });

        doThrow(new BusinessException(ResponseStatus.NICKNAME_DUPLICATE))
                .when(userCommandService)
                .registerUser(USER_ID_1, USERNAME_DUPLICATE, IMAGE_URL_PROFILE, INTRODUCTION_HELLO);

        // when & then
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 닉네임입니다."));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("닉네임 길이 검증 실패")
    void registerUser_Fail_NicknameTooShort() throws Exception {
        // given
        UserRegisterRequest request = new UserRegisterRequest(NICKNAME_TOO_SHORT, IMAGE_URL_PROFILE, INTRODUCTION_HELLO);

        // when & then
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userCommandService, never()).registerUser(any(Long.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("내 프로필 조회 성공")
    void getMyProfile_Success() throws Exception {
        // given
        given(userQueryService.getUserById(USER_ID_1)).willReturn(mockUser);

        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE_RETRIEVE_SUCCESS))
                .andExpect(jsonPath("$.data.id").value(USER_ID_1))
                .andExpect(jsonPath("$.data.username").value(USERNAME_TEST));

        verify(userQueryService).getUserById(USER_ID_1);
    }

    @Test
    @WithMockUser(username = "3")
    @DisplayName("다른 사용자 프로필 조회 성공")
    void getUserProfile_Success() throws Exception {
        // given
        given(userQueryService.getUserById(USER_ID_2)).willReturn(mockUser);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}", USER_ID_2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE_RETRIEVE_SUCCESS))
                .andExpect(jsonPath("$.data.username").value(USERNAME_TEST));

        verify(userQueryService).getUserById(USER_ID_2);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("프로필 수정 성공")
    void updateUserProfile_Success() throws Exception {
        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(USERNAME_UPDATE, IMAGE_URL_UPDATE, INTRODUCTION_UPDATE);
        String lockKey = USER_ID_1 + ":updateProfile";

        User updatedUser = User.builder()
                .id(USER_ID_1)
                .username(USERNAME_UPDATE)
                .imageUrl(IMAGE_URL_UPDATE)
                .introduction(INTRODUCTION_UPDATE)
                .build();

        given(duplicatePreventionService.generateKey(USER_ID_1, "updateProfile")).willReturn(lockKey);
        given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                .willAnswer(invocation -> {
                    DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                    return action.execute();
                });
        given(userCommandService.updateProfile(USER_ID_1, USERNAME_UPDATE, IMAGE_URL_UPDATE, INTRODUCTION_UPDATE))
                .willReturn(updatedUser);

        // when & then
        mockMvc.perform(patch("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE_USER_INFO_UPDATED))
                .andExpect(jsonPath("$.data.username").value(USERNAME_UPDATE));

        verify(userCommandService).updateProfile(USER_ID_1, USERNAME_UPDATE, IMAGE_URL_UPDATE, INTRODUCTION_UPDATE);
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("사용자 삭제 성공")
    void deleteUser_Success() throws Exception {
        // given
        String refreshToken = TEST_REFRESH_TOKEN;

        // when & then
        mockMvc.perform(delete("/api/v1/users")
                        .with(csrf())
                        .cookie(new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(SUCCESS_MESSAGE_USER_DELETE));

        verify(userCommandService).deleteUser(USER_ID_1);
        verify(cookieService).deleteAccessTokenCookie(any(HttpServletResponse.class));
        verify(cookieService).deleteRefreshTokenCookie(any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("인증 없이 내 프로필 조회 실패")
    void getMyProfile_Fail_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        verify(userQueryService, never()).getUserById(any(Long.class));
    }

    @Test
    @DisplayName("CSRF 토큰 없이 POST 요청 실패")
    void registerUser_Fail_NoCsrfToken() throws Exception {
        // given
        UserRegisterRequest request = new UserRegisterRequest(USERNAME_TEST, IMAGE_URL_PROFILE, INTRODUCTION_HELLO);

        // when & then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(userCommandService, never()).registerUser(any(Long.class), any(String.class), any(String.class), any(String.class));
    }
}
