package com.dolpin.domain.place.service.factory;

import com.dolpin.domain.place.dto.response.PlaceSearchResponse;
import com.dolpin.domain.place.entity.Place;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AiSearchPlaceDtoStrategy implements PlaceDtoStrategy {

    @Override
    public boolean supports(PlaceDtoContext context) {
        // AI 유사도 점수나 AI 키워드가 있는 경우
        return context.hasSimilarityScore() || context.hasAiKeywords();
    }

    @Override
    public int getPriority() {
        return 1; // 높은 우선순위
    }

    @Override
    public PlaceSearchResponse.PlaceDto createPlaceDto(PlaceDtoContext context) {
        Place place = context.getPlace();

        return PlaceSearchResponse.PlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .thumbnail(place.getImageUrl())
                .distance(null)
                .momentCount(context.getMomentCount())
                .keywords(prioritizeAiKeywords(context))
                .location(buildLocationMap(place.getLocation()))
                .isBookmarked(context.getIsBookmarked())
                .similarityScore(context.getSimilarityScore())
                .build();
    }

    private List<String> prioritizeAiKeywords(PlaceDtoContext context) {
        // AI 키워드를 우선적으로 사용
        if (context.hasAiKeywords()) {
            return context.getAiKeywords();
        }

        // AI 키워드가 없으면 기존 키워드 사용
        return context.getPlace().getKeywords().stream()
                .map(pk -> pk.getKeyword().getKeyword())
                .limit(5) // AI 검색에서는 키워드 개수 제한
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildLocationMap(Point location) {
        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("type", "Point");
        locationMap.put("coordinates", new double[]{location.getX(), location.getY()});
        return locationMap;
    }
}
