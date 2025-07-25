package com.dolpin.domain.comment.entity;

import com.dolpin.global.constants.CommentTestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Comment 엔티티 테스트")
class CommentTest {

    @Test
    @DisplayName("일반 댓글 생성 테스트")
    void createComment_Success() {
        // given & when
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        // then
        assertThat(comment.getUserId()).isEqualTo(CommentTestConstants.TEST_USER_ID);
        assertThat(comment.getMomentId()).isEqualTo(CommentTestConstants.TEST_MOMENT_ID);
        assertThat(comment.getContent()).isEqualTo(CommentTestConstants.TEST_COMMENT_CONTENT);
        assertThat(comment.getDepth()).isEqualTo(CommentTestConstants.ROOT_COMMENT_DEPTH);
        assertThat(comment.getParentComment()).isNull();
        assertThat(comment.isDeleted()).isEqualTo(CommentTestConstants.IS_NOT_DELETED);
    }

    @Test
    @DisplayName("대댓글 생성 테스트")
    void createReplyComment_Success() {
        // given
        Comment parentComment = Comment.builder()
                .id(CommentTestConstants.PARENT_COMMENT_ID)
                .userId(CommentTestConstants.OTHER_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.PARENT_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        // when
        Comment replyComment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.REPLY_CONTENT)
                .parentComment(parentComment)
                .depth(CommentTestConstants.REPLY_COMMENT_DEPTH)
                .build();

        // then
        assertThat(replyComment.getUserId()).isEqualTo(CommentTestConstants.TEST_USER_ID);
        assertThat(replyComment.getMomentId()).isEqualTo(CommentTestConstants.TEST_MOMENT_ID);
        assertThat(replyComment.getContent()).isEqualTo(CommentTestConstants.REPLY_CONTENT);
        assertThat(replyComment.getDepth()).isEqualTo(CommentTestConstants.REPLY_COMMENT_DEPTH);
        assertThat(replyComment.getParentComment()).isEqualTo(parentComment);
        assertThat(replyComment.isReply()).isEqualTo(CommentTestConstants.IS_REPLY);
    }

    @Test
    @DisplayName("댓글 소프트 삭제 테스트")
    void softDeleteComment_Success() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        // when
        comment.softDelete();

        // then
        assertThat(comment.isDeleted()).isEqualTo(CommentTestConstants.IS_DELETED);
        assertThat(comment.getDeletedAt()).isNotNull();
        assertThat(comment.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("댓글 소유자 확인 테스트 - 소유자")
    void isOwnedBy_Owner_ReturnsTrue() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        // when & then
        assertThat(comment.isOwnedBy(CommentTestConstants.TEST_USER_ID)).isEqualTo(CommentTestConstants.IS_OWNER);
    }

    @Test
    @DisplayName("댓글 소유자 확인 테스트 - 비소유자")
    void isOwnedBy_NotOwner_ReturnsFalse() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        // when & then
        assertThat(comment.isOwnedBy(CommentTestConstants.OTHER_USER_ID)).isEqualTo(CommentTestConstants.IS_NOT_OWNER);
    }

    @Test
    @DisplayName("댓글 조회 권한 확인 테스트 - 정상 댓글")
    void canBeViewedBy_NotDeleted_ReturnsTrue() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        // when & then
        assertThat(comment.canBeViewedBy(CommentTestConstants.TEST_USER_ID)).isTrue();
        assertThat(comment.canBeViewedBy(CommentTestConstants.OTHER_USER_ID)).isTrue();
    }

    @Test
    @DisplayName("댓글 조회 권한 확인 테스트 - 삭제된 댓글")
    void canBeViewedBy_Deleted_ReturnsFalse() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();
        comment.softDelete();

        // when & then
        assertThat(comment.canBeViewedBy(CommentTestConstants.TEST_USER_ID)).isFalse();
        assertThat(comment.canBeViewedBy(CommentTestConstants.OTHER_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("댓글 삭제 권한 확인 테스트 - 소유자")
    void canBeDeletedBy_Owner_ReturnsTrue() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        // when & then
        assertThat(comment.canBeDeletedBy(CommentTestConstants.TEST_USER_ID)).isTrue();
    }

    @Test
    @DisplayName("댓글 삭제 권한 확인 테스트 - 비소유자")
    void canBeDeletedBy_NotOwner_ReturnsFalse() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        // when & then
        assertThat(comment.canBeDeletedBy(CommentTestConstants.OTHER_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("댓글 삭제 권한 확인 테스트 - 이미 삭제된 댓글")
    void canBeDeletedBy_AlreadyDeleted_ReturnsFalse() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();
        comment.softDelete();

        // when & then
        assertThat(comment.canBeDeletedBy(CommentTestConstants.TEST_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("대댓글 여부 확인 테스트 - 일반 댓글")
    void isReply_RootComment_ReturnsFalse() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        // when & then
        assertThat(comment.isReply()).isEqualTo(CommentTestConstants.IS_NOT_REPLY);
    }

    @Test
    @DisplayName("대댓글 여부 확인 테스트 - 대댓글")
    void isReply_ReplyComment_ReturnsTrue() {
        // given
        Comment parentComment = Comment.builder()
                .id(CommentTestConstants.PARENT_COMMENT_ID)
                .userId(CommentTestConstants.OTHER_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.PARENT_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        Comment replyComment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.REPLY_CONTENT)
                .parentComment(parentComment)
                .depth(CommentTestConstants.REPLY_COMMENT_DEPTH)
                .build();

        // when & then
        assertThat(replyComment.isReply()).isEqualTo(CommentTestConstants.IS_REPLY);
    }

    @Test
    @DisplayName("Comment PrePersist 테스트")
    void prePersist_SetsTimestamps() {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        LocalDateTime beforePersist = LocalDateTime.now();

        // when
        comment.prePersist();

        // then
        LocalDateTime afterPersist = LocalDateTime.now();
        assertThat(comment.getCreatedAt()).isBetween(beforePersist, afterPersist);
        assertThat(comment.getUpdatedAt()).isBetween(beforePersist, afterPersist);
        assertThat(comment.getCreatedAt()).isEqualTo(comment.getUpdatedAt());
    }

    @Test
    @DisplayName("Comment PreUpdate 테스트")
    void preUpdate_UpdatesTimestamp() throws InterruptedException {
        // given
        Comment comment = Comment.builder()
                .userId(CommentTestConstants.TEST_USER_ID)
                .momentId(CommentTestConstants.TEST_MOMENT_ID)
                .content(CommentTestConstants.TEST_COMMENT_CONTENT)
                .depth(CommentTestConstants.ROOT_COMMENT_DEPTH)
                .build();

        comment.prePersist();
        LocalDateTime originalUpdatedAt = comment.getUpdatedAt();

        Thread.sleep(1);

        // when
        comment.preUpdate();

        // then
        assertThat(comment.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(comment.getUpdatedAt()).isAfter(comment.getCreatedAt());
    }
}
