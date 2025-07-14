package com.dolpin.domain.place.service.factory;

import com.dolpin.domain.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PlaceDtoContext {
    private final Place place;
    private final Double distance;
    private final Double similarityScore;
    private final List<String> aiKeywords;
    private final Long momentCount;
    private final Boolean isBookmarked;

    // 편의 메서드들
    public boolean hasAiKeywords() {
        return aiKeywords != null && !aiKeywords.isEmpty();
    }

    public boolean hasDistance() {
        return distance != null;
    }

    public boolean hasSimilarityScore() {
        return similarityScore != null;
    }

    public static PlaceDtoContext of(Place place) {
        return PlaceDtoContext.builder()
                .place(place)
                .momentCount(0L)
                .isBookmarked(false)
                .build();
    }
}
