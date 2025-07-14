package com.dolpin.domain.place.service.template;

import com.dolpin.domain.place.dto.response.PlaceDetailResponse;
import com.dolpin.domain.place.entity.PlaceHours;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlaceDetailContext {
    private final Long placeId;
    private final Long userId;
    private List<String> keywords;
    private List<PlaceDetailResponse.Menu> menus;
    private List<PlaceHours> hours;
    private Boolean isBookmarked;

    public static PlaceDetailContext of(Long placeId, Long userId) {
        return PlaceDetailContext.builder()
                .placeId(placeId)
                .userId(userId)
                .build();
    }
}
