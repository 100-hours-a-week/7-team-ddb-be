package com.dolpin.domain.place.repository;

import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    // 기본 장소 정보만 조회 (컬렉션 없이)
    @Query("SELECT p FROM Place p WHERE p.id = :id")
    Optional<Place> findBasicPlaceById(@Param("id") Long id);

    // 장소 + 키워드 정보 조회
    @Query("SELECT p FROM Place p " +
            "LEFT JOIN FETCH p.keywords k LEFT JOIN FETCH k.keyword " +
            "WHERE p.id = :id")
    Optional<Place> findByIdWithKeywords(@Param("id") Long id);

    // 장소 + 메뉴 정보 조회
    @Query("SELECT p FROM Place p " +
            "LEFT JOIN FETCH p.menus " +
            "WHERE p.id = :id")
    Optional<Place> findByIdWithMenus(@Param("id") Long id);

    // 장소 + 영업시간 정보 조회
    @Query("SELECT p FROM Place p " +
            "LEFT JOIN FETCH p.hours " +
            "WHERE p.id = :id")
    Optional<Place> findByIdWithHours(@Param("id") Long id);

    // 기존 메서드는 주석 처리 또는 제거
    // @Query("SELECT p FROM Place p " +
    //        "LEFT JOIN FETCH p.keywords k LEFT JOIN FETCH k.keyword " +
    //        "LEFT JOIN FETCH p.menus " +
    //        "LEFT JOIN FETCH p.hours " +
    //        "WHERE p.id = :id")
    // Optional<Place> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT p FROM Place p " +
            "LEFT JOIN FETCH p.keywords k LEFT JOIN FETCH k.keyword " +
            "WHERE p.id IN :ids")
    List<Place> findByIdsWithKeywords(@Param("ids") List<Long> ids);

    @Query(value =
            "SELECT p.id as id, p.name as name, p.category as category, " +
                    "p.road_address as roadAddress, p.lot_address as lotAddress, " +
                    "ST_X(p.location) as longitude, " +
                    "ST_Y(p.location) as latitude, " +
                    "ST_Distance(p.location::geography, ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography) as distance " +
                    "FROM place p " +
                    "WHERE p.id IN :placeIds " +
                    "AND ST_DWithin(p.location::geography, ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography, :radius) " +
                    "ORDER BY distance",
            nativeQuery = true)
    List<PlaceWithDistance> findPlacesWithinRadiusByIds(
            @Param("placeIds") List<Long> placeIds,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius);

    @Query(value = "SELECT p.id as id, p.name as name, p.category as category, " +
            "p.road_address as roadAddress, p.lot_address as lotAddress, " +
            "ST_X(p.location) as longitude, " +  // 경도(X) 좌표 추가
            "ST_Y(p.location) as latitude, " +   // 위도(Y) 좌표 추가
            "ST_Distance(p.location, ST_SetSRID(ST_Point(:lng, :lat), 4326)) as distance " +
            "FROM place p " +
            "WHERE p.category = :category " +
            "AND ST_DWithin(p.location, ST_SetSRID(ST_Point(:lng, :lat), 4326), :radius) " +
            "ORDER BY distance",
            countQuery = "SELECT COUNT(*) " +
                    "FROM place p " +
                    "WHERE p.category = :category " +
                    "AND ST_DWithin(p.location, ST_SetSRID(ST_Point(:lng, :lat), 4326), :radius)",
            nativeQuery = true)
    List<PlaceWithDistance> findPlacesByCategoryWithinRadius(
            @Param("category") String category,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius);

    @Query("SELECT DISTINCT p.category FROM Place p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findDistinctCategories();
}