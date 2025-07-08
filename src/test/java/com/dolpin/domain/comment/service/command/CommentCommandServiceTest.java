package com.dolpin.domain.comment.service.command;

import com.dolpin.domain.comment.dto.request.CommentCreateRequest;
import com.dolpin.domain.comment.dto.response.CommentCreateResponse;
import com.dolpin.domain.comment.entity.Comment;
import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.comment.service.template.CommentCreateOperation;
import com.dolpin.domain.comment.service.template.CommentDeleteOperation;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.constants.CommentTestConstants;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentCommandService 테스트 - Template Method 패턴 적용")
class CommentCommandServiceTest {

    @InjectMocks
    private CommentCommandServiceImpl commentCommandService;

    // Template Method 패턴의 Operation들을 Mock으로 주입
    @Mock
    private CommentCreateOperation commentCreateOperation;

    @Mock
    private CommentDeleteOperation commentDeleteOperation;

    // Operation 내부에서 사용되는 실제 의존성들 (Integration Test에서 사용)
    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private UserQueryService userQueryService;

    @Test
    @DisplayName("공개 기록에 일반 댓글 작성 성공")
    void createComment_PublicMoment_Success() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
                CommentTestConstants.NEW_COMMENT_CONTENT,
                null
        );

        CommentCreateResponse expectedResponse = createExpectedCommentResponse();
        given(commentCreateOperation.executeCommentOperation(any()))
                .willReturn(expectedResponse);

        // when
        CommentCreateResponse response = commentCommandService.createComment(
                CommentTestConstants.TEST_MOMENT_ID,
                request,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getId()).isEqualTo(CommentTestConstants.TEST_COMMENT_ID);
        assertThat(response.getContent()).isEqualTo(CommentTestConstants.NEW_COMMENT_CONTENT);
        assertThat(response.getDepth()).isEqualTo(CommentTestConstants.ROOT_COMMENT_DEPTH);
        assertThat(response.getParentCommentId()).isNull();
        assertThat(response.getIsOwner()).isEqualTo(CommentTestConstants.IS_OWNER);
        assertThat(response.getMomentId()).isEqualTo(CommentTestConstants.TEST_MOMENT_ID);

        // CommentCreateOperation이 호출되었는지 검증
        then(commentCreateOperation).should().executeCommentOperation(any());
    }

    @Test
    @DisplayName("대댓글 작성 성공")
    void createComment_WithParentComment_Success() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
                CommentTestConstants.REPLY_CONTENT,
                CommentTestConstants.PARENT_COMMENT_ID
        );

        CommentCreateResponse expectedResponse = createExpectedReplyResponse();

        given(commentCreateOperation.executeCommentOperation(any()))
                .willReturn(expectedResponse);

        // when
        CommentCreateResponse response = commentCommandService.createComment(
                CommentTestConstants.TEST_MOMENT_ID,
                request,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getId()).isEqualTo(CommentTestConstants.REPLY_COMMENT_ID);
        assertThat(response.getContent()).isEqualTo(CommentTestConstants.REPLY_CONTENT);
        assertThat(response.getDepth()).isEqualTo(CommentTestConstants.REPLY_COMMENT_DEPTH);
        assertThat(response.getParentCommentId()).isEqualTo(CommentTestConstants.PARENT_COMMENT_ID);
        assertThat(response.getIsOwner()).isEqualTo(CommentTestConstants.IS_OWNER);

        then(commentCreateOperation).should().executeCommentOperation(any());
    }

    @Test
    @DisplayName("댓글 생성 시 Operation에서 예외 발생")
    void createComment_OperationThrowsException_PropagatesException() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
                CommentTestConstants.NEW_COMMENT_CONTENT,
                null
        );

        BusinessException expectedException = new BusinessException(
                ResponseStatus.MOMENT_NOT_FOUND.withMessage("기록을 찾을 수 없습니다.")
        );

        given(commentCreateOperation.executeCommentOperation(any()))
                .willThrow(expectedException);

        // when & then
        assertThatThrownBy(() -> commentCommandService.createComment(
                CommentTestConstants.TEST_MOMENT_ID,
                request,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("기록을 찾을 수 없습니다.");

        then(commentCreateOperation).should().executeCommentOperation(any());
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_Success() {
        // given - CommentDeleteOperation이 정상적으로 실행되도록 모킹
        given(commentDeleteOperation.executeCommentOperation(any()))
                .willReturn(null); // 삭제는 반환값이 없음

        // when
        commentCommandService.deleteComment(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.TEST_COMMENT_ID,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        then(commentDeleteOperation).should().executeCommentOperation(any());
    }

    @Test
    @DisplayName("댓글 삭제 시 Operation에서 예외 발생")
    void deleteComment_OperationThrowsException_PropagatesException() {
        // given
        BusinessException expectedException = new BusinessException(
                ResponseStatus.FORBIDDEN.withMessage("댓글을 삭제할 권한이 없습니다.")
        );

        given(commentDeleteOperation.executeCommentOperation(any()))
                .willThrow(expectedException);

        // when & then
        assertThatThrownBy(() -> commentCommandService.deleteComment(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.TEST_COMMENT_ID,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("댓글을 삭제할 권한이 없습니다.");

        then(commentDeleteOperation).should().executeCommentOperation(any());
    }

    // Helper methods
    private CommentCreateResponse createExpectedCommentResponse() {
        return CommentCreateResponse.builder()
                .id(CommentTestConstants.TEST_COMMENT_ID)
                .content(CommentTestConstants.NEW_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .parentCommentId(null)
                .isOwner(CommentTestConstants.IS_OWNER)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .createdAt(LocalDateTime.now())
                .user(CommentCreateResponse.UserDto.builder()
                        .id(CommentTestConstants.TEST_USER_ID)
                        .nickname(CommentTestConstants.TEST_USERNAME)
                        .profileImage(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                        .build())
                .build();
    }

    private CommentCreateResponse createExpectedReplyResponse() {
        return CommentCreateResponse.builder()
                .id(CommentTestConstants.REPLY_COMMENT_ID)
                .content(CommentTestConstants.REPLY_CONTENT)
                .depth(CommentTestConstants.REPLY_COMMENT_DEPTH)
                .parentCommentId(CommentTestConstants.PARENT_COMMENT_ID)
                .isOwner(CommentTestConstants.IS_OWNER)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .createdAt(LocalDateTime.now())
                .user(CommentCreateResponse.UserDto.builder()
                        .id(CommentTestConstants.TEST_USER_ID)
                        .nickname(CommentTestConstants.TEST_USERNAME)
                        .profileImage(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                        .build())
                .build();
    }
}
