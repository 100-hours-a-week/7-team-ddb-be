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


    @Query("SELECT p FROM Place p " +
            "LEFT JOIN FETCH p.keywords k LEFT JOIN FETCH k.keyword " +
            "LEFT JOIN FETCH p.menus " +
            "LEFT JOIN FETCH p.hours " +
            "WHERE p.id = :id")
    Optional<Place> findByIdWithDetails(@Param("id") Long id);


    @Query("SELECT p FROM Place p " +
            "LEFT JOIN FETCH p.keywords k LEFT JOIN FETCH k.keyword " +
            "WHERE p.id IN :ids")
    List<Place> findByIdsWithKeywords(@Param("ids") List<Long> ids);


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