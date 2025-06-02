package com.dolpin.domain.moment.repository;

import com.dolpin.domain.moment.entity.MomentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MomentImageRepository extends JpaRepository<MomentImage, Long> {

    // 특정 Moment의 모든 이미지를 순서대로 조회
    @Query("SELECT mi FROM MomentImage mi " +
            "WHERE mi.moment.id = :momentId " +
            "ORDER BY mi.imageSequence ASC")
    List<MomentImage> findByMomentIdOrderBySequence(@Param("momentId") Long momentId);

    // 특정 Moment의 특정 순서 이미지 조회
    @Query("SELECT mi FROM MomentImage mi " +
            "WHERE mi.moment.id = :momentId " +
            "AND mi.imageSequence = :sequence")
    Optional<MomentImage> findByMomentIdAndSequence(@Param("momentId") Long momentId,
                                                    @Param("sequence") Integer sequence);

    // 특정 Moment의 이미지 개수
    @Query("SELECT COUNT(mi) FROM MomentImage mi WHERE mi.moment.id = :momentId")
    long countByMomentId(@Param("momentId") Long momentId);

    // 특정 Moment의 모든 이미지 삭제
    void deleteByMomentId(Long momentId);

    // 특정 순서 이후의 이미지들 조회 (순서 재정렬용)
    @Query("SELECT mi FROM MomentImage mi " +
            "WHERE mi.moment.id = :momentId " +
            "AND mi.imageSequence > :sequence " +
            "ORDER BY mi.imageSequence ASC")
    List<MomentImage> findByMomentIdAndSequenceGreaterThan(@Param("momentId") Long momentId,
                                                           @Param("sequence") Integer sequence);

    // 특정 이미지 URL을 가진 이미지 조회 (중복 체크용)
    @Query("SELECT mi FROM MomentImage mi WHERE mi.imageUrl = :imageUrl")
    List<MomentImage> findByImageUrl(@Param("imageUrl") String imageUrl);
}
