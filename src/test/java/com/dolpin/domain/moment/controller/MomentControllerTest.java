package com.dolpin.domain.moment.controller;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.*;
import com.dolpin.global.fixture.MomentTestFixture;
import com.dolpin.domain.moment.service.command.MomentCommandService;
import com.dolpin.domain.moment.service.query.MomentQueryService;
import com.dolpin.global.constants.MomentTestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.redis.service.DuplicatePreventionService;
import com.dolpin.global.response.ResponseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(MomentController.class)
@DisplayName("MomentController 테스트")
class MomentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MomentQueryService momentQueryService;

    @MockitoBean
    private MomentCommandService momentCommandService;

    @MockitoBean
    private DuplicatePreventionService duplicatePreventionService;

    private MomentTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new MomentTestFixture();
    }

    @Nested
    @DisplayName("전체 기록 목록 조회")
    class GetAllMomentsTest {

        @Test
        @DisplayName("성공 - 인증된 사용자")
        @WithMockUser(username = "1")
        void getAllMoments_Success_AuthenticatedUser() throws Exception {
            // given
            MomentListResponse response = fixture.createMomentListResponse();
            given(momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    null
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(MomentTestConstants.MOMENTS_BASE_PATH)
                            .param("limit", String.valueOf(MomentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_LIST_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.moments").isArray())
                    .andExpect(jsonPath("$.data.moments").value(hasSize(2)));
        }

        @Test
        @DisplayName("성공 - 커서 페이지네이션")
        @WithMockUser(username = "1")
        void getAllMoments_Success_WithCursor() throws Exception {
            // given
            MomentListResponse response = fixture.createPaginatedMomentListResponse(5, true);
            given(momentQueryService.getAllMoments(
                    MomentTestConstants.TEST_USER_ID,
                    5,
                    MomentTestConstants.TEST_CURSOR
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(MomentTestConstants.MOMENTS_BASE_PATH)
                            .param("limit", "5")
                            .param("cursor", MomentTestConstants.TEST_CURSOR))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.meta.pagination.limit").value(5))
                    .andExpect(jsonPath("$.data.meta.pagination.has_next").value(true));
        }

        @Test
        @DisplayName("성공 - 빈 목록 조회")
        @WithMockUser(username = "1")
        void getAllMoments_Success_EmptyList() throws Exception {
            // given
            MomentListResponse response = fixture.createEmptyMomentListResponse();
            given(momentQueryService.getAllMoments(any(), any(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get(MomentTestConstants.MOMENTS_BASE_PATH))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.moments").isEmpty());
        }
    }

    @Nested
    @DisplayName("내 기록 목록 조회")
    class GetMyMomentsTest {

        @Test
        @DisplayName("성공 - 내 기록 조회")
        @WithMockUser(username = "1")
        void getMyMoments_Success() throws Exception {
            // given
            MomentListResponse response = fixture.createMyMomentListResponse();
            given(momentQueryService.getMyMoments(
                    MomentTestConstants.TEST_USER_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    null
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(MomentTestConstants.MY_MOMENTS_PATH)
                            .param("limit", String.valueOf(MomentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.USER_MOMENT_LIST_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.moments").isArray());
        }

        @Test
        @DisplayName("실패 - 인증되지 않은 사용자")
        void getMyMoments_Fail_Unauthenticated() throws Exception {
            // when & then
            mockMvc.perform(get(MomentTestConstants.MY_MOMENTS_PATH))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("다른 사용자 기록 목록 조회")
    class GetUserMomentsTest {

        @Test
        @WithMockUser
        @DisplayName("성공 - 다른 사용자 기록 조회")
        void getUserMoments_Success() throws Exception {
            // given
            MomentListResponse response = fixture.createUserMomentListResponse();
            given(momentQueryService.getUserMoments(
                    MomentTestConstants.OTHER_USER_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    null
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(MomentTestConstants.USER_MOMENTS_PATH, MomentTestConstants.OTHER_USER_ID)
                            .param("limit", String.valueOf(MomentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.USER_MOMENT_LIST_SUCCESS_MESSAGE));
        }

        @Test
        @WithMockUser
        @DisplayName("실패 - 존재하지 않는 사용자")
        void getUserMoments_Fail_UserNotFound() throws Exception {
            // given
            given(momentQueryService.getUserMoments(any(), any(), any()))
                    .willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(get(MomentTestConstants.USER_MOMENTS_PATH, 999L))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("장소별 기록 목록 조회")
    class GetPlaceMomentsTest {

        @Test
        @WithMockUser
        @DisplayName("성공 - 장소별 기록 조회")
        void getPlaceMoments_Success() throws Exception {
            // given
            MomentListResponse response = fixture.createPlaceMomentListResponse();
            given(momentQueryService.getPlaceMoments(
                    MomentTestConstants.TEST_PLACE_ID,
                    MomentTestConstants.DEFAULT_PAGE_LIMIT,
                    null
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(MomentTestConstants.PLACE_MOMENTS_PATH, MomentTestConstants.TEST_PLACE_ID)
                            .param("limit", String.valueOf(MomentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.PLACE_MOMENT_LIST_SUCCESS_MESSAGE));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 장소")
        @WithMockUser
        void getPlaceMoments_Fail_PlaceNotFound() throws Exception {
            // given
            given(momentQueryService.getPlaceMoments(any(), any(), any()))
                    .willThrow(new BusinessException(ResponseStatus.PLACE_NOT_FOUND));

            // when & then
            mockMvc.perform(get(MomentTestConstants.PLACE_MOMENTS_PATH, 999L))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("기록 상세 조회")
    class GetMomentDetailTest {

        @Test
        @DisplayName("성공 - 기록 상세 조회 (소유자)")
        @WithMockUser(username = "1")
        void getMomentDetail_Success_Owner() throws Exception {
            // given
            MomentDetailResponse response = fixture.createMomentDetailResponse();
            given(momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID,
                    MomentTestConstants.TEST_USER_ID
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(MomentTestConstants.MOMENT_DETAIL_PATH, MomentTestConstants.TEST_MOMENT_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_DETAIL_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.id").value(MomentTestConstants.TEST_MOMENT_ID))
                    .andExpect(jsonPath("$.data.title").value(MomentTestConstants.TEST_MOMENT_TITLE))
                    .andExpect(jsonPath("$.data.is_owner").value(true));
        }

        @Test
        @DisplayName("성공 - 기록 상세 조회 (비소유자)")
        @WithMockUser(username = "999")
        void getMomentDetail_Success_NonOwner() throws Exception {
            // given
            MomentDetailResponse response = fixture.createOtherUserMomentDetailResponse();
            given(momentQueryService.getMomentDetail(
                    MomentTestConstants.TEST_MOMENT_ID,
                    MomentTestConstants.OTHER_USER_ID
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(MomentTestConstants.MOMENT_DETAIL_PATH, MomentTestConstants.TEST_MOMENT_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.is_owner").value(false));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 기록")
        @WithMockUser(username = "1")
        void getMomentDetail_Fail_MomentNotFound() throws Exception {
            // given
            given(momentQueryService.getMomentDetail(anyLong(), anyLong()))
                    .willThrow(new BusinessException(ResponseStatus.MOMENT_NOT_FOUND));

            // when & then
            mockMvc.perform(get(MomentTestConstants.MOMENT_DETAIL_PATH, 999L))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패 - 접근 권한 없음")
        @WithMockUser(username = "999")
        void getMomentDetail_Fail_AccessDenied() throws Exception {
            // given
            given(momentQueryService.getMomentDetail(anyLong(), anyLong()))
                    .willThrow(new BusinessException(ResponseStatus.FORBIDDEN));

            // when & then
            mockMvc.perform(get(MomentTestConstants.MOMENT_DETAIL_PATH, MomentTestConstants.TEST_MOMENT_ID))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("기록 생성")
    class CreateMomentTest {

        @Test
        @DisplayName("성공 - 기록 생성")
        @WithMockUser(username = "1")
        void createMoment_Success() throws Exception {
            // given
            MomentCreateRequest request = fixture.createMomentCreateRequest();
            MomentCreateResponse response = fixture.createMomentCreateResponse();
            String lockKey = fixture.createMomentCreateLockKey();

            // DuplicatePreventionService 모킹
            given(duplicatePreventionService.generateKey(MomentTestConstants.TEST_USER_ID, "createMoment"))
                    .willReturn(lockKey);

            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(5), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });

            given(momentCommandService.createMoment(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_CREATED_MESSAGE))
                    .andExpect(jsonPath("$.data.id").value(MomentTestConstants.TEST_MOMENT_ID));
        }

        @Test
        @DisplayName("성공 - 장소 정보 없이 생성")
        @WithMockUser(username = "1")
        void createMoment_Success_WithoutPlace() throws Exception {
            // given
            MomentCreateRequest request = fixture.createMomentCreateRequestWithoutPlace();
            MomentCreateResponse response = fixture.createMomentCreateResponse();
            String lockKey = fixture.createMomentCreateLockKey();

            given(duplicatePreventionService.generateKey(MomentTestConstants.TEST_USER_ID, "createMoment"))
                    .willReturn(lockKey);

            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(5), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });

            given(momentCommandService.createMoment(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("성공 - 비공개 기록 생성")
        @WithMockUser(username = "1")
        void createMoment_Success_Private() throws Exception {
            // given
            MomentCreateRequest request = fixture.createPrivateMomentCreateRequest();
            MomentCreateResponse response = fixture.createMomentCreateResponse();
            String lockKey = fixture.createMomentCreateLockKey();

            given(duplicatePreventionService.generateKey(any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(5), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });
            given(momentCommandService.createMoment(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("실패 - 제목 누락")
        @WithMockUser(username = "1")
        void createMoment_Fail_EmptyTitle() throws Exception {
            // given
            MomentCreateRequest request = fixture.createInvalidTitleMomentCreateRequest();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 내용 누락")
        @WithMockUser(username = "1")
        void createMoment_Fail_EmptyContent() throws Exception {
            // given
            MomentCreateRequest request = fixture.createInvalidContentMomentCreateRequest();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 제목 길이 초과")
        @WithMockUser(username = "1")
        void createMoment_Fail_TitleTooLong() throws Exception {
            // given
            MomentCreateRequest request = fixture.createLongTitleMomentCreateRequest();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 내용 길이 초과")
        @WithMockUser(username = "1")
        void createMoment_Fail_ContentTooLong() throws Exception {
            // given
            MomentCreateRequest request = fixture.createLongContentMomentCreateRequest();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 중복 요청")
        @WithMockUser(username = "1")
        void createMoment_Fail_DuplicateRequest() throws Exception {
            // given
            MomentCreateRequest request = fixture.createMomentCreateRequest();
            String lockKey = fixture.createMomentCreateLockKey();

            given(duplicatePreventionService.generateKey(any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(5), any()))
                    .willThrow(new RuntimeException("동일한 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."));

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("실패 - 인증되지 않은 사용자")
        void createMoment_Fail_Unauthenticated() throws Exception {
            // given
            MomentCreateRequest request = fixture.createMomentCreateRequest();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("기록 수정")
    class UpdateMomentTest {

        @Test
        @DisplayName("성공 - 기록 수정")
        @WithMockUser(username = "1")
        void updateMoment_Success() throws Exception {
            // given
            MomentUpdateRequest request = fixture.createMomentUpdateRequest();
            MomentUpdateResponse response = fixture.createMomentUpdateResponse();
            String lockKey = fixture.createMomentUpdateLockKey();

            given(duplicatePreventionService.generateKey(
                    MomentTestConstants.TEST_USER_ID,
                    "updateMoment",
                    MomentTestConstants.TEST_MOMENT_ID
            )).willReturn(lockKey);

            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });

            given(momentCommandService.updateMoment(any(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_UPDATED_MESSAGE))
                    .andExpect(jsonPath("$.data.id").value(MomentTestConstants.TEST_MOMENT_ID));
        }

        @Test
        @DisplayName("성공 - 부분 수정")
        @WithMockUser(username = "1")
        void updateMoment_Success_Partial() throws Exception {
            // given
            MomentUpdateRequest request = fixture.createPartialMomentUpdateRequest();
            MomentUpdateResponse response = fixture.createMomentUpdateResponse();
            String lockKey = fixture.createMomentUpdateLockKey();

            given(duplicatePreventionService.generateKey(any(), any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });
            given(momentCommandService.updateMoment(any(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 - 유효성 검증 실패")
        @WithMockUser(username = "1")
        void updateMoment_Fail_ValidationError() throws Exception {
            // given
            MomentUpdateRequest request = fixture.createInvalidUpdateRequest();

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 기록")
        @WithMockUser(username = "1")
        void updateMoment_Fail_MomentNotFound() throws Exception {
            // given
            MomentUpdateRequest request = fixture.createMomentUpdateRequest();
            String lockKey = fixture.createMomentUpdateLockKey();

            given(duplicatePreventionService.generateKey(any(), any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });
            given(momentCommandService.updateMoment(any(), any(), any()))
                    .willThrow(new BusinessException(ResponseStatus.MOMENT_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", 999L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패 - 수정 권한 없음")
        @WithMockUser(username = "999")
        void updateMoment_Fail_NoPermission() throws Exception {
            // given
            MomentUpdateRequest request = fixture.createMomentUpdateRequest();
            String lockKey = fixture.createOtherUserLockKey("updateMoment");

            given(duplicatePreventionService.generateKey(any(), any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });
            given(momentCommandService.updateMoment(any(), any(), any()))
                    .willThrow(new BusinessException(ResponseStatus.FORBIDDEN));

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패 - 인증되지 않은 사용자")
        void updateMoment_Fail_Unauthenticated() throws Exception {
            // given
            MomentUpdateRequest request = fixture.createMomentUpdateRequest();

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("기록 삭제")
    class DeleteMomentTest {

        @Test
        @DisplayName("성공 - 기록 삭제")
        @WithMockUser(username = "1")
        void deleteMoment_Success() throws Exception {
            // given
            String lockKey = fixture.createMomentDeleteLockKey();

            given(duplicatePreventionService.generateKey(
                    MomentTestConstants.TEST_USER_ID,
                    "deleteMoment",
                    MomentTestConstants.TEST_MOMENT_ID
            )).willReturn(lockKey);

            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(2), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });

            willDoNothing().given(momentCommandService).deleteMoment(any(), any());

            // when & then
            mockMvc.perform(delete("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_DELETED_MESSAGE));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 기록")
        @WithMockUser(username = "1")
        void deleteMoment_Fail_MomentNotFound() throws Exception {
            // given
            String lockKey = fixture.createMomentDeleteLockKey();

            given(duplicatePreventionService.generateKey(any(), any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(2), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });
            willThrow(new BusinessException(ResponseStatus.MOMENT_NOT_FOUND))
                    .given(momentCommandService).deleteMoment(any(), any());

            // when & then
            mockMvc.perform(delete("/api/v1/users/moments/{moment_id}", 999L)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패 - 삭제 권한 없음")
        @WithMockUser(username = "999")
        void deleteMoment_Fail_NoPermission() throws Exception {
            // given
            String lockKey = fixture.createOtherUserLockKey("deleteMoment");

            given(duplicatePreventionService.generateKey(any(), any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(2), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });
            willThrow(new BusinessException(ResponseStatus.FORBIDDEN))
                    .given(momentCommandService).deleteMoment(any(), any());

            // when & then
            mockMvc.perform(delete("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패 - 인증되지 않은 사용자")
        void deleteMoment_Fail_Unauthenticated() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("추가 테스트 케이스")
    class AdditionalTestCases {

        @Test
        @DisplayName("성공 - 이미지 순서 테스트")
        @WithMockUser(username = "1")
        void createMoment_Success_OrderedImages() throws Exception {
            // given
            MomentCreateRequest request = fixture.createOrderedImagesMomentCreateRequest();
            MomentCreateResponse response = fixture.createMomentCreateResponse();
            String lockKey = fixture.createMomentCreateLockKey();

            given(duplicatePreventionService.generateKey(any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(5), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });
            given(momentCommandService.createMoment(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("성공 - 이미지만 업데이트")
        @WithMockUser(username = "1")
        void updateMoment_Success_ImagesOnly() throws Exception {
            // given
            MomentUpdateRequest request = fixture.createImageUpdateRequest();
            MomentUpdateResponse response = fixture.createMomentUpdateResponse();
            String lockKey = fixture.createMomentUpdateLockKey();

            given(duplicatePreventionService.generateKey(any(), any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });
            given(momentCommandService.updateMoment(any(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("성공 - 공개 설정 변경")
        @WithMockUser(username = "1")
        void updateMoment_Success_VisibilityChange() throws Exception {
            // given
            MomentUpdateRequest request = fixture.createVisibilityUpdateRequest();
            MomentUpdateResponse response = fixture.createMomentUpdateResponse();
            String lockKey = fixture.createMomentUpdateLockKey();

            given(duplicatePreventionService.generateKey(any(), any(), any())).willReturn(lockKey);
            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });
            given(momentCommandService.updateMoment(any(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }
}
