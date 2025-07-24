package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.BookmarkResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PlaceBookmarkQueryService {

    /**
     * 사용자의 북마크 목록 조회
     */
    BookmarkResponse getUserBookmarks(Long userId);

    /**
     * 특정 장소의 북마크 상태 조회 (장소 존재 여부 검증 포함)
     */
    boolean isBookmarkedWithValidation(Long userId, Long placeId);

    /**
     * 특정 장소의 북마크 상태 조회 (기존 메서드)
     */
    boolean isBookmarked(Long userId, Long placeId);

    /**
     * 여러 장소의 북마크 상태 배치 조회
     */
    Map<Long, Boolean> getBookmarkStatusMap(Long userId, List<Long> placeIds);

    /**
     * 사용자가 북마크한 장소 ID 목록 조회
     */
    Set<Long> getBookmarkedPlaceIds(Long userId);
}
