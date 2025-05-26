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

    // 한 번에 모든 연관 데이터 조회
    @Query("SELECT DISTINCT p FROM Place p " +
            "LEFT JOIN FETCH p.keywords pk LEFT JOIN FETCH pk.keyword " +
            "LEFT JOIN FETCH p.menus " +
            "LEFT JOIN FETCH p.hours " +
            "WHERE p.id = :id")
    Optional<Place> findCompleteById(@Param("id") Long id);

    // 여러 ID에 대해 한 번에 모든 연관 데이터 조회
    @Query("SELECT DISTINCT p FROM Place p " +
            "LEFT JOIN FETCH p.keywords pk LEFT JOIN FETCH pk.keyword " +
            "LEFT JOIN FETCH p.menus " +
            "LEFT JOIN FETCH p.hours " +
            "WHERE p.id IN :ids")
    List<Place> findCompleteByIds(@Param("ids") List<Long> ids);

    // 검색용 - 키워드만 포함한 조회 (필요한 데이터만)
    @Query("SELECT DISTINCT p FROM Place p " +
            "LEFT JOIN FETCH p.keywords pk LEFT JOIN FETCH pk.keyword " +
            "WHERE p.id IN :ids")
    List<Place> findByIdsWithKeywords(@Param("ids") List<Long> ids);

    // 기존 메서드들 유지 (하위 호환성)
    @Query("SELECT p FROM Place p " +
            "LEFT JOIN FETCH p.keywords k LEFT JOIN FETCH k.keyword " +
            "WHERE p.id = :id")
    Optional<Place> findByIdWithKeywords(@Param("id") Long id);

    @Query("SELECT p FROM Place p " +
            "LEFT JOIN FETCH p.menus " +
            "WHERE p.id = :id")
    Optional<Place> findByIdWithMenus(@Param("id") Long id);

    @Query("SELECT p FROM Place p " +
            "LEFT JOIN FETCH p.hours " +
            "WHERE p.id = :id")
    Optional<Place> findByIdWithHours(@Param("id") Long id);

    // Native Query들은 그대로 유지
    @Query(value =
            "SELECT p.id as id, p.name as name, p.category as category, " +
                    "p.road_address as roadAddress, p.lot_address as lotAddress, " +
                    "p.image_url as imageUrl, " +
                    "ST_X(p.location) as longitude, " +
                    "ST_Y(p.location) as latitude, " +
                    "ST_Distance(p.location::geography, ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography) as distance " +
                    "FROM place p " +
                    "WHERE p.id IN :placeIds " +
                    "AND ST_DWithin(p.location::geography, ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography, :radius)",
            nativeQuery = true)
    List<PlaceWithDistance> findPlacesWithinRadiusByIds(
            @Param("placeIds") List<Long> placeIds,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius);

    @Query(value = "SELECT p.id as id, p.name as name, p.category as category, " +
            "p.road_address as roadAddress, p.lot_address as lotAddress, " +
            "p.image_url as imageUrl, " +
            "ST_X(p.location) as longitude, " +
            "ST_Y(p.location) as latitude, " +
            "ST_Distance(p.location::geography, ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography) as distance " +
            "FROM place p " +
            "WHERE p.category = :category " +
            "AND ST_DWithin(p.location::geography, ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography, :radius) " +
            "ORDER BY distance",
            countQuery = "SELECT COUNT(*) " +
                    "FROM place p " +
                    "WHERE p.category = :category " +
                    "AND ST_DWithin(p.location::geography, ST_SetSRID(ST_Point(:lng, :lat), 4326)::geography, :radius)",
            nativeQuery = true)
    List<PlaceWithDistance> findPlacesByCategoryWithinRadius(
            @Param("category") String category,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius);

    @Query("SELECT p.category " +
            "FROM Place p " +
            "WHERE p.category IS NOT NULL " +
            "GROUP BY p.category " +
            "ORDER BY COUNT(*) DESC")
    List<String> findDistinctCategories();
}