package com.dolpin.domain.comment.service.query;

import com.dolpin.domain.comment.dto.response.CommentListResponse;
import com.dolpin.domain.comment.entity.Comment;
import com.dolpin.domain.comment.repository.CommentRepository;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.repository.MomentRepository;
import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.user.service.UserQueryService;
import com.dolpin.global.constants.CommentTestConstants;
import com.dolpin.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentQueryService 테스트")
class CommentQueryServiceImplTest {

    @InjectMocks
    private CommentQueryServiceImpl commentQueryService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private UserQueryService userQueryService;

    @Test
    @DisplayName("댓글 목록 조회 성공 - 첫 페이지")
    void getCommentsByMomentId_FirstPage_Success() {
        // given
        Moment publicMoment = createPublicMoment();
        List<Comment> comments = createCommentList();
        List<User> users = createUserList();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findByMomentIdAndNotDeletedWithPagination(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT + 1,
                CommentTestConstants.TEST_OFFSET
        )).willReturn(comments);
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(users.get(0));
        given(userQueryService.getUserById(CommentTestConstants.OTHER_USER_ID))
                .willReturn(users.get(1));

        // when
        CommentListResponse response = commentQueryService.getCommentsByMomentId(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT,
                null,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getComments()).hasSize(CommentTestConstants.COMMENT_LIST_SIZE);
        assertThat(response.getMeta().getPagination().getLimit()).isEqualTo(CommentTestConstants.DEFAULT_PAGE_LIMIT);
        assertThat(response.getMeta().getPagination().getHasNext()).isEqualTo(CommentTestConstants.NO_NEXT);
        assertThat(response.getMeta().getPagination().getNextCursor()).isNull();

        // 첫 번째 댓글 검증
        CommentListResponse.CommentDto firstComment = response.getComments().get(0);
        assertThat(firstComment.getId()).isEqualTo(CommentTestConstants.TEST_COMMENT_ID);
        assertThat(firstComment.getContent()).isEqualTo(CommentTestConstants.TEST_COMMENT_CONTENT);
        assertThat(firstComment.getDepth()).isEqualTo(CommentTestConstants.ROOT_COMMENT_DEPTH);
        assertThat(firstComment.getIsOwner()).isEqualTo(CommentTestConstants.IS_OWNER);

        then(commentRepository).should().findByMomentIdAndNotDeletedWithPagination(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT + 1,
                CommentTestConstants.TEST_OFFSET
        );
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 커서 기반 페이지네이션")
    void getCommentsByMomentId_WithCursor_Success() {
        // given
        Moment publicMoment = createPublicMoment();
        List<Comment> comments = createCommentList();
        List<User> users = createUserList();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findByMomentIdAndNotDeletedWithCursorNative(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.TEST_CURSOR,
                CommentTestConstants.DEFAULT_PAGE_LIMIT + 1
        )).willReturn(comments);
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(users.get(0));
        given(userQueryService.getUserById(CommentTestConstants.OTHER_USER_ID))
                .willReturn(users.get(1));

        // when
        CommentListResponse response = commentQueryService.getCommentsByMomentId(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT,
                CommentTestConstants.TEST_CURSOR,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getComments()).hasSize(CommentTestConstants.COMMENT_LIST_SIZE);
        assertThat(response.getMeta().getPagination().getLimit()).isEqualTo(CommentTestConstants.DEFAULT_PAGE_LIMIT);

        then(commentRepository).should().findByMomentIdAndNotDeletedWithCursorNative(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.TEST_CURSOR,
                CommentTestConstants.DEFAULT_PAGE_LIMIT + 1
        );
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - hasNext가 true인 경우")
    void getCommentsByMomentId_HasNext_Success() {
        // given
        Moment publicMoment = createPublicMoment();
        List<Comment> commentsWithNext = createCommentListWithNext();
        List<User> users = createUserList();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findByMomentIdAndNotDeletedWithPagination(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.CUSTOM_PAGE_LIMIT + 1,
                CommentTestConstants.TEST_OFFSET
        )).willReturn(commentsWithNext);
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(users.get(0));
        given(userQueryService.getUserById(CommentTestConstants.OTHER_USER_ID))
                .willReturn(users.get(1));

        // when
        CommentListResponse response = commentQueryService.getCommentsByMomentId(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.CUSTOM_PAGE_LIMIT,
                null,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getComments()).hasSize(CommentTestConstants.CUSTOM_PAGE_LIMIT);
        assertThat(response.getMeta().getPagination().getHasNext()).isEqualTo(CommentTestConstants.HAS_NEXT);
        assertThat(response.getMeta().getPagination().getNextCursor()).isNotNull();
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 빈 목록")
    void getCommentsByMomentId_EmptyList_Success() {
        // given
        Moment publicMoment = createPublicMoment();
        List<Comment> emptyComments = List.of();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findByMomentIdAndNotDeletedWithPagination(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT + 1,
                CommentTestConstants.TEST_OFFSET
        )).willReturn(emptyComments);

        // when
        CommentListResponse response = commentQueryService.getCommentsByMomentId(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT,
                null,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getComments()).hasSize(CommentTestConstants.EMPTY_COMMENT_LIST_SIZE);
        assertThat(response.getMeta().getPagination().getHasNext()).isEqualTo(CommentTestConstants.NO_NEXT);
        assertThat(response.getMeta().getPagination().getNextCursor()).isNull();
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 대댓글 포함")
    void getCommentsByMomentId_WithReplies_Success() {
        // given
        Moment publicMoment = createPublicMoment();
        List<Comment> commentsWithReplies = createCommentsWithReplies();
        List<User> users = createUserList();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findByMomentIdAndNotDeletedWithPagination(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT + 1,
                CommentTestConstants.TEST_OFFSET
        )).willReturn(commentsWithReplies);
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(users.get(0));
        given(userQueryService.getUserById(CommentTestConstants.OTHER_USER_ID))
                .willReturn(users.get(1));

        // when
        CommentListResponse response = commentQueryService.getCommentsByMomentId(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT,
                null,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getComments()).hasSize(2);

        // 부모 댓글 검증
        CommentListResponse.CommentDto parentComment = response.getComments().get(0);
        assertThat(parentComment.getId()).isEqualTo(CommentTestConstants.PARENT_COMMENT_ID);
        assertThat(parentComment.getDepth()).isEqualTo(CommentTestConstants.ROOT_COMMENT_DEPTH);
        assertThat(parentComment.getParentCommentId()).isNull();

        // 대댓글 검증
        CommentListResponse.CommentDto replyComment = response.getComments().get(1);
        assertThat(replyComment.getId()).isEqualTo(CommentTestConstants.REPLY_COMMENT_ID);
        assertThat(replyComment.getDepth()).isEqualTo(CommentTestConstants.REPLY_COMMENT_DEPTH);
        assertThat(replyComment.getParentCommentId()).isEqualTo(CommentTestConstants.PARENT_COMMENT_ID);
    }

    @Test
    @DisplayName("존재하지 않는 기록의 댓글 조회 시 예외")
    void getCommentsByMomentId_MomentNotFound_ThrowsException() {
        // given
        given(momentRepository.findBasicMomentById(CommentTestConstants.DELETED_MOMENT_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentQueryService.getCommentsByMomentId(
                CommentTestConstants.DELETED_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT,
                null,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommentTestConstants.MOMENT_NOT_FOUND_MESSAGE);
    }

    @Test
    @DisplayName("비공개 기록의 댓글 조회 시 접근 권한 없음 예외")
    void getCommentsByMomentId_PrivateMomentAccessDenied_ThrowsException() {
        // given
        Moment privateMoment = createPrivateMoment();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(privateMoment));

        // when & then
        assertThatThrownBy(() -> commentQueryService.getCommentsByMomentId(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT,
                null,
                CommentTestConstants.TEST_USER_ID
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(CommentTestConstants.ACCESS_DENIED_MESSAGE);
    }

    @Test
    @DisplayName("limit 값 검증 - null인 경우 기본값 사용")
    void getCommentsByMomentId_NullLimit_UsesDefault() {
        // given
        Moment publicMoment = createPublicMoment();
        List<Comment> comments = createCommentList();
        List<User> users = createUserList();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findByMomentIdAndNotDeletedWithPagination(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT + 1,
                CommentTestConstants.TEST_OFFSET
        )).willReturn(comments);
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(users.get(0));
        given(userQueryService.getUserById(CommentTestConstants.OTHER_USER_ID))
                .willReturn(users.get(1));

        // when
        CommentListResponse response = commentQueryService.getCommentsByMomentId(
                CommentTestConstants.TEST_MOMENT_ID,
                null,
                null,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getMeta().getPagination().getLimit()).isEqualTo(CommentTestConstants.DEFAULT_PAGE_LIMIT);
    }

    @Test
    @DisplayName("limit 값 검증 - 최대값 초과 시 최대값 사용")
    void getCommentsByMomentId_OverMaxLimit_UsesMaxLimit() {
        // given
        Moment publicMoment = createPublicMoment();
        List<Comment> comments = createCommentList();
        List<User> users = createUserList();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findByMomentIdAndNotDeletedWithPagination(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.MAX_PAGE_LIMIT + 1,
                CommentTestConstants.TEST_OFFSET
        )).willReturn(comments);
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(users.get(0));
        given(userQueryService.getUserById(CommentTestConstants.OTHER_USER_ID))
                .willReturn(users.get(1));

        // when
        CommentListResponse response = commentQueryService.getCommentsByMomentId(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.OVER_MAX_LIMIT,
                null,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getMeta().getPagination().getLimit()).isEqualTo(CommentTestConstants.MAX_PAGE_LIMIT);
    }

    @Test
    @DisplayName("limit 값 검증 - 0 이하 값 시 기본값 사용")
    void getCommentsByMomentId_ZeroOrNegativeLimit_UsesDefault() {
        // given
        Moment publicMoment = createPublicMoment();
        List<Comment> comments = createCommentList();
        List<User> users = createUserList();

        given(momentRepository.findBasicMomentById(CommentTestConstants.TEST_MOMENT_ID))
                .willReturn(Optional.of(publicMoment));
        given(commentRepository.findByMomentIdAndNotDeletedWithPagination(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.DEFAULT_PAGE_LIMIT + 1,
                CommentTestConstants.TEST_OFFSET
        )).willReturn(comments);
        given(userQueryService.getUserById(CommentTestConstants.TEST_USER_ID))
                .willReturn(users.get(0));
        given(userQueryService.getUserById(CommentTestConstants.OTHER_USER_ID))
                .willReturn(users.get(1));

        // when
        CommentListResponse response = commentQueryService.getCommentsByMomentId(
                CommentTestConstants.TEST_MOMENT_ID,
                CommentTestConstants.NEGATIVE_LIMIT,
                null,
                CommentTestConstants.TEST_USER_ID
        );

        // then
        assertThat(response.getMeta().getPagination().getLimit()).isEqualTo(CommentTestConstants.DEFAULT_PAGE_LIMIT);
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

    private Moment createPrivateMoment() {
        return Moment.builder()
                .id(CommentTestConstants.TEST_MOMENT_ID)
                .userId(CommentTestConstants.OTHER_USER_ID)
                .title(CommentTestConstants.PRIVATE_MOMENT_TITLE)
                .content(CommentTestConstants.PRIVATE_MOMENT_CONTENT)
                .isPublic(CommentTestConstants.IS_PRIVATE)
                .viewCount(CommentTestConstants.DEFAULT_VIEW_COUNT)
                .build();
    }

    private List<User> createUserList() {
        User testUser = User.builder()
                .id(CommentTestConstants.TEST_USER_ID)
                .username(CommentTestConstants.TEST_USERNAME)
                .imageUrl(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();

        User otherUser = User.builder()
                .id(CommentTestConstants.OTHER_USER_ID)
                .username(CommentTestConstants.OTHER_USERNAME)
                .imageUrl(CommentTestConstants.TEST_PROFILE_IMAGE_URL)
                .build();

        return List.of(testUser, otherUser);
    }

    private List<Comment> createCommentList() {
        LocalDateTime now = LocalDateTime.now();

        return List.of(
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID)
                        .userId(CommentTestConstants.TEST_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(10))
                        .updatedAt(now.minusMinutes(10))
                        .build(),
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID + 1)
                        .userId(CommentTestConstants.OTHER_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.OTHER_USER_COMMENT_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(8))
                        .updatedAt(now.minusMinutes(8))
                        .build(),
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID + 2)
                        .userId(CommentTestConstants.TEST_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.NEW_COMMENT_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(6))
                        .updatedAt(now.minusMinutes(6))
                        .build(),
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID + 3)
                        .userId(CommentTestConstants.OTHER_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.UPDATED_COMMENT_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(4))
                        .updatedAt(now.minusMinutes(4))
                        .build(),
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID + 4)
                        .userId(CommentTestConstants.TEST_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.REPLY_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(2))
                        .updatedAt(now.minusMinutes(2))
                        .build()
        );
    }

    private List<Comment> createCommentListWithNext() {
        LocalDateTime now = LocalDateTime.now();

        return List.of(
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID)
                        .userId(CommentTestConstants.TEST_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(10))
                        .updatedAt(now.minusMinutes(10))
                        .build(),
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID + 1)
                        .userId(CommentTestConstants.OTHER_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.OTHER_USER_COMMENT_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(8))
                        .updatedAt(now.minusMinutes(8))
                        .build(),
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID + 2)
                        .userId(CommentTestConstants.TEST_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.NEW_COMMENT_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(6))
                        .updatedAt(now.minusMinutes(6))
                        .build(),
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID + 3)
                        .userId(CommentTestConstants.OTHER_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.UPDATED_COMMENT_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(4))
                        .updatedAt(now.minusMinutes(4))
                        .build(),
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID + 4)
                        .userId(CommentTestConstants.TEST_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.REPLY_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(2))
                        .updatedAt(now.minusMinutes(2))
                        .build(),
                Comment.builder()
                        .id(CommentTestConstants.TEST_COMMENT_ID + 5)
                        .userId(CommentTestConstants.OTHER_USER_ID)
                        .momentId(CommentTestConstants.TEST_MOMENT_ID)
                        .content(CommentTestConstants.PARENT_COMMENT_CONTENT)
                        .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                        .createdAt(now.minusMinutes(1))
                        .updatedAt(now.minusMinutes(1))
                        .build()
        );
    }

    private List<Comment> createCommentsWithReplies() {
        LocalDateTime now = LocalDateTime.now();

        Comment parentComment = Comment.builder()
                .id(CommentTestConstants.PARENT_COMMENT_ID)
                .userId(CommentTestConstants.OTHER_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.PARENT_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .createdAt(now.minusMinutes(10))
                .updatedAt(now.minusMinutes(10))
                .build();

        Comment replyComment = Comment.builder()
                .id(CommentTestConstants.REPLY_COMMENT_ID)
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.REPLY_CONTENT)
                .parentComment(parentComment)
                .depth(CommentTestConstants.REPLY_COMMENT_DEPTH)
                .createdAt(now.minusMinutes(5))
                .updatedAt(now.minusMinutes(5))
                .build();

        return List.of(parentComment, replyComment);
    }
}
