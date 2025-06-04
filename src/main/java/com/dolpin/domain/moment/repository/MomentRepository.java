package com.dolpin.domain.moment.repository;

import com.dolpin.domain.moment.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // 특정 사용자의 Moment 목록 조회 (공개/비공개 구분)
    @Query("SELECT m FROM Moment m " +
            "WHERE m.userId = :userId " +
            "AND (:includePrivate = true OR m.isPublic = true) " +
            "ORDER BY m.createdAt DESC")
    Page<Moment> findByUserIdWithVisibility(@Param("userId") Long userId,
                                            @Param("includePrivate") boolean includePrivate,
                                            Pageable pageable);

    // 특정 장소의 공개 Moment 목록 조회
    @Query("SELECT m FROM Moment m " +
            "WHERE m.placeId = :placeId " +
            "AND m.isPublic = true " +
            "ORDER BY m.createdAt DESC")
    Page<Moment> findPublicMomentsByPlaceId(@Param("placeId") Long placeId, Pageable pageable);

    // 공개 Moment 전체 목록 조회 (피드용)
    @Query("SELECT m FROM Moment m " +
            "WHERE m.isPublic = true " +
            "ORDER BY m.createdAt DESC")
    Page<Moment> findPublicMoments(Pageable pageable);

    // 특정 사용자의 Moment 개수
    @Query("SELECT COUNT(m) FROM Moment m " +
            "WHERE m.userId = :userId " +
            "AND (:includePrivate = true OR m.isPublic = true)")
    long countByUserIdWithVisibility(@Param("userId") Long userId,
                                     @Param("includePrivate") boolean includePrivate);

    // 특정 장소의 공개 Moment 개수
    @Query("SELECT COUNT(m) FROM Moment m " +
            "WHERE m.placeId = :placeId " +
            "AND m.isPublic = true")
    long countPublicMomentsByPlaceId(@Param("placeId") Long placeId);

    // 사용자 권한 확인용 - 특정 Moment의 소유자 확인
    @Query("SELECT m.userId FROM Moment m WHERE m.id = :momentId")
    Optional<Long> findUserIdByMomentId(@Param("momentId") Long momentId);

    // 최근 업데이트된 공개 Moment 조회 (홈 피드용)
    @Query("SELECT m FROM Moment m " +
            "WHERE m.isPublic = true " +
            "AND m.updatedAt >= :since " +
            "ORDER BY m.updatedAt DESC")
    List<Moment> findRecentPublicMoments(@Param("since") java.time.LocalDateTime since, Pageable pageable);

    // 특정 사용자들의 Moment 조회 (팔로우 기능 대비)
    @Query("SELECT m FROM Moment m " +
            "WHERE m.userId IN :userIds " +
            "AND m.isPublic = true " +
            "ORDER BY m.createdAt DESC")
    Page<Moment> findMomentsByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);

    // 여러 장소의 Moment 개수를 한 번에 조회
    @Query("SELECT m.placeId, COUNT(m) FROM Moment m " +
            "WHERE m.placeId IN :placeIds " +
            "AND m.isPublic = true " +
            "GROUP BY m.placeId")
    List<Object[]> countPublicMomentsByPlaceIds(@Param("placeIds") List<Long> placeIds);
}
