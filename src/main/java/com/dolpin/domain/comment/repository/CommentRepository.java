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

    // 특정 기록의 댓글 목록 조회 (첫 페이지용)
    @Query("SELECT c FROM Comment c " +
            "WHERE c.momentId = :momentId " +
            "AND c.deletedAt IS NULL " +
            "ORDER BY c.createdAt DESC")
    Page<Comment> findByMomentIdAndNotDeleted(@Param("momentId") Long momentId, Pageable pageable);

    // 네이티브 쿼리로 커서 기반 페이지네이션
    @Query(value = "SELECT * FROM comment c " +
            "WHERE c.moment_id = :momentId " +
            "AND c.deleted_at IS NULL " +
            "AND (:cursor IS NULL OR c.created_at < CAST(:cursor AS timestamp)) " +
            "ORDER BY c.created_at DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Comment> findByMomentIdAndNotDeletedWithCursorNative(
            @Param("momentId") Long momentId,
            @Param("cursor") String cursor,
            @Param("limit") int limit);

    // 특정 기록의 댓글 개수
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.momentId = :momentId " +
            "AND c.deletedAt IS NULL")
    long countByMomentIdAndNotDeleted(@Param("momentId") Long momentId);

    // 특정 사용자의 댓글 개수
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.userId = :userId " +
            "AND c.deletedAt IS NULL")
    long countByUserIdAndNotDeleted(@Param("userId") Long userId);

    // 댓글 ID와 기록 ID로 댓글 조회
    @Query("SELECT c FROM Comment c " +
            "WHERE c.id = :commentId " +
            "AND c.momentId = :momentId " +
            "AND c.deletedAt IS NULL")
    Optional<Comment> findByIdAndMomentIdAndNotDeleted(
            @Param("commentId") Long commentId,
            @Param("momentId") Long momentId);

    // 특정 기록의 최근 댓글들
    @Query("SELECT c FROM Comment c " +
            "WHERE c.momentId = :momentId " +
            "AND c.deletedAt IS NULL " +
            "AND c.createdAt >= :since " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findRecentCommentsByMomentId(
            @Param("momentId") Long momentId,
            @Param("since") LocalDateTime since);

    // 사용자가 작성한 댓글 목록
    @Query("SELECT c FROM Comment c " +
            "WHERE c.userId = :userId " +
            "AND c.deletedAt IS NULL " +
            "ORDER BY c.createdAt DESC")
    Page<Comment> findByUserIdAndNotDeleted(@Param("userId") Long userId, Pageable pageable);

    // 여러 기록 댓글 갯수 조회
    @Query("SELECT c.momentId, COUNT(c) FROM Comment c " +
            "WHERE c.momentId IN :momentIds " +
            "AND c.deletedAt IS NULL " +
            "GROUP BY c.momentId")
    List<Object[]> countByMomentIds(@Param("momentIds") List<Long> momentIds);
}
