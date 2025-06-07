package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.BookmarkResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.entity.PlaceBookmark;
import com.dolpin.domain.place.repository.PlaceBookmarkRepository;
import com.dolpin.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceBookmarkQueryServiceImpl implements PlaceBookmarkQueryService {

    private final PlaceBookmarkRepository bookmarkRepository;
    private final PlaceRepository placeRepository;

    @Override
    @Transactional(readOnly = true)
    public BookmarkResponse getUserBookmarks(Long userId) {
        // 사용자의 북마크 목록 조회
        List<PlaceBookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);

        if (bookmarks.isEmpty()) {
            return BookmarkResponse.from(List.of());
        }

        // 장소 ID 목록 추출
        List<Long> placeIds = bookmarks.stream()
                .map(PlaceBookmark::getPlaceId)
                .collect(Collectors.toList());

        // 장소 정보를 키워드와 함께 한 번에 조회 (N+1 문제 방지)
        List<Place> places = placeRepository.findByIdsWithKeywords(placeIds);

        // Place ID -> Place 매핑
        Map<Long, Place> placeMap = places.stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));

        // DTO 변환
        List<BookmarkResponse.BookmarkDto> bookmarkDtos = bookmarks.stream()
                .map(bookmark -> {
                    Place place = placeMap.get(bookmark.getPlaceId());
                    if (place != null) {
                        return BookmarkResponse.BookmarkDto.from(bookmark, place);
                    } else {
                        // 장소가 삭제된 경우 로그 남기고 null 반환 (필터링됨)
                        log.warn("Place not found for bookmark: userId={}, placeId={}",
                                userId, bookmark.getPlaceId());
                        return null;
                    }
                })
                .filter(dto -> dto != null) // null 필터링
                .collect(Collectors.toList());

        return BookmarkResponse.from(bookmarkDtos);
    }
}
