package com.dolpin.domain.comment.service.command;

import com.dolpin.domain.comment.dto.request.CommentCreateRequest;
import com.dolpin.domain.comment.dto.response.CommentCreateResponse;
import com.dolpin.domain.comment.entity.Comment;
import com.dolpin.domain.comment.repository.CommentRepository;
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
@DisplayName("CommentCommandService 테스트")
class CommentCommandServiceTest {

    @InjectMocks
    private CommentCommandServiceImpl commentCommandService;

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

        Moment publicMoment = createPublicMoment();
        User user = createTestUser();
        Comment savedComment = createCommentWithId();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(user);
        given(commentRepository.save(any(Comment.class)))
                .willReturn(savedComment);

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

        then(commentRepository).should().save(any(Comment.class));
    }

    @Test
    @DisplayName("본인 비공개 기록에 댓글 작성 성공")
    void createComment_OwnPrivateMoment_Success() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
                CommentTestConstants.NEW_COMMENT_CONTENT,
                null
        );

        Moment privateMoment = createPrivateMoment(CommentTestConstants.TEST_USER_ID);
        User user = createTestUser();
        Comment savedComment = createCommentWithId();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(privateMoment));
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(user);
        given(commentRepository.save(any(Comment.class)))
                .willReturn(savedComment);

        // when
        CommentCreateResponse response = commentCommandService.createComment(
                CommentTestConstants.TEST_MOMENT_ID,
                request,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getId()).isEqualTo(CommentTestConstants.TEST_COMMENT_ID);
        assertThat(response.getContent()).isEqualTo(CommentTestConstants.NEW_COMMENT_CONTENT);
        assertThat(response.getIsOwner()).isEqualTo(CommentTestConstants.IS_OWNER);

        then(commentRepository).should().save(any(Comment.class));
    }

    @Test
    @DisplayName("대댓글 작성 성공")
    void createComment_WithParentComment_Success() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
                CommentTestConstants.REPLY_CONTENT,
                CommentTestConstants.PARENT_COMMENT_ID
        );

        Moment publicMoment = createPublicMoment();
        User user = createTestUser();
        Comment parentComment = createParentComment();
        Comment savedReplyComment = createReplyCommentWithId();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findValidParentComment(
                CommentTestConstants.PARENT_COMMENT_ID,
                CommentTestConstants.TEST_MOMENT_ID
        )).willReturn(Optional.of(parentComment));
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(user);
        given(commentRepository.save(any(Comment.class)))
                .willReturn(savedReplyComment);

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

        then(commentRepository).should().findValidParentComment(
                CommentTestConstants.PARENT_COMMENT_ID,
                CommentTestConstants.TEST_MOMENT_ID
        );
        then(commentRepository).should().save(any(Comment.class));
    }

    @Test
    @DisplayName("존재하지 않는 기록에 댓글 작성 시 예외")
    void createComment_MomentNotFound_ThrowsException() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
                CommentTestConstants.NEW_COMMENT_CONTENT,
                null
        );

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentCommandService.createComment(
                CommentTestConstants.TEST_MOMENT_ID,
                request,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommentTestConstants.MOMENT_NOT_FOUND_MESSAGE);

        then(commentRepository).should(never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("다른 사용자의 비공개 기록에 댓글 작성 시 예외")
    void createComment_OtherUserPrivateMoment_ThrowsException() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
                CommentTestConstants.NEW_COMMENT_CONTENT,
                null
        );

        Moment privateMoment = createPrivateMoment(CommentTestConstants.OTHER_USER_ID);

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(privateMoment));

        // when & then
        assertThatThrownBy(() -> commentCommandService.createComment(
                CommentTestConstants.TEST_MOMENT_ID,
                request,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommentTestConstants.PRIVATE_MOMENT_COMMENT_DENIED_MESSAGE);

        then(commentRepository).should(never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("유효하지 않은 부모 댓글로 대댓글 작성 시 예외")
    void createComment_InvalidParentComment_ThrowsException() {
        // given
        CommentCreateRequest request = new CommentCreateRequest(
                CommentTestConstants.REPLY_CONTENT,
                CommentTestConstants.INVALID_PARENT_COMMENT_ID
        );

        Moment publicMoment = createPublicMoment();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findValidParentComment(
                CommentTestConstants.INVALID_PARENT_COMMENT_ID,
                CommentTestConstants.TEST_MOMENT_ID
        )).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentCommandService.createComment(
                CommentTestConstants.TEST_MOMENT_ID,
                request,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommentTestConstants.INVALID_PARENT_COMMENT_MESSAGE);

        then(commentRepository).should(never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_Success() {
        // given
        Moment moment = createPublicMoment();
        Comment comment = createCommentWithId();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(moment));
        given(commentRepository.findByIdAndMomentIdAndNotDeleted(
                CommentTestConstants.TEST_COMMENT_ID,
                CommentTestConstants.TEST_MOMENT_ID
        )).willReturn(Optional.of(comment));
        given(commentRepository.save(comment)).willReturn(comment);

        // when
        commentCommandService.deleteComment(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.TEST_COMMENT_ID,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        then(commentRepository).should().save(comment);
        assertThat(comment.isDeleted()).isEqualTo(CommentTestConstants.IS_DELETED);
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 시 예외")
    void deleteComment_CommentNotFound_ThrowsException() {
        // given
        Moment moment = createPublicMoment();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(moment));
        given(commentRepository.findByIdAndMomentIdAndNotDeleted(
                CommentTestConstants.NON_EXISTENT_COMMENT_ID,
                CommentTestConstants.TEST_MOMENT_ID
        )).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentCommandService.deleteComment(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.NON_EXISTENT_COMMENT_ID,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommentTestConstants.COMMENT_NOT_FOUND_MESSAGE);

        then(commentRepository).should(never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("다른 사용자의 댓글 삭제 시 예외")
    void deleteComment_NotOwner_ThrowsException() {
        // given
        Moment moment = createPublicMoment();
        Comment otherUserComment = createOtherUserComment();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(moment));
        given(commentRepository.findByIdAndMomentIdAndNotDeleted(
                CommentTestConstants.TEST_COMMENT_ID,
                CommentTestConstants.TEST_MOMENT_ID
        )).willReturn(Optional.of(otherUserComment));

        // when & then
        assertThatThrownBy(() -> commentCommandService.deleteComment(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.TEST_COMMENT_ID,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommentTestConstants.DELETE_PERMISSION_DENIED_MESSAGE);

        then(commentRepository).should(never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("존재하지 않는 기록의 댓글 삭제 시 예외")
    void deleteComment_MomentNotFound_ThrowsException() {
        // given
        given(momentRepository.findBasicMomentById(CommentTestConstants.DELETED_MOMENT_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentCommandService.deleteComment(
                CommentTestConstants.DELETED_MOMENT_ID,
                CommentTestConstants.TEST_COMMENT_ID,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommentTestConstants.MOMENT_NOT_FOUND_MESSAGE);

        then(commentRepository).should(never()).findByIdAndMomentIdAndNotDeleted(any(), any());
        then(commentRepository).should(never()).save(any(Comment.class));
    }

    // Helper methods
    private Moment createPublicMoment() {
        return Moment.builder()
                .id(CommentTestConstants.TEST_MOMENT_ID)
                .userId(CommentTestConstants.OTHER_USER_ID)
                .title(CommentTestConstants.PUBLIC_MOMENT_TITLE)
                .content(CommentTestConstants.MOMENT_CONTENT)
                .isPublic(CommentTestConstants.IS_PUBLIC)
                .viewCount(CommentTestConstants.DEFAULT_VIEW_COUNT)
                .build();
    }

    private Moment createPrivateMoment(Long userId) {
        return Moment.builder()
                .id(CommentTestConstants.TEST_MOMENT_ID)
                .userId(userId)
                .title(CommentTestConstants.PRIVATE_MOMENT_TITLE)
                .content(CommentTestConstants.PRIVATE_MOMENT_CONTENT)
                .isPublic(CommentTestConstants.IS_PRIVATE)
                .viewCount(CommentTestConstants.DEFAULT_VIEW_COUNT)
                .build();
    }

    private User createTestUser() {
        return User.builder()
                .id(CommentTestConstants.TEST_USER_ID)
                .username(CommentTestConstants.TEST_USERNAME)
                .imageUrl(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();
    }

    private Comment createCommentWithId() {
        return Comment.builder()
                .id(CommentTestConstants.TEST_COMMENT_ID)
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.NEW_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Comment createParentComment() {
        return Comment.builder()
                .id(CommentTestConstants.PARENT_COMMENT_ID)
                .userId(CommentTestConstants.OTHER_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.PARENT_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Comment createReplyCommentWithId() {
        Comment parentComment = createParentComment();
        return Comment.builder()
                .id(CommentTestConstants.REPLY_COMMENT_ID)
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.REPLY_CONTENT)
                .parentComment(parentComment)
                .depth(CommentTestConstants.REPLY_COMMENT_DEPTH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Comment createOtherUserComment() {
        return Comment.builder()
                .id(CommentTestConstants.TEST_COMMENT_ID)
                .userId(CommentTestConstants.OTHER_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.OTHER_USER_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
