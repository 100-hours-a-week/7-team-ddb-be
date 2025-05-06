package com.dolpin.domain.place.repository;

import com.dolpin.domain.place.dto.response.PlaceWithDistance;
import com.dolpin.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * 지정된 ID 목록 중에서 특정 반경 내에 있는 장소만 조회
     */
    @Query(value =
            "SELECT p as place, ST_Distance(p.location, ST_SetSRID(ST_Point(:lng, :lat), 4326)) as distance " +
                    "FROM place p " +
                    "WHERE p.id IN :placeIds " +
                    "AND ST_DWithin(p.location, ST_SetSRID(ST_Point(:lng, :lat), 4326), :radius) " +
                    "ORDER BY distance",
            nativeQuery = true)
    List<PlaceWithDistance> findPlacesWithinRadiusByIds(
            @Param("placeIds") List<Long> placeIds,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius);

    @Query(value = "SELECT p as place, " +
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
}