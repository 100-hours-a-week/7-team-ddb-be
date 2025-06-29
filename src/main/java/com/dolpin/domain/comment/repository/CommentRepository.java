package com.dolpin.domain.comment.repository;

import com.dolpin.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 스레드 기반 정렬 - 첫 페이지용 (네이티브 쿼리)
    @Query(value = "SELECT * FROM comment c " +
            "WHERE c.moment_id = :momentId " +
            "AND c.deleted_at IS NULL " +
            "ORDER BY " +
            "CASE WHEN c.parent_comment_id IS NULL THEN c.created_at " +
            "     ELSE (SELECT parent.created_at FROM comment parent WHERE parent.id = c.parent_comment_id) " +
            "END ASC, " +
            "c.parent_comment_id ASC NULLS FIRST, " +
            "c.created_at ASC " +
            "LIMIT :limit OFFSET :offset",
            nativeQuery = true)
    List<Comment> findByMomentIdAndNotDeletedWithPagination(
            @Param("momentId") Long momentId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    // 스레드 기반 정렬 - 커서 기반 페이지네이션
    @Query(value = "SELECT * FROM comment c " +
            "WHERE c.moment_id = :momentId " +
            "AND c.deleted_at IS NULL " +
            "AND (:cursor IS NULL OR " +
            "     CASE WHEN c.parent_comment_id IS NULL " +
            "          THEN c.created_at > CAST(:cursor AS timestamp) " +
            "          ELSE (SELECT parent.created_at FROM comment parent WHERE parent.id = c.parent_comment_id) > CAST(:cursor AS timestamp) " +
            "     END) " +
            "ORDER BY " +
            "CASE WHEN c.parent_comment_id IS NULL THEN c.created_at " +
            "     ELSE (SELECT parent.created_at FROM comment parent WHERE parent.id = c.parent_comment_id) " +
            "END ASC, " +
            "c.parent_comment_id ASC NULLS FIRST, " +
            "c.created_at ASC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Comment> findByMomentIdAndNotDeletedWithCursorNative(
            @Param("momentId") Long momentId,
            @Param("cursor") String cursor,
            @Param("limit") int limit);

    // 나머지 메서드들은 그대로 유지
    @Query("SELECT c FROM Comment c " +
            "WHERE c.id = :commentId " +
            "AND c.momentId = :momentId " +
            "AND c.deletedAt IS NULL")
    Optional<Comment> findValidParentComment(@Param("commentId") Long commentId, @Param("momentId") Long momentId);

    @Query("SELECT c FROM Comment c " +
            "WHERE c.id = :commentId " +
            "AND c.momentId = :momentId " +
            "AND c.deletedAt IS NULL")
    Optional<Comment> findByIdAndMomentIdAndNotDeleted(
            @Param("commentId") Long commentId,
            @Param("momentId") Long momentId);

    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.momentId = :momentId " +
            "AND c.deletedAt IS NULL")
    long countByMomentIdAndNotDeleted(@Param("momentId") Long momentId);

    @Query("SELECT c.momentId, COUNT(c) FROM Comment c " +
            "WHERE c.momentId IN :momentIds " +
            "AND c.deletedAt IS NULL " +
            "GROUP BY c.momentId")
    List<Object[]> countByMomentIds(@Param("momentIds") List<Long> momentIds);
}
