package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.BookmarkResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PlaceBookmarkQueryService {

    BookmarkResponse getUserBookmarks(Long userId);

    boolean isBookmarked(Long userId, Long placeId);

    Map<Long, Boolean> getBookmarkStatusMap(Long userId, List<Long> placeIds);

    Set<Long> getBookmarkedPlaceIds(Long userId);
}
