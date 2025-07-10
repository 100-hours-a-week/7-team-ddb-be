package com.dolpin.domain.place.service.factory;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.entity.Place;
import org.springframework.stereotype.Component;

import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DistanceBasedPlaceDtoStrategy implements PlaceDtoStrategy {

    @Override
    public boolean supports(PlaceDtoContext context) {
        // 거리 정보가 있고 AI 유사도 점수가 없는 경우 (카테고리 검색 등)
        return context.hasDistance() && !context.hasSimilarityScore();
    }

    @Override
    public int getPriority() {
        return 2; // AI 검색보다는 낮지만 표준보다는 높은 우선순위
    }

    @Override
    public PlaceSearchResponse.PlaceDto createPlaceDto(PlaceDtoContext context) {
        Place place = context.getPlace();

        return PlaceSearchResponse.PlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .thumbnail(place.getImageUrl())
                .distance(convertDistance(context.getDistance()))
                .momentCount(context.getMomentCount())
                .keywords(extractKeywords(context))
                .location(buildLocationMap(place.getLocation()))
                .isBookmarked(context.getIsBookmarked())
                .similarityScore(null) // 거리 기반 검색에서는 유사도 점수 없음
                .build();
    }

    private List<String> extractKeywords(PlaceDtoContext context) {
        return context.getPlace().getKeywords().stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildLocationMap(Point location) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});
        return locationMap;
    }

    private Double convertDistance(Double distanceInMeters) {
        if (distanceInMeters == null) return 0.0;

        if (distanceInMeters < 1000) {
            return (double) Math.round(distanceInMeters);
        } else {
            return BigDecimal.valueOf(distanceInMeters / 1000.0)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }
}
