package com.dolpin.domain.comment.controller;

import com.dolpin.domain.comment.dto.request.CommentCreateRequest;
import com.dolpin.domain.comment.dto.response.CommentCreateResponse;
import com.dolpin.domain.comment.dto.response.CommentListResponse;
import com.dolpin.global.fixture.CommentTestFixture;
import com.dolpin.domain.comment.service.command.CommentCommandService;
import com.dolpin.domain.comment.service.query.CommentQueryService;
import com.dolpin.global.constants.CommentTestConstants;
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

import java.time.Duration;

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
@WebMvcTest(CommentController.class)
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

    @MockitoBean
    private DuplicatePreventionService duplicatePreventionService;

    private CommentTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new CommentTestFixture();
    }

    // DuplicatePreventionService 기본 모킹 설정
    private void setupSuccessfulDuplicatePreventionService() {
        String lockKey = fixture.createLockKey();
        String contentKey = "content:1:1:12345"; // 테스트용 content key

        // generateKey 모킹
        given(duplicatePreventionService.generateKey(
                CommentTestConstants.TEST_USER_ID,
                "createComment",
                CommentTestConstants.TEST_MOMENT_ID
        )).willReturn(lockKey);

        // generateContentKey 모킹
        given(duplicatePreventionService.generateContentKey(
                CommentTestConstants.TEST_USER_ID,
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.TEST_COMMENT_CONTENT
        )).willReturn(contentKey);

        // checkDuplicateContent 모킹 (아무것도 하지 않음 - 정상 케이스)
        willDoNothing().given(duplicatePreventionService)
                .checkDuplicateContent(eq(contentKey), anyString(), any(Duration.class));

        // executeWithLock 모킹
        given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                .willAnswer(invocation -> {
                    DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                    return action.execute();
                });
    }

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetCommentsTest {

        @Test
        @DisplayName("성공 - 인증된 사용자")
        @WithMockUser(username = "1")
        void getComments_Success_AuthenticatedUser() throws Exception {
            // given
            CommentListResponse response = fixture.createCommentListResponse();
            given(commentQueryService.getCommentsByMomentId(
                    CommentTestConstants.TEST_MOMENT_ID,
                    CommentTestConstants.DEFAULT_PAGE_LIMIT,
                    null,
                    CommentTestConstants.TEST_USER_ID
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.GET_COMMENT_SUCCESS_MESSAGE))
                    .andExpect(jsonPath(CommentTestConstants.COMMENTS_JSON_PATH).isArray());
        }

        @Test
        @DisplayName("성공 - 커서 페이지네이션")
        @WithMockUser(username = "1")
        void getComments_Success_WithCursor() throws Exception {
            // given
            CommentListResponse response = fixture.createCommentListResponseWithCursor();
            given(commentQueryService.getCommentsByMomentId(
                    CommentTestConstants.TEST_MOMENT_ID,
                    CommentTestConstants.CUSTOM_PAGE_LIMIT,
                    CommentTestConstants.TEST_CURSOR,
                    CommentTestConstants.TEST_USER_ID
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.CUSTOM_PAGE_LIMIT))
                            .param(CommentTestConstants.CURSOR_PARAM, CommentTestConstants.TEST_CURSOR))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(CommentTestConstants.LIMIT_JSON_PATH).value(CommentTestConstants.CUSTOM_PAGE_LIMIT));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 기록")
        @WithMockUser(username = "1")
        void getComments_Fail_MomentNotFound() throws Exception {
            // given
            given(commentQueryService.getCommentsByMomentId(anyLong(), anyInt(), any(), anyLong()))
                    .willThrow(new BusinessException(ResponseStatus.MOMENT_NOT_FOUND));

            // when & then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.DELETED_MOMENT_ID)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패 - 접근 권한 없음")
        @WithMockUser(username = "999")
        void getComments_Fail_AccessDenied() throws Exception {
            // given
            given(commentQueryService.getCommentsByMomentId(anyLong(), anyInt(), any(), anyLong()))
                    .willThrow(new BusinessException(ResponseStatus.FORBIDDEN));

            // when & then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("댓글 생성")
    class CreateCommentTest {

        @Test
        @DisplayName("성공 - 일반 댓글 생성")
        @WithMockUser(username = "1")
        void createComment_Success_RootComment() throws Exception {
            // given
            CommentCreateRequest request = fixture.createCommentCreateRequest();
            CommentCreateResponse response = fixture.createCommentCreateResponse();

            setupSuccessfulDuplicatePreventionService();
            given(commentCommandService.createComment(any(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.COMMENT_CREATED_MESSAGE))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_ID_JSON_PATH).value(CommentTestConstants.TEST_COMMENT_ID))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_CONTENT_JSON_PATH).value(CommentTestConstants.TEST_COMMENT_CONTENT))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_DEPTH_JSON_PATH).value(CommentTestConstants.ROOT_COMMENT_DEPTH))
                    .andExpect(jsonPath(CommentTestConstants.IS_OWNER_JSON_PATH).value(CommentTestConstants.IS_OWNER));
        }

        @Test
        @DisplayName("성공 - 대댓글 생성")
        @WithMockUser(username = "1")
        void createComment_Success_ReplyComment() throws Exception {
            // given
            CommentCreateRequest request = fixture.createReplyCommentCreateRequest();
            CommentCreateResponse response = fixture.createReplyCommentCreateResponse();

            setupSuccessfulDuplicatePreventionService();
            given(commentCommandService.createComment(any(), any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.COMMENT_CREATED_MESSAGE))
                    .andExpect(jsonPath(CommentTestConstants.COMMENT_DEPTH_JSON_PATH).value(CommentTestConstants.REPLY_COMMENT_DEPTH))
                    .andExpect(jsonPath(CommentTestConstants.PARENT_COMMENT_ID_JSON_PATH).value(CommentTestConstants.PARENT_COMMENT_ID));
        }

        @Test
        @DisplayName("실패 - 중복 요청")
        @WithMockUser(username = "1")
        void createComment_Fail_DuplicateRequest() throws Exception {
            // given
            CommentCreateRequest request = fixture.createCommentCreateRequest();
            String lockKey = fixture.createLockKey();

            given(duplicatePreventionService.generateKey(
                    CommentTestConstants.TEST_USER_ID,
                    "createComment",
                    CommentTestConstants.TEST_MOMENT_ID
            )).willReturn(lockKey);

            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                    .willThrow(new RuntimeException("동일한 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."));

            // when & then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("실패 - 중복 내용")
        @WithMockUser(username = "1")
        void createComment_Fail_DuplicateContent() throws Exception {
            // given
            CommentCreateRequest request = fixture.createCommentCreateRequest();
            String lockKey = fixture.createLockKey();
            String contentKey = "content:1:1:12345";

            given(duplicatePreventionService.generateKey(
                    CommentTestConstants.TEST_USER_ID,
                    "createComment",
                    CommentTestConstants.TEST_MOMENT_ID
            )).willReturn(lockKey);

            given(duplicatePreventionService.generateContentKey(
                    CommentTestConstants.TEST_USER_ID,
                    CommentTestConstants.TEST_MOMENT_ID,
                    CommentTestConstants.TEST_COMMENT_CONTENT
            )).willReturn(contentKey);

            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });

            // checkDuplicateContent에서 예외 발생
            willThrow(new IllegalArgumentException("동일한 댓글이 이미 등록되어 있습니다."))
                    .given(duplicatePreventionService)
                    .checkDuplicateContent(eq(contentKey), anyString(), any(Duration.class));

            // when & then - IllegalArgumentException은 400으로 처리됨
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest()) // 400으로 변경
                    .andExpect(jsonPath("$.message").value("동일한 댓글이 이미 등록되어 있습니다."));
        }

        @Test
        @DisplayName("실패 - 내용 누락")
        @WithMockUser(username = "1")
        void createComment_Fail_EmptyContent() throws Exception {
            // given
            CommentCreateRequest request = fixture.createInvalidCommentCreateRequest();

            // when & then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 내용 길이 초과")
        @WithMockUser(username = "1")
        void createComment_Fail_ContentTooLong() throws Exception {
            // given
            CommentCreateRequest request = fixture.createLongContentCommentCreateRequest();

            // when & then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 타인의 비공개 기록")
        @WithMockUser(username = "999")
        void createComment_Fail_PrivateMomentAccess() throws Exception {
            // given
            CommentCreateRequest request = fixture.createCommentCreateRequest();
            String lockKey = fixture.createOtherUserLockKey();

            given(duplicatePreventionService.generateKey(999L, "createComment", CommentTestConstants.TEST_MOMENT_ID))
                    .willReturn(lockKey);

            given(duplicatePreventionService.generateContentKey(999L, CommentTestConstants.TEST_MOMENT_ID, CommentTestConstants.TEST_COMMENT_CONTENT))
                    .willReturn("content:999:1:12345");

            willDoNothing().given(duplicatePreventionService)
                    .checkDuplicateContent(anyString(), anyString(), any(Duration.class));

            given(duplicatePreventionService.executeWithLock(eq(lockKey), eq(0), eq(3), any()))
                    .willAnswer(invocation -> {
                        DuplicatePreventionService.LockAction<?> action = invocation.getArgument(3);
                        return action.execute();
                    });

            given(commentCommandService.createComment(anyLong(), any(), anyLong()))
                    .willThrow(new BusinessException(ResponseStatus.FORBIDDEN));

            // when & then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 부모 댓글")
        @WithMockUser(username = "1")
        void createComment_Fail_InvalidParentComment() throws Exception {
            // given
            CommentCreateRequest request = fixture.createInvalidParentCommentCreateRequest();

            setupSuccessfulDuplicatePreventionService();
            given(commentCommandService.createComment(anyLong(), any(), anyLong()))
                    .willThrow(new BusinessException(ResponseStatus.INVALID_PARAMETER));

            // when & then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 인증되지 않은 사용자")
        void createComment_Fail_Unauthenticated() throws Exception {
            // given
            CommentCreateRequest request = fixture.createCommentCreateRequest();

            // when & then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteCommentTest {

        @Test
        @DisplayName("성공 - 댓글 삭제")
        @WithMockUser(username = "1")
        void deleteComment_Success() throws Exception {
            // given
            willDoNothing().given(commentCommandService)
                    .deleteComment(
                            CommentTestConstants.TEST_MOMENT_ID,
                            CommentTestConstants.TEST_COMMENT_ID,
                            CommentTestConstants.TEST_USER_ID
                    );

            // when & then
            mockMvc.perform(delete(CommentTestConstants.COMMENT_DELETE_PATH,
                            CommentTestConstants.TEST_MOMENT_ID,
                            CommentTestConstants.TEST_COMMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath(CommentTestConstants.MESSAGE_JSON_PATH).value(CommentTestConstants.COMMENT_DELETED_MESSAGE));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 댓글")
        @WithMockUser(username = "1")
        void deleteComment_Fail_CommentNotFound() throws Exception {
            // given
            willThrow(new BusinessException(ResponseStatus.USER_NOT_FOUND))
                    .given(commentCommandService)
                    .deleteComment(anyLong(), anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete(CommentTestConstants.COMMENT_DELETE_PATH,
                            CommentTestConstants.TEST_MOMENT_ID,
                            CommentTestConstants.NON_EXISTENT_COMMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패 - 삭제 권한 없음")
        @WithMockUser(username = "999")
        void deleteComment_Fail_NoPermission() throws Exception {
            // given
            willThrow(new BusinessException(ResponseStatus.FORBIDDEN))
                    .given(commentCommandService)
                    .deleteComment(anyLong(), anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete(CommentTestConstants.COMMENT_DELETE_PATH,
                            CommentTestConstants.TEST_MOMENT_ID,
                            CommentTestConstants.TEST_COMMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 기록")
        @WithMockUser(username = "1")
        void deleteComment_Fail_MomentNotFound() throws Exception {
            // given
            willThrow(new BusinessException(ResponseStatus.MOMENT_NOT_FOUND))
                    .given(commentCommandService)
                    .deleteComment(anyLong(), anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete(CommentTestConstants.COMMENT_DELETE_PATH,
                            CommentTestConstants.DELETED_MOMENT_ID,
                            CommentTestConstants.TEST_COMMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패 - 인증되지 않은 사용자")
        void deleteComment_Fail_Unauthenticated() throws Exception {
            // when & then
            mockMvc.perform(delete(CommentTestConstants.COMMENT_DELETE_PATH,
                            CommentTestConstants.TEST_MOMENT_ID,
                            CommentTestConstants.TEST_COMMENT_ID)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("추가 테스트 케이스")
    class AdditionalTestCases {

        @Test
        @DisplayName("성공 - 빈 댓글 목록 조회")
        @WithMockUser(username = "1")
        void getComments_Success_EmptyList() throws Exception {
            // given
            CommentListResponse response = fixture.createEmptyCommentListResponse();
            given(commentQueryService.getCommentsByMomentId(
                    CommentTestConstants.TEST_MOMENT_ID,
                    CommentTestConstants.DEFAULT_PAGE_LIMIT,
                    null,
                    CommentTestConstants.TEST_USER_ID
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(CommentTestConstants.COMMENTS_JSON_PATH).isEmpty());
        }

        @Test
        @DisplayName("성공 - 페이지네이션 테스트")
        @WithMockUser(username = "1")
        void getComments_Success_Pagination() throws Exception {
            // given
            CommentListResponse response = fixture.createPaginatedCommentListResponse(
                    CommentTestConstants.CUSTOM_PAGE_LIMIT,
                    CommentTestConstants.HAS_NEXT
            );
            given(commentQueryService.getCommentsByMomentId(
                    CommentTestConstants.TEST_MOMENT_ID,
                    CommentTestConstants.CUSTOM_PAGE_LIMIT,
                    null,
                    CommentTestConstants.TEST_USER_ID
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.CUSTOM_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(CommentTestConstants.LIMIT_JSON_PATH).value(CommentTestConstants.CUSTOM_PAGE_LIMIT))
                    .andExpect(jsonPath(CommentTestConstants.HAS_NEXT_JSON_PATH).value(CommentTestConstants.HAS_NEXT))
                    .andExpect(jsonPath("$.data.comments").value(hasSize(CommentTestConstants.CUSTOM_PAGE_LIMIT)));
        }

        @Test
        @DisplayName("성공 - 스레드 구조 댓글 조회")
        @WithMockUser(username = "1")
        void getComments_Success_ThreadedComments() throws Exception {
            // given
            CommentListResponse response = fixture.createThreadedCommentListResponse();
            given(commentQueryService.getCommentsByMomentId(
                    CommentTestConstants.TEST_MOMENT_ID,
                    CommentTestConstants.DEFAULT_PAGE_LIMIT,
                    null,
                    CommentTestConstants.TEST_USER_ID
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.DEFAULT_PAGE_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(CommentTestConstants.COMMENTS_JSON_PATH).isArray())
                    .andExpect(jsonPath("$.data.comments[0].depth").value(CommentTestConstants.ROOT_COMMENT_DEPTH))
                    .andExpect(jsonPath("$.data.comments[1].depth").value(CommentTestConstants.REPLY_COMMENT_DEPTH));
        }

        @Test
        @DisplayName("실패 - 댓글 내용에 공백만 있는 경우 검증")
        @WithMockUser(username = "1")
        void createComment_Fail_BlankContent() throws Exception {
            // given
            CommentCreateRequest request = fixture.createBlankContentCommentCreateRequest();

            // when & then
            mockMvc.perform(post(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("성공 - 최대 페이지 크기 제한 테스트")
        @WithMockUser(username = "1")
        void getComments_Success_MaxLimitTest() throws Exception {
            // given
            CommentListResponse response = fixture.createCommentListResponse();
            given(commentQueryService.getCommentsByMomentId(
                    CommentTestConstants.TEST_MOMENT_ID,
                    CommentTestConstants.OVER_MAX_LIMIT,
                    null,
                    CommentTestConstants.TEST_USER_ID
            )).willReturn(response);

            // when & then
            mockMvc.perform(get(CommentTestConstants.COMMENTS_BASE_PATH, CommentTestConstants.TEST_MOMENT_ID)
                            .param(CommentTestConstants.LIMIT_PARAM, String.valueOf(CommentTestConstants.OVER_MAX_LIMIT)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }
}
