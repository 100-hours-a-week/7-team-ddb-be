package com.dolpin.domain.comment.controller;

import com.dolpin.domain.comment.dto.request.CommentCreateRequest;
import com.dolpin.domain.comment.dto.response.CommentCreateResponse;
import com.dolpin.domain.comment.dto.response.CommentListResponse;
import com.dolpin.domain.comment.service.command.CommentCommandService;
import com.dolpin.domain.comment.service.query.CommentQueryService;
import com.dolpin.global.config.TestConfig;
import com.dolpin.global.constants.CommentTestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CommentController.class, excludeAutoConfiguration = {})
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@ActiveProfiles("test")
@DisplayName("CommentController 테스트")
class CommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentQueryService commentQueryService;

    @MockitoBean
    private CommentCommandService commentCommandService;

    private CommentListResponse mockCommentListResponse;
    private CommentCreateResponse mockCommentCreateResponse;

    @BeforeEach
    void setUp() {
        setupMockResponse();
    }

    private void setupMockResponse() {

        CommentListResponse.CommentDto commentDto = CommentListResponse.CommentDto.builder()
                .id(CommentTestConstants.TEST_COMMENT_ID)
                .user(CommentListResponse.UserDto.builder()
                        .id(CommentTestConstants.TEST_USER_ID)
                        .nickname(CommentTestConstants.TEST_USERNAME)
                        .profileImage(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                        .build())
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .parentCommentId(null)
                .createdAt(LocalDateTime.now())
                .isOwner(CommentTestConstants.IS_OWNER)
                .build();

        CommentListResponse.MetaDto meta = CommentListResponse.MetaDto.builder()
                .pagination(CommentListResponse.PaginationDto.builder()
                        .limit(CommentTestConstants.DEFAULT_PAGE_LIMIT)
                        .nextCursor(null)
                        .hasNext(CommentTestConstants.NO_NEXT)
                        .build())
                .build();

        CommentListResponse.LinksDto links = CommentListResponse.LinksDto.builder()
                .self(CommentListResponse.LinkDto.builder()
                        .href(String.format("/api/v1/moments/%d/comments?limit=%d",
                                CommentTestConstants.TEST_MOMENT_ID, CommentTestConstants.DEFAULT_PAGE_LIMIT))
                        .build())
                .next(null)
                .build();

        mockCommentListResponse = CommentListResponse.builder()
                .comments(List.of(commentDto))
                .meta(meta)
                .links(links)
                .build();

        mockCommentCreateResponse = CommentCreateResponse.builder()
                .id(CommentTestConstants.TEST_COMMENT_ID)
                .user(CommentCreateResponse.UserDto.builder()
                        .id(CommentTestConstants.TEST_USER_ID)
                        .nickname(CommentTestConstants.TEST_USERNAME)
                        .profileImage(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                        .build())
                .content(CommentTestConstants.NEW_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .parentCommentId(null)
                .createdAt(LocalDateTime.now())
                .isOwner(CommentTestConstants.IS_OWNER)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/moments/{moment_id}/comments - 댓글 목록 조회")
    class GetCommentsTest{

        @Test
        @DisplayName("로그인 사용자의 댓글 목록 조회가 정상 동작한다")
        @WithMockUser(username = "1")
        void getComments_WithAuthentication_ReturnsCommentList() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            Long userId = CommentTestConstants.TEST_USER_ID;

            given(commentQueryService.getCommentsByMomentId(
                    eq(momentId), eq(CommentTestConstants.DEFAULT_PAGE_LIMIT), isNull(), eq(userId)))
                    .willReturn(mockCommentListResponse);

            // When & Then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.DEFAULT_PAGE_LIMIT))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.GET_COMMENT_SUCCESS_MESSAGE))
                    .andExpect(jsonPath(CommentTestConstants.COMMENTS_JSON_PATH).isArray())
                    .andExpect(jsonPath("$.data.comments[0].id").value(CommentTestConstants.TEST_COMMENT_ID))
                    .andExpect(jsonPath("$.data.comments[0].content").value(CommentTestConstants.TEST_COMMENT_CONTENT))
                    .andExpect(jsonPath("$.data.comments[0].depth").value(CommentTestConstants.ROOT_COMMENT_DEPTH))
                    .andExpect(jsonPath("$.data.comments[0].is_owner").value(CommentTestConstants.IS_OWNER))
                    .andExpect(jsonPath(CommentTestConstants.META_JSON_PATH).exists())
                    .andExpect(jsonPath(CommentTestConstants.PAGINATION_JSON_PATH).exists());
        }

        @Test
        @DisplayName("커서 기반 페이지네이션이 정상 동작한다")
        @WithMockUser(username = "1")
        void getComments_WithCursor_ReturnsCommentList() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            Long userId = CommentTestConstants.TEST_USER_ID;
            String cursor = CommentTestConstants.TEST_CURSOR;

            given(commentQueryService.getCommentsByMomentId(
                    eq(momentId), eq(CommentTestConstants.CUSTOM_PAGE_LIMIT), eq(cursor), eq(userId)))
                    .willReturn(mockCommentListResponse);

            // When & Then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.CUSTOM_PAGE_LIMIT))
                            .param(CommentTestConstants.CURSOR_PARAM, cursor)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.GET_COMMENT_SUCCESS_MESSAGE));
        }

        @Test
        @DisplayName("빈 댓글 목록을 정상적으로 반환한다")
        @WithMockUser(username = "1")
        void getComments_EmptyList_ReturnsEmptyCommentList() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            Long userId   = CommentTestConstants.TEST_USER_ID;

            CommentListResponse emptyResponse = createEmptyCommentListResponse();
            given(commentQueryService.getCommentsByMomentId(
                    eq(momentId),
                    eq(CommentTestConstants.DEFAULT_PAGE_LIMIT),
                    isNull(),
                    eq(userId)))
                    .willReturn(emptyResponse);

            // When & Then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value(CommentTestConstants.GET_COMMENT_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @ParameterizedTest
        @ValueSource(ints = {5, 10, 20, 50})
        @DisplayName("다양한 limit 값으로 댓글 목록 조회가 정상 동작한다")
        @WithMockUser(username = "1")
        void getComments_WithVariousLimits_ReturnsCommentList(int limit) throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            Long userId = CommentTestConstants.TEST_USER_ID;

            given(commentQueryService.getCommentsByMomentId(
                    eq(momentId), eq(limit), isNull(), eq(userId)))
                    .willReturn(mockCommentListResponse);

            // When & Then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(limit))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.GET_COMMENT_SUCCESS_MESSAGE));
        }

        @Test
        @DisplayName("잘못된 형식의 moment_id로 조회 실패")
        @WithMockUser(username = "1")
        void getComments_WithInvalidMomentId_Returns400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/moments/invalid/comments")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/moments/{moment_id}/comments - 댓글 생성")
    class CreateCommentTest{

        @Test
        @DisplayName("일반 댓글 생성이 정상 동작한다")
        @WithMockUser(username = "1")
        void createComment_RootComment_Success() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            Long userId = CommentTestConstants.TEST_USER_ID;

            CommentCreateRequest request = new CommentCreateRequest(
                    CommentTestConstants.NEW_COMMENT_CONTENT, null
            );

            given(commentCommandService.createComment(eq(momentId), any(CommentCreateRequest.class), eq(userId)))
                    .willReturn(mockCommentCreateResponse);

            // When & Then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.COMMENT_CREATED_MESSAGE))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_ID_JSON_PATH).value(CommentTestConstants.TEST_COMMENT_ID))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_CONTENT_JSON_PATH).value(CommentTestConstants.NEW_COMMENT_CONTENT))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_DEPTH_JSON_PATH).value(CommentTestConstants.ROOT_COMMENT_DEPTH))
                    .andExpect(jsonPath(CommentTestConstants.PARENT_COMMENT_ID_JSON_PATH).doesNotExist())
                    .andExpect(jsonPath("$.data.is_owner").value(CommentTestConstants.IS_OWNER));
        }

        @Test
        @DisplayName("대댓글 생성이 정상 동작한다")
        @WithMockUser(username = "1")
        void createComment_ReplyComment_Success() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            Long userId = CommentTestConstants.TEST_USER_ID;

            CommentCreateRequest request = new CommentCreateRequest(
                    CommentTestConstants.REPLY_CONTENT, CommentTestConstants.PARENT_COMMENT_ID);

            CommentCreateResponse replyResponse = CommentCreateResponse.builder()
                    .id(CommentTestConstants.REPLY_COMMENT_ID)
                    .user(CommentCreateResponse.UserDto.builder()
                            .id(CommentTestConstants.TEST_USER_ID)
                            .nickname(CommentTestConstants.TEST_USERNAME)
                            .profileImage(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                            .build())
                    .content(CommentTestConstants.REPLY_CONTENT)
                    .depth(CommentTestConstants.REPLY_COMMENT_DEPTH)
                    .parentCommentId(CommentTestConstants.PARENT_COMMENT_ID)
                    .createdAt(LocalDateTime.now())
                    .isOwner(CommentTestConstants.IS_OWNER)
                    .momentId(CommentTestConstants.TEST_MOMENT_ID)
                    .build();

            given(commentCommandService.createComment(eq(momentId), any(CommentCreateRequest.class), eq(userId)))
                    .willReturn(replyResponse);

            // When & Then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.COMMENT_CREATED_MESSAGE))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_ID_JSON_PATH).value(CommentTestConstants.REPLY_COMMENT_ID))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_CONTENT_JSON_PATH).value(CommentTestConstants.REPLY_CONTENT))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_DEPTH_JSON_PATH).value(CommentTestConstants.REPLY_COMMENT_DEPTH))
                    .andExpect(jsonPath("$.data.parent_comment_id").value(CommentTestConstants.PARENT_COMMENT_ID));
        }

        @Test
        @DisplayName("댓글 생성 실패 - 내용 누락")
        @WithMockUser(username = "1")
        void createComment_ContentMissing_Fail() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            CommentCreateRequest request = new CommentCreateRequest(CommentTestConstants.NULL_CONTENT, null);

            // When & Then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("댓글 생성 실패 - 빈 내용")
        @WithMockUser(username = "1")
        void createComment_EmptyContent_Fail() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            CommentCreateRequest request = new CommentCreateRequest(CommentTestConstants.EMPTY_CONTENT, null);

            // When & Then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("댓글 생성 실패 - 공백만 있는 내용")
        @WithMockUser(username = "1")
        void createComment_BlankContent_Fail() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            CommentCreateRequest request = new CommentCreateRequest(CommentTestConstants.BLANK_CONTENT, null);

            // When & Then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("댓글 생성 성공 - 최대 길이 내용")
        @WithMockUser(username = "1")
        void createComment_MaxLengthContent_Success() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            Long userId = CommentTestConstants.TEST_USER_ID;
            String maxLengthContent = "A".repeat(CommentTestConstants.MAX_CONTENT_LENGTH);

            CommentCreateRequest request = new CommentCreateRequest(maxLengthContent, null);

            given(commentCommandService.createComment(eq(momentId), any(CommentCreateRequest.class), eq(userId)))
                    .willReturn(mockCommentCreateResponse);

            // When & Then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, momentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.COMMENT_CREATED_MESSAGE));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/moments/{moment_id}/comments/{comment_id} - 댓글 삭제")
    class DeleteCommentTest {

        @Test
        @DisplayName("댓글 삭제가 정상 동작한다")
        @WithMockUser(username = "1")
        void deleteComment_Success() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;
            Long commentId = CommentTestConstants.TEST_COMMENT_ID;
            Long userId = CommentTestConstants.TEST_USER_ID;

            willDoNothing().given(commentCommandService)
                    .deleteComment(eq(momentId), eq(commentId), eq(userId));

            // When & Then
            mockMvc.perform(delete(CommentTestConstants.COMMENT_DELETE_PATH, momentId, commentId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.COMMENT_DELETED_MESSAGE))
                    .andExpect(jsonPath(CommentTestConstants.DATA_JSON_PATH).doesNotExist());
        }

        @Test
        @DisplayName("잘못된 형식의 comment_id로 삭제 실패")
        @WithMockUser(username = "1")
        void deleteComment_WithInvalidCommentId_Returns400() throws Exception {
            // Given
            Long momentId = CommentTestConstants.TEST_MOMENT_ID;

            // When & Then
            mockMvc.perform(delete("/api/v1/moments/{moment_id}/comments/invalid", momentId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 형식의 moment_id로 삭제 실패")
        @WithMockUser(username = "1")
        void deleteComment_WithInvalidMomentId_Returns400() throws Exception {
            // Given
            Long commentId = CommentTestConstants.TEST_COMMENT_ID;

            // When & Then
            mockMvc.perform(delete("/api/v1/moments/invalid/comments/{comment_id}", commentId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }



    private CommentListResponse createEmptyCommentListResponse() {
        CommentListResponse.MetaDto meta = CommentListResponse.MetaDto.builder()
                .pagination(CommentListResponse.PaginationDto.builder()
                        .limit(CommentTestConstants.DEFAULT_PAGE_LIMIT)
                        .nextCursor(null)
                        .hasNext(CommentTestConstants.NO_NEXT)
                        .build())
                .build();

        CommentListResponse.LinksDto links = CommentListResponse.LinksDto.builder()
                .self(CommentListResponse.LinkDto.builder()
                        .href(String.format("/api/v1/moments/%d/comments?limit=%d",
                                CommentTestConstants.TEST_MOMENT_ID, CommentTestConstants.DEFAULT_PAGE_LIMIT))
                        .build())
                .next(null)
                .build();

        return CommentListResponse.builder()
                .comments(Collections.emptyList())
                .meta(meta)
                .links(links)
                .build();
    }
}
