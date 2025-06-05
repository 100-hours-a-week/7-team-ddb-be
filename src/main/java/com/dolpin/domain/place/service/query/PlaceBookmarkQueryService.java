package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.BookmarkResponse;

public interface PlaceBookmarkQueryService {

    BookmarkResponse getUserBookmarks(Long userId);
}
