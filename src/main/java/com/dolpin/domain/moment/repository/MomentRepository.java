package com.dolpin.domain.moment.repository;

import com.dolpin.domain.moment.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MomentRepository extends JpaRepository<Moment, Long> {

    // 기본 Moment 정보만 조회 (이미지 없이)
    @Query("SELECT m FROM Moment m WHERE m.id = :id")
    Optional<Moment> findBasicMomentById(@Param("id") Long id);

    // Moment + 이미지 정보 함께 조회
    @Query("SELECT m FROM Moment m " +
            "LEFT JOIN FETCH m.images " +
            "WHERE m.id = :id")
    Optional<Moment> findByIdWithImages(@Param("id") Long id);

    // 네이티브 쿼리로 공개 + 사용자 기록 조회 (커서 기반)
    @Query(value = "SELECT * FROM moment m " +
            "WHERE (m.is_public = true OR m.user_id = :currentUserId) " +
            "AND (:cursor IS NULL OR m.created_at < CAST(:cursor AS timestamp)) " +
            "ORDER BY m.created_at DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Moment> findPublicMomentsWithUserPrivateNative(@Param("currentUserId") Long currentUserId,
                                                        @Param("cursor") String cursor,
                                                        @Param("limit") int limit);

    // 네이티브 쿼리로 사용자 기록 조회 (커서 기반)
    @Query(value = "SELECT * FROM moment m " +
            "WHERE m.user_id = :userId " +
            "AND (:includePrivate = true OR m.is_public = true) " +
            "AND (:cursor IS NULL OR m.created_at < CAST(:cursor AS timestamp)) " +
            "ORDER BY m.created_at DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Moment> findByUserIdWithVisibilityNative(@Param("userId") Long userId,
                                                  @Param("includePrivate") boolean includePrivate,
                                                  @Param("cursor") String cursor,
                                                  @Param("limit") int limit);

    // 특정 장소의 공개 Moment 목록 조회 (기존 유지)
    @Query("SELECT m FROM Moment m " +
            "WHERE m.placeId = :placeId " +
            "AND m.isPublic = true " +
            "ORDER BY m.createdAt DESC")
    Page<Moment> findPublicMomentsByPlaceId(@Param("placeId") Long placeId, Pageable pageable);

    // 나머지 메서드들은 기존과 동일...
    @Modifying
    @Query("UPDATE Moment m SET m.viewCount = m.viewCount + 1 WHERE m.id = :momentId")
    int incrementViewCount(@Param("momentId") Long momentId);

    @Query("SELECT COUNT(m) FROM Moment m " +
            "WHERE m.userId = :userId " +
            "AND (:includePrivate = true OR m.isPublic = true)")
    long countByUserIdWithVisibility(@Param("userId") Long userId,
                                     @Param("includePrivate") boolean includePrivate);

    @Query("SELECT COUNT(m) FROM Moment m " +
            "WHERE m.placeId = :placeId " +
            "AND m.isPublic = true")
    long countPublicMomentsByPlaceId(@Param("placeId") Long placeId);

    @Query("SELECT m.userId FROM Moment m WHERE m.id = :momentId")
    Optional<Long> findUserIdByMomentId(@Param("momentId") Long momentId);

    @Query("SELECT m FROM Moment m " +
            "WHERE m.isPublic = true " +
            "AND m.updatedAt >= :since " +
            "ORDER BY m.updatedAt DESC")
    List<Moment> findRecentPublicMoments(@Param("since") java.time.LocalDateTime since, Pageable pageable);

    @Query("SELECT m FROM Moment m " +
            "WHERE m.userId IN :userIds " +
            "AND m.isPublic = true " +
            "ORDER BY m.createdAt DESC")
    Page<Moment> findMomentsByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);

    @Query("SELECT m.placeId, COUNT(m) FROM Moment m " +
            "WHERE m.placeId IN :placeIds " +
            "AND m.isPublic = true " +
            "GROUP BY m.placeId")
    List<Object[]> countPublicMomentsByPlaceIds(@Param("placeIds") List<Long> placeIds);

    @Query("SELECT COUNT(m) FROM Moment m " +
            "WHERE m.userId = :userId " +
            "AND m.createdAt >= :startDate " +
            "AND m.createdAt < :endDate")
    long countByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
}
