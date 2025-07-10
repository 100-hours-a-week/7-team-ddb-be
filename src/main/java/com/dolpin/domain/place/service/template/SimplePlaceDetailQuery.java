package com.dolpin.domain.place.service.template;

import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.query.PlaceBookmarkQueryService;
import org.springframework.stereotype.Component;

@Component
public class SimplePlaceDetailQuery extends PlaceDetailQueryTemplate {

    public SimplePlaceDetailQuery(PlaceRepository placeRepository,
                                  PlaceBookmarkQueryService bookmarkQueryService) {
        super(placeRepository, bookmarkQueryService);
    }

    @Override
    protected PlaceDetailContext collectDetailInformation(Long placeId, Long userId) {
        return PlaceDetailContext.builder()
                .placeId(placeId)
                .userId(userId)
                .keywords(getKeywords(placeId))
                .menus(getMenus(placeId))
                .hours(getHours(placeId))
                .isBookmarked(null) // 북마크 정보 없음
                .build();
    }
}
