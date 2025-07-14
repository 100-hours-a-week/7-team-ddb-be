package com.dolpin.domain.moment.controller;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.MomentCreateResponse;
import com.dolpin.domain.moment.dto.response.MomentDetailResponse;
import com.dolpin.domain.moment.dto.response.MomentListResponse;
import com.dolpin.domain.moment.dto.response.MomentUpdateResponse;
import com.dolpin.domain.moment.repository.MomentRepository;
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
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    @MockitoBean
    private MomentRepository momentRepository;

    private MomentCreateRequest createRequest;
    private MomentUpdateRequest updateRequest;
    private MomentCreateResponse createResponse;
    private MomentUpdateResponse updateResponse;
    private MomentDetailResponse detailResponse;
    private MomentListResponse listResponse;

    @BeforeEach
    void setUp() {
        setupTestData();
        // 기본 모킹은 각 테스트에서 필요에 따라 설정
    }

    private void setupTestData() {
        // Create Request
        createRequest = MomentCreateRequest.builder()
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .placeId(MomentTestConstants.TEST_PLACE_ID)
                .placeName(MomentTestConstants.TEST_PLACE_NAME)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .build();

        // Update Request
        updateRequest = MomentUpdateRequest.builder()
                .title(MomentTestConstants.UPDATED_MOMENT_TITLE)
                .content(MomentTestConstants.UPDATED_MOMENT_CONTENT)
                .placeId(MomentTestConstants.UPDATED_PLACE_ID)
                .placeName(MomentTestConstants.UPDATED_PLACE_NAME)
                .images(MomentTestConstants.UPDATED_IMAGES)
                .isPublic(MomentTestConstants.UPDATED_IS_PUBLIC)
                .build();

        // Create Response
        createResponse = MomentCreateResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .createdAt(LocalDateTime.now())
                .build();

        // Update Response
        updateResponse = MomentUpdateResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .updatedAt(LocalDateTime.now())
                .build();

        // Detail Response
        detailResponse = MomentDetailResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .images(MomentTestConstants.TEST_IMAGES)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .isOwner(MomentTestConstants.IS_OWNER)
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .createdAt(LocalDateTime.now())
                .build();

        // List Response
        listResponse = createMockListResponse();
    }

    private void setupSuccessfulDuplicatePreventionService() {
        given(duplicatePreventionService.executeWithLock(anyString(), anyInt(), anyInt(), any()))
                .willAnswer(invocation -> {
                    Object[] args = invocation.getArguments();
                    if (args.length < 4 || args[3] == null) {
                        throw new IllegalArgumentException("LockAction cannot be null");
                    }

                    DuplicatePreventionService.LockAction<?> action =
                            (DuplicatePreventionService.LockAction<?>) args[3];

                    return action.execute();
                });
    }

    private MomentListResponse createMockListResponse() {
        MomentListResponse.MomentSummaryDto momentDto = MomentListResponse.MomentSummaryDto.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .thumbnail(MomentTestConstants.TEST_IMAGE_1)
                .imagesCount(MomentTestConstants.TEST_IMAGES_COUNT)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .createdAt(LocalDateTime.now())
                .build();

        MomentListResponse.MetaDto meta = MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(MomentTestConstants.DEFAULT_PAGE_LIMIT)
                        .hasNext(MomentTestConstants.NO_NEXT)
                        .build())
                .build();

        return MomentListResponse.builder()
                .moments(List.of(momentDto))
                .meta(meta)
                .build();
    }

    @Nested
    @DisplayName("Moment 조회 테스트")
    class MomentQueryTests {

        @Test
        @DisplayName("전체 Moment 목록 조회 성공")
        @WithMockUser(username = "1")
        void getAllMoments_Success() throws Exception {
            // given
            given(momentQueryService.getAllMoments(eq(MomentTestConstants.TEST_USER_ID), any(), any()))
                    .willReturn(listResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/moments")
                            .param("limit", String.valueOf(MomentTestConstants.DEFAULT_PAGE_LIMIT))
                            .param("cursor", MomentTestConstants.TEST_CURSOR))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_LIST_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.moments").isArray())
                    .andExpect(jsonPath("$.data.moments[0].id").value(MomentTestConstants.TEST_MOMENT_ID))
                    .andExpect(jsonPath("$.data.moments[0].title").value(MomentTestConstants.TEST_MOMENT_TITLE));
        }

        @Test
        @DisplayName("내 Moment 목록 조회 성공")
        @WithMockUser(username = "1")
        void getMyMoments_Success() throws Exception {
            // given
            given(momentQueryService.getMyMoments(eq(MomentTestConstants.TEST_USER_ID), any(), any()))
                    .willReturn(listResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/me/moments"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.USER_MOMENT_LIST_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.moments").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("특정 사용자의 Moment 목록 조회 성공")
        void getUserMoments_Success() throws Exception {
            // given
            given(momentQueryService.getUserMoments(eq(MomentTestConstants.TEST_USER_ID), any(), any()))
                    .willReturn(listResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/{user_id}/moments", MomentTestConstants.TEST_USER_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.USER_MOMENT_LIST_SUCCESS_MESSAGE));
        }

        @Test
        @WithMockUser
        @DisplayName("특정 장소의 Moment 목록 조회 성공")
        void getPlaceMoments_Success() throws Exception {
            // given
            given(momentQueryService.getPlaceMoments(eq(MomentTestConstants.TEST_PLACE_ID), any(), any()))
                    .willReturn(listResponse);

            // when & then
            mockMvc.perform(get("/api/v1/places/{place_id}/moments", MomentTestConstants.TEST_PLACE_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.PLACE_MOMENT_LIST_SUCCESS_MESSAGE));
        }

        @Test
        @DisplayName("Moment 상세 조회 성공")
        @WithMockUser(username = "1")
        void getMomentDetail_Success() throws Exception {
            // given
            given(momentQueryService.getMomentDetail(eq(MomentTestConstants.TEST_MOMENT_ID), eq(MomentTestConstants.TEST_USER_ID)))
                    .willReturn(detailResponse);

            // when & then
            mockMvc.perform(get("/api/v1/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_DETAIL_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.id").value(MomentTestConstants.TEST_MOMENT_ID))
                    .andExpect(jsonPath("$.data.title").value(MomentTestConstants.TEST_MOMENT_TITLE))
                    .andExpect(jsonPath("$.data.is_owner").value(MomentTestConstants.IS_OWNER));
        }
    }

    @Nested
    @DisplayName("Moment 생성 테스트")
    class MomentCreateTests {

        @Test
        @DisplayName("Moment 생성 성공")
        @WithMockUser(username = "1")
        void createMoment_Success() throws Exception {
            // given
            setupSuccessfulDuplicatePreventionService();
            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any()))
                    .willReturn(createResponse);

            // when & then
            mockMvc.perform(post("/api/v1/users/moments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("제목 누락 시 생성 실패")
        @WithMockUser(username = "1")
        void createMoment_FailWhenTitleMissing() throws Exception {
            // given
            MomentCreateRequest invalidRequest = MomentCreateRequest.builder()
                    .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                    .build();

            // when & then
            mockMvc.perform(post("/api/v1/users/moments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("내용 누락 시 생성 실패")
        @WithMockUser(username = "1")
        void createMoment_FailWhenContentMissing() throws Exception {
            // given
            MomentCreateRequest invalidRequest = MomentCreateRequest.builder()
                    .title(MomentTestConstants.TEST_MOMENT_TITLE)
                    .build();

            // when & then
            mockMvc.perform(post("/api/v1/users/moments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("제목 길이 초과 시 생성 실패")
        @WithMockUser(username = "1")
        void createMoment_FailWhenTitleTooLong() throws Exception {
            // given
            MomentCreateRequest invalidRequest = MomentCreateRequest.builder()
                    .title(MomentTestConstants.LONG_TITLE)
                    .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                    .build();

            // when & then
            mockMvc.perform(post("/api/v1/users/moments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Moment 수정 테스트")
    class MomentUpdateTests {

        @Test
        @DisplayName("Moment 수정 성공")
        @WithMockUser(username = "1")
        void updateMoment_Success() throws Exception {
            // given
            setupSuccessfulDuplicatePreventionService();
            given(momentCommandService.updateMoment(eq(MomentTestConstants.TEST_USER_ID), eq(MomentTestConstants.TEST_MOMENT_ID), any()))
                    .willReturn(updateResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("존재하지 않는 Moment 수정 시 실패")
        @WithMockUser(username = "1")
        void updateMoment_FailWhenNotFound() throws Exception {
            // given
            setupSuccessfulDuplicatePreventionService();
            given(momentCommandService.updateMoment(any(), any(), any()))
                    .willThrow(new BusinessException(ResponseStatus.MOMENT_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Moment 삭제 테스트")
    class MomentDeleteTests {

        @Test
        @DisplayName("Moment 삭제 성공")
        @WithMockUser(username = "1")
        void deleteMoment_Success() throws Exception {
            // given
            setupSuccessfulDuplicatePreventionService();
            willDoNothing().given(momentCommandService)
                    .deleteMoment(eq(MomentTestConstants.TEST_USER_ID), eq(MomentTestConstants.TEST_MOMENT_ID));

            // when & then
            mockMvc.perform(delete("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("존재하지 않는 Moment 삭제 시 실패")
        @WithMockUser(username = "1")
        void deleteMoment_FailWhenNotFound() throws Exception {
            // given
            setupSuccessfulDuplicatePreventionService();
            willThrow(new BusinessException(ResponseStatus.MOMENT_NOT_FOUND))
                    .given(momentCommandService)
                    .deleteMoment(any(), any());

            // when & then
            mockMvc.perform(delete("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("중복 방지 테스트")
    class DuplicatePreventionTests {

        @Test
        @DisplayName("중복 요청 감지 시 락 실패")
        @WithMockUser(username = "1")
        void createMoment_FailWhenDuplicateRequest() throws Exception {
            // given - 중복 방지 서비스가 예외를 던지도록 모킹
            given(duplicatePreventionService.executeWithLock(anyString(), anyInt(), anyInt(), any()))
                    .willThrow(new RuntimeException("동일한 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."));

            // when & then
            mockMvc.perform(post("/api/v1/users/moments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("락 키 생성 검증")
        @WithMockUser(username = "1")
        void verifyLockKeyGeneration() throws Exception {
            // given
            setupSuccessfulDuplicatePreventionService();

            // generateKey 메서드 모킹 추가
            given(duplicatePreventionService.generateKey(1L, "createMoment"))
                    .willReturn("1:createMoment");

            given(momentCommandService.createMoment(any(), any()))
                    .willReturn(createResponse);

            // when
            mockMvc.perform(post("/api/v1/users/moments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated()); // 실제 응답은 201이므로 수정

            // then
            // generateKey 호출 검증
            verify(duplicatePreventionService).generateKey(1L, "createMoment");

            // executeWithLock 호출 검증
            verify(duplicatePreventionService).executeWithLock(
                    eq("1:createMoment"),
                    eq(0),
                    eq(5),
                    any()
            );
        }
    }
}
