package com.dolpin.domain.place.service.factory;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.entity.Place;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StandardPlaceDtoStrategy implements PlaceDtoStrategy {

    @Override
    public boolean supports(PlaceDtoContext context) {
        // 기본적으로 모든 컨텍스트 지원 (가장 낮은 우선순위)
        return true;
    }

    @Override
    public int getPriority() {
        return 999; // 가장 낮은 우선순위 (fallback)
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
                .similarityScore(context.getSimilarityScore())
                .build();
    }

    protected List<String> extractKeywords(PlaceDtoContext context) {
        if (context.hasAiKeywords()) {
            return context.getAiKeywords();
        }

        return context.getPlace().getKeywords().stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .collect(Collectors.toList());
    }

    protected Map<String, Object> buildLocationMap(Point location) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});
        return locationMap;
    }

    protected Double convertDistance(Double distanceInMeters) {
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
