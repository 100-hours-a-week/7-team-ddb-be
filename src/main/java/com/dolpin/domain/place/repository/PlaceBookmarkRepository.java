package com.dolpin.domain.place.repository;

import com.dolpin.domain.place.entity.PlaceBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceBookmarkRepository extends JpaRepository<PlaceBookmark, Long> {

    // 특정 사용자의 특정 장소 북마크 조회
    @Query("SELECT pb FROM PlaceBookmark pb " +
            "WHERE pb.userId = :userId AND pb.placeId = :placeId")
    Optional<PlaceBookmark> findByUserIdAndPlaceId(@Param("userId") Long userId, @Param("placeId") Long placeId);

    // 특정 사용자의 모든 북마크 조회 (최신순)
    @Query("SELECT pb FROM PlaceBookmark pb " +
            "WHERE pb.userId = :userId " +
            "ORDER BY pb.createdAt DESC")
    List<PlaceBookmark> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // 특정 사용자의 북마크 개수
    @Query("SELECT COUNT(pb) FROM PlaceBookmark pb WHERE pb.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    // 특정 장소의 북마크 개수
    @Query("SELECT COUNT(pb) FROM PlaceBookmark pb WHERE pb.placeId = :placeId")
    long countByPlaceId(@Param("placeId") Long placeId);

    // 특정 사용자가 북마크한 장소 ID 목록
    @Query("SELECT pb.placeId FROM PlaceBookmark pb WHERE pb.userId = :userId")
    List<Long> findPlaceIdsByUserId(@Param("userId") Long userId);

    // 북마크 존재 여부 확인
    boolean existsByUserIdAndPlaceId(Long userId, Long placeId);

    // 사용자의 북마크 삭제
    void deleteByUserIdAndPlaceId(Long userId, Long placeId);
}
