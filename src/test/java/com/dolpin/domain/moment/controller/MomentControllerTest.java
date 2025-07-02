package com.dolpin.domain.moment.controller;

import com.dolpin.domain.moment.dto.request.MomentCreateRequest;
import com.dolpin.domain.moment.dto.request.MomentUpdateRequest;
import com.dolpin.domain.moment.dto.response.*;
import com.dolpin.domain.moment.service.command.MomentCommandService;
import com.dolpin.domain.moment.service.query.MomentQueryService;
import com.dolpin.global.constants.MomentTestConstants;
import com.dolpin.global.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private MomentListResponse mockMomentListResponse;
    private MomentDetailResponse mockMomentDetailResponse;
    private MomentCreateResponse mockMomentCreateResponse;
    private MomentUpdateResponse mockMomentUpdateResponse;

    @BeforeEach
    void setUp() {
        setupMockResponses();
    }

    private void setupMockResponses() {
        // MomentListResponse 설정
        MomentListResponse.MomentSummaryDto momentDto = MomentListResponse.MomentSummaryDto.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .thumbnail(MomentTestConstants.TEST_IMAGE_1)
                .imagesCount(MomentTestConstants.TEST_IMAGES_COUNT)
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .createdAt(LocalDateTime.now())
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .author(MomentListResponse.AuthorDto.builder()
                        .id(MomentTestConstants.TEST_USER_ID)
                        .nickname(MomentTestConstants.TEST_USERNAME)
                        .profileImage(MomentTestConstants.TEST_PROFILE_IMAGE_URL)
                        .build())
                .build();

        MomentListResponse.MetaDto meta = MomentListResponse.MetaDto.builder()
                .pagination(MomentListResponse.PaginationDto.builder()
                        .limit(MomentTestConstants.DEFAULT_PAGE_LIMIT)
                        .nextCursor(null)
                        .hasNext(MomentTestConstants.NO_NEXT)
                        .build())
                .build();

        mockMomentListResponse = MomentListResponse.builder()
                .moments(List.of(momentDto))
                .meta(meta)
                .links(null)
                .build();

        // MomentDetailResponse 설정
        mockMomentDetailResponse = MomentDetailResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .title(MomentTestConstants.TEST_MOMENT_TITLE)
                .content(MomentTestConstants.TEST_MOMENT_CONTENT)
                .images(MomentTestConstants.TEST_IMAGES)
                .place(MomentDetailResponse.PlaceDetailDto.builder()
                        .id(MomentTestConstants.TEST_PLACE_ID)
                        .name(MomentTestConstants.TEST_PLACE_NAME)
                        .build())
                .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                .isOwner(MomentTestConstants.IS_OWNER)
                .createdAt(LocalDateTime.now())
                .commentCount(MomentTestConstants.DEFAULT_COMMENT_COUNT)
                .viewCount(MomentTestConstants.DEFAULT_VIEW_COUNT)
                .author(MomentDetailResponse.AuthorDto.builder()
                        .id(MomentTestConstants.TEST_USER_ID)
                        .nickname(MomentTestConstants.TEST_USERNAME)
                        .profileImage(MomentTestConstants.TEST_PROFILE_IMAGE_URL)
                        .build())
                .build();

        // MomentCreateResponse 설정
        mockMomentCreateResponse = MomentCreateResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .createdAt(LocalDateTime.now())
                .build();

        // MomentUpdateResponse 설정
        mockMomentUpdateResponse = MomentUpdateResponse.builder()
                .id(MomentTestConstants.TEST_MOMENT_ID)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("기록 목록 조회 테스트")
    class GetMomentsTest {

        @Test
        @DisplayName("전체 기록 목록 조회 성공 - 로그인 사용자")
        @WithMockUser(username = "1")
        void getAllMoments_WithUser_Success() throws Exception {
            // given
            given(momentQueryService.getAllMoments(eq(MomentTestConstants.TEST_USER_ID), any(), any()))
                    .willReturn(mockMomentListResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/moments")
                            .param("limit", String.valueOf(MomentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_LIST_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.moments").isArray())
                    .andExpect(jsonPath("$.data.moments[0].id").value(MomentTestConstants.TEST_MOMENT_ID))
                    .andExpect(jsonPath("$.data.moments[0].title").value(MomentTestConstants.TEST_MOMENT_TITLE));
        }

        @Test
        @DisplayName("본인 기록 목록 조회 성공")
        @WithMockUser(username = "1")
        void getMyMoments_Success() throws Exception {
            // given
            given(momentQueryService.getMyMoments(eq(MomentTestConstants.TEST_USER_ID), any(), any()))
                    .willReturn(mockMomentListResponse);

            // when & then
            mockMvc.perform(get(MomentTestConstants.MY_MOMENTS_PATH)
                            .param("limit", String.valueOf(MomentTestConstants.DEFAULT_PAGE_LIMIT))
                            .param("cursor", MomentTestConstants.TEST_CURSOR))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.USER_MOMENT_LIST_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.moments").isArray());
        }

        @Test
        @DisplayName("다른 사용자 기록 목록 조회 성공")
        @WithMockUser(username = "1") // 인증 추가
        void getUserMoments_Success() throws Exception {
            // given
            given(momentQueryService.getUserMoments(eq(MomentTestConstants.OTHER_USER_ID), any(), any()))
                    .willReturn(mockMomentListResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/{user_id}/moments", MomentTestConstants.OTHER_USER_ID)
                            .param("limit", String.valueOf(MomentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.USER_MOMENT_LIST_SUCCESS_MESSAGE));
        }

        @Test
        @DisplayName("장소별 기록 목록 조회 성공")
        @WithMockUser(username = "1") // 인증 추가
        void getPlaceMoments_Success() throws Exception {
            // given
            given(momentQueryService.getPlaceMoments(eq(MomentTestConstants.TEST_PLACE_ID), any(), any()))
                    .willReturn(mockMomentListResponse);

            // when & then
            mockMvc.perform(get("/api/v1/places/{place_id}/moments", MomentTestConstants.TEST_PLACE_ID)
                            .param("limit", String.valueOf(MomentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.PLACE_MOMENT_LIST_SUCCESS_MESSAGE));
        }

        @Test
        @DisplayName("빈 목록 조회 성공")
        @WithMockUser(username = "1")
        void getAllMoments_EmptyList_Success() throws Exception {
            // given
            MomentListResponse emptyResponse = MomentListResponse.builder()
                    .moments(Collections.emptyList())
                    .meta(MomentListResponse.MetaDto.builder()
                            .pagination(MomentListResponse.PaginationDto.builder()
                                    .limit(MomentTestConstants.DEFAULT_PAGE_LIMIT)
                                    .nextCursor(null)
                                    .hasNext(MomentTestConstants.NO_NEXT)
                                    .build())
                            .build())
                    .build();

            given(momentQueryService.getAllMoments(eq(MomentTestConstants.TEST_USER_ID), any(), any()))
                    .willReturn(emptyResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/moments"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.moments").isEmpty());
        }
    }

    @Nested
    @DisplayName("기록 상세 조회 테스트")
    class GetMomentDetailTest {

        @Test
        @DisplayName("기록 상세 조회 성공 - 로그인 사용자")
        @WithMockUser(username = "1")
        void getMomentDetail_WithUser_Success() throws Exception {
            // given
            given(momentQueryService.getMomentDetail(eq(MomentTestConstants.TEST_MOMENT_ID), eq(MomentTestConstants.TEST_USER_ID)))
                    .willReturn(mockMomentDetailResponse);

            // when & then
            mockMvc.perform(get("/api/v1/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_DETAIL_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.id").value(MomentTestConstants.TEST_MOMENT_ID))
                    .andExpect(jsonPath("$.data.title").value(MomentTestConstants.TEST_MOMENT_TITLE))
                    .andExpect(jsonPath("$.data.content").value(MomentTestConstants.TEST_MOMENT_CONTENT))
                    .andExpect(jsonPath("$.data.is_owner").value(MomentTestConstants.IS_OWNER))
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images.length()").value(MomentTestConstants.TEST_IMAGES_COUNT));
        }
    }

    @Nested
    @DisplayName("기록 생성 테스트")
    class CreateMomentTest {

        @Test
        @DisplayName("기록 생성 성공")
        @WithMockUser(username = "1")
        void createMoment_Success() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .placeId(MomentTestConstants.TEST_PLACE_ID)
                    .placeName(MomentTestConstants.NEW_PLACE_NAME)
                    .images(MomentTestConstants.TEST_IMAGES)
                    .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                    .build();

            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any(MomentCreateRequest.class)))
                    .willReturn(mockMomentCreateResponse);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_CREATED_MESSAGE))
                    .andExpect(jsonPath("$.data.id").value(MomentTestConstants.TEST_MOMENT_ID))
                    .andExpect(jsonPath("$.data.created_at").exists());
        }

        @Test
        @DisplayName("기록 생성 실패 - 제목 누락")
        @WithMockUser(username = "1")
        void createMoment_TitleMissing_Fail() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .placeId(MomentTestConstants.TEST_PLACE_ID)
                    .placeName(MomentTestConstants.NEW_PLACE_NAME)
                    .images(MomentTestConstants.TEST_IMAGES)
                    .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                    .build();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("기록 생성 실패 - 내용 누락")
        @WithMockUser(username = "1")
        void createMoment_ContentMissing_Fail() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .placeId(MomentTestConstants.TEST_PLACE_ID)
                    .placeName(MomentTestConstants.NEW_PLACE_NAME)
                    .images(MomentTestConstants.TEST_IMAGES)
                    .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                    .build();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("기록 생성 실패 - 제목 길이 초과")
        @WithMockUser(username = "1")
        void createMoment_TitleTooLong_Fail() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.LONG_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .placeId(MomentTestConstants.TEST_PLACE_ID)
                    .placeName(MomentTestConstants.NEW_PLACE_NAME)
                    .images(MomentTestConstants.TEST_IMAGES)
                    .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                    .build();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("기록 생성 실패 - 내용 길이 초과")
        @WithMockUser(username = "1")
        void createMoment_ContentTooLong_Fail() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.LONG_CONTENT)
                    .placeId(MomentTestConstants.TEST_PLACE_ID)
                    .placeName(MomentTestConstants.NEW_PLACE_NAME)
                    .images(MomentTestConstants.TEST_IMAGES)
                    .isPublic(MomentTestConstants.DEFAULT_IS_PUBLIC)
                    .build();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("기록 생성 성공 - 최소 필수 정보만")
        @WithMockUser(username = "1")
        void createMoment_MinimalInfo_Success() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .build();

            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any(MomentCreateRequest.class)))
                    .willReturn(mockMomentCreateResponse);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_CREATED_MESSAGE));
        }
    }

    @Nested
    @DisplayName("기록 수정 테스트")
    class UpdateMomentTest {

        @Test
        @DisplayName("기록 수정 성공")
        @WithMockUser(username = "1")
        void updateMoment_Success() throws Exception {
            // given
            MomentUpdateRequest request = MomentUpdateRequest.builder()
                    .title(MomentTestConstants.UPDATED_MOMENT_TITLE)
                    .content(MomentTestConstants.UPDATED_MOMENT_CONTENT)
                    .placeId(MomentTestConstants.UPDATED_PLACE_ID)
                    .placeName(MomentTestConstants.UPDATED_PLACE_NAME)
                    .images(MomentTestConstants.UPDATED_IMAGES)
                    .isPublic(MomentTestConstants.UPDATED_IS_PUBLIC)
                    .build();

            given(momentCommandService.updateMoment(
                    eq(MomentTestConstants.TEST_USER_ID),
                    eq(MomentTestConstants.TEST_MOMENT_ID),
                    any(MomentUpdateRequest.class)))
                    .willReturn(mockMomentUpdateResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_UPDATED_MESSAGE))
                    .andExpect(jsonPath("$.data.id").value(MomentTestConstants.TEST_MOMENT_ID))
                    .andExpect(jsonPath("$.data.updated_at").exists());
        }

        @Test
        @DisplayName("기록 수정 성공 - 부분 수정")
        @WithMockUser(username = "1")
        void updateMoment_PartialUpdate_Success() throws Exception {
            // given
            MomentUpdateRequest request = MomentUpdateRequest.builder()
                    .title(MomentTestConstants.UPDATED_MOMENT_TITLE)
                    .build();

            given(momentCommandService.updateMoment(
                    eq(MomentTestConstants.TEST_USER_ID),
                    eq(MomentTestConstants.TEST_MOMENT_ID),
                    any(MomentUpdateRequest.class)))
                    .willReturn(mockMomentUpdateResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_UPDATED_MESSAGE));
        }

        @Test
        @DisplayName("기록 수정 실패 - 제목 길이 초과")
        @WithMockUser(username = "1")
        void updateMoment_TitleTooLong_Fail() throws Exception {
            // given
            MomentUpdateRequest request = MomentUpdateRequest.builder()
                    .title(MomentTestConstants.LONG_TITLE)
                    .content(MomentTestConstants.UPDATED_MOMENT_CONTENT)
                    .build();

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("기록 수정 실패 - 장소명 길이 초과")
        @WithMockUser(username = "1")
        void updateMoment_PlaceNameTooLong_Fail() throws Exception {
            // given
            MomentUpdateRequest request = MomentUpdateRequest.builder()
                    .title(MomentTestConstants.UPDATED_MOMENT_TITLE)
                    .placeName(MomentTestConstants.LONG_PLACE_NAME)
                    .build();

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("기록 삭제 테스트")
    class DeleteMomentTest {

        @Test
        @DisplayName("기록 삭제 성공")
        @WithMockUser(username = "1")
        void deleteMoment_Success() throws Exception {
            // given
            willDoNothing().given(momentCommandService)
                    .deleteMoment(eq(MomentTestConstants.TEST_USER_ID), eq(MomentTestConstants.TEST_MOMENT_ID));

            // when & then
            mockMvc.perform(delete("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_DELETED_MESSAGE))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    @DisplayName("인증 및 권한 테스트")
    class AuthenticationTest {

        @Test
        @DisplayName("인증 없이 본인 기록 조회 실패")
        void getMyMoments_WithoutAuth_Fail() throws Exception {
            // when & then
            mockMvc.perform(get(MomentTestConstants.MY_MOMENTS_PATH))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("인증 없이 기록 생성 실패")
        void createMoment_WithoutAuth_Fail() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .build();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("인증 없이 기록 수정 실패")
        void updateMoment_WithoutAuth_Fail() throws Exception {
            // given
            MomentUpdateRequest request = MomentUpdateRequest.builder()
                    .title(MomentTestConstants.UPDATED_MOMENT_TITLE)
                    .build();

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("인증 없이 기록 삭제 실패")
        void deleteMoment_WithoutAuth_Fail() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("잘못된 JSON 형식으로 기록 생성 실패")
        @WithMockUser(username = "1")
        void createMoment_InvalidJson_Fail() throws Exception {
            // given
            String invalidJson = "{ \"title\": \"test\", \"content\": }"; // 잘못된 JSON

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("빈 문자열로 기록 생성 실패")
        @WithMockUser(username = "1")
        void createMoment_EmptyString_Fail() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title("")
                    .content("")
                    .build();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("공백만 있는 문자열로 기록 생성 실패")
        @WithMockUser(username = "1")
        void createMoment_WhitespaceString_Fail() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title("   ")
                    .content("   ")
                    .build();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Content-Type 누락으로 기록 생성 실패")
        @WithMockUser(username = "1")
        void createMoment_NoContentType_Fail() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .build();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("파라미터 유효성 테스트")
    class ParameterValidationTest {

        @Test
        @DisplayName("음수 limit 파라미터로 목록 조회")
        @WithMockUser(username = "1")
        void getAllMoments_NegativeLimit_Success() throws Exception {
            // given
            given(momentQueryService.getAllMoments(eq(MomentTestConstants.TEST_USER_ID), eq(-1), any()))
                    .willReturn(mockMomentListResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/moments")
                            .param("limit", "-1"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("매우 큰 limit 파라미터로 목록 조회")
        @WithMockUser(username = "1")
        void getAllMoments_LargeLimit_Success() throws Exception {
            // given
            given(momentQueryService.getAllMoments(eq(MomentTestConstants.TEST_USER_ID), eq(1000), any()))
                    .willReturn(mockMomentListResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/moments")
                            .param("limit", "1000"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("잘못된 형식의 limit 파라미터로 목록 조회 실패")
        @WithMockUser(username = "1")
        void getAllMoments_InvalidLimit_Fail() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/moments")
                            .param("limit", "invalid"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 형식의 moment_id로 상세 조회 실패")
        @WithMockUser(username = "1")
        void getMomentDetail_InvalidMomentId_Fail() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/moments/{moment_id}", "invalid"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryValueTest {

        @Test
        @DisplayName("제목 최대 길이로 기록 생성 성공")
        @WithMockUser(username = "1")
        void createMoment_MaxTitleLength_Success() throws Exception {
            // given
            String maxLengthTitle = "A".repeat(MomentTestConstants.MAX_TITLE_LENGTH);
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(maxLengthTitle)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .build();

            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any(MomentCreateRequest.class)))
                    .willReturn(mockMomentCreateResponse);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("내용 최대 길이로 기록 생성 성공")
        @WithMockUser(username = "1")
        void createMoment_MaxContentLength_Success() throws Exception {
            // given
            String maxLengthContent = "A".repeat(MomentTestConstants.MAX_CONTENT_LENGTH);
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(maxLengthContent)
                    .build();

            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any(MomentCreateRequest.class)))
                    .willReturn(mockMomentCreateResponse);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("장소명 최대 길이로 기록 생성 성공")
        @WithMockUser(username = "1")
        void createMoment_MaxPlaceNameLength_Success() throws Exception {
            // given
            String maxLengthPlaceName = "A".repeat(MomentTestConstants.MAX_PLACE_NAME_LENGTH);
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .placeName(maxLengthPlaceName)
                    .build();

            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any(MomentCreateRequest.class)))
                    .willReturn(mockMomentCreateResponse);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("0으로 limit 설정하여 목록 조회")
        @WithMockUser(username = "1")
        void getAllMoments_ZeroLimit_Success() throws Exception {
            // given
            given(momentQueryService.getAllMoments(eq(MomentTestConstants.TEST_USER_ID), eq(0), any()))
                    .willReturn(mockMomentListResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/moments")
                            .param("limit", "0"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("CSRF 보안 테스트")
    class CsrfTest {

        @Test
        @DisplayName("CSRF 토큰 없이 기록 생성 실패")
        @WithMockUser(username = "1")
        void createMoment_WithoutCsrf_Fail() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .build();

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CSRF 토큰 없이 기록 수정 실패")
        @WithMockUser(username = "1")
        void updateMoment_WithoutCsrf_Fail() throws Exception {
            // given
            MomentUpdateRequest request = MomentUpdateRequest.builder()
                    .title(MomentTestConstants.UPDATED_MOMENT_TITLE)
                    .build();

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CSRF 토큰 없이 기록 삭제 실패")
        @WithMockUser(username = "1")
        void deleteMoment_WithoutCsrf_Fail() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("응답 형식 테스트")
    class ResponseFormatTest {

        @Test
        @DisplayName("기록 목록 조회 응답 형식 검증")
        @WithMockUser(username = "1")
        void getAllMoments_ResponseFormat_Valid() throws Exception {
            // given
            given(momentQueryService.getAllMoments(eq(MomentTestConstants.TEST_USER_ID), any(), any()))
                    .willReturn(mockMomentListResponse);

            // when & then
            mockMvc.perform(get("/api/v1/users/moments"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.moments").isArray())
                    .andExpect(jsonPath("$.data.meta").exists())
                    .andExpect(jsonPath("$.data.meta.pagination").exists())
                    .andExpect(jsonPath("$.data.meta.pagination.limit").isNumber())
                    .andExpect(jsonPath("$.data.meta.pagination.has_next").isBoolean());
        }

        @Test
        @DisplayName("기록 상세 조회 응답 형식 검증")
        @WithMockUser(username = "1")
        void getMomentDetail_ResponseFormat_Valid() throws Exception {
            // given
            given(momentQueryService.getMomentDetail(eq(MomentTestConstants.TEST_MOMENT_ID), eq(MomentTestConstants.TEST_USER_ID)))
                    .willReturn(mockMomentDetailResponse);

            // when & then
            mockMvc.perform(get("/api/v1/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.title").isString())
                    .andExpect(jsonPath("$.data.content").isString())
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.is_public").isBoolean())
                    .andExpect(jsonPath("$.data.is_owner").isBoolean())
                    .andExpect(jsonPath("$.data.created_at").isString())
                    .andExpect(jsonPath("$.data.comment_count").isNumber())
                    .andExpect(jsonPath("$.data.view_count").isNumber())
                    .andExpect(jsonPath("$.data.author").exists())
                    .andExpect(jsonPath("$.data.author.id").isNumber())
                    .andExpect(jsonPath("$.data.author.nickname").isString());
        }

        @Test
        @DisplayName("기록 생성 응답 형식 검증")
        @WithMockUser(username = "1")
        void createMoment_ResponseFormat_Valid() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .build();

            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any(MomentCreateRequest.class)))
                    .willReturn(mockMomentCreateResponse);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.created_at").isString());
        }
    }

    @Nested
    @DisplayName("다양한 시나리오 테스트")
    class VariousScenarioTest {

        @Test
        @DisplayName("이미지가 포함된 기록 생성")
        @WithMockUser(username = "1")
        void createMoment_WithImages_Success() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .images(MomentTestConstants.ORDERED_IMAGES)
                    .build();

            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any(MomentCreateRequest.class)))
                    .willReturn(mockMomentCreateResponse);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value(MomentTestConstants.MOMENT_CREATED_MESSAGE));
        }

        @Test
        @DisplayName("비공개 기록 생성")
        @WithMockUser(username = "1")
        void createMoment_Private_Success() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .isPublic(false)
                    .build();

            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any(MomentCreateRequest.class)))
                    .willReturn(mockMomentCreateResponse);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("장소 정보가 포함된 기록 생성")
        @WithMockUser(username = "1")
        void createMoment_WithPlace_Success() throws Exception {
            // given
            MomentCreateRequest request = MomentCreateRequest.builder()
                    .title(MomentTestConstants.NEW_MOMENT_TITLE)
                    .content(MomentTestConstants.NEW_MOMENT_CONTENT)
                    .placeId(MomentTestConstants.TEST_PLACE_ID)
                    .placeName(MomentTestConstants.TEST_PLACE_NAME)
                    .build();

            given(momentCommandService.createMoment(eq(MomentTestConstants.TEST_USER_ID), any(MomentCreateRequest.class)))
                    .willReturn(mockMomentCreateResponse);

            // when & then
            mockMvc.perform(post(MomentTestConstants.MOMENTS_BASE_PATH)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("공개 설정 변경")
        @WithMockUser(username = "1")
        void updateMoment_ChangeVisibility_Success() throws Exception {
            // given
            MomentUpdateRequest request = MomentUpdateRequest.builder()
                    .isPublic(false)
                    .build();

            given(momentCommandService.updateMoment(
                    eq(MomentTestConstants.TEST_USER_ID),
                    eq(MomentTestConstants.TEST_MOMENT_ID),
                    any(MomentUpdateRequest.class)))
                    .willReturn(mockMomentUpdateResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/users/moments/{moment_id}", MomentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("이미지만 수정")
        @WithMockUser(username = "1")
        void updateMoment_OnlyImages_Success() throws Exception {
            // given
            MomentUpdateRequest request = MomentUpdateRequest.builder()
                    .images(MomentTestConstants.UPDATED_IMAGES)
                    .build();

            given(momentCommandService.updateMoment(
                    eq(MomentTestConstants.TEST_USER_ID),
                    eq(MomentTestConstants.TEST_MOMENT_ID),
                    any(MomentUpdateRequest.class)))
                    .willReturn(mockMomentUpdateResponse);

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
