package com.dolpin.domain.place.service.query;

import com.dolpin.domain.place.dto.response.BookmarkResponse;
import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.entity.PlaceBookmark;
import com.dolpin.domain.place.repository.PlaceBookmarkRepository;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.domain.place.service.cache.BookmarkCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceBookmarkQueryServiceImpl implements PlaceBookmarkQueryService {

    private final PlaceBookmarkRepository bookmarkRepository;
    private final PlaceRepository placeRepository;
    private final BookmarkCacheService bookmarkCacheService;

    @Override
    @Transactional(readOnly = true)
    public BookmarkResponse getUserBookmarks(Long userId) {
        // 1. 캐시에서 북마크 목록 조회
        List<BookmarkCacheService.UserBookmarkCacheItem> cachedItems =
                bookmarkCacheService.getCachedUserBookmarkList(userId);

        if (cachedItems != null) {
            // 캐시 히트: DTO 변환 후 반환
            List<BookmarkResponse.BookmarkDto> bookmarkDtos = cachedItems.stream()
                    .map(this::convertCacheItemToDto)
                    .collect(Collectors.toList());

            log.debug("북마크 목록 캐시 히트: userId={}, count={}", userId, bookmarkDtos.size());
            return BookmarkResponse.from(bookmarkDtos);
        }

        // 2. 캐시 미스: DB에서 조회 후 캐시 저장
        return loadBookmarksFromDbAndCache(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long userId, Long placeId) {
        if (userId == null || placeId == null) {
            return false;
        }

        // 캐시에서 조회
        Boolean cachedStatus = bookmarkCacheService.getBookmarkStatus(userId, placeId);
        if (cachedStatus != null) {
            log.debug("북마크 상태 캐시 히트: userId={}, placeId={}, status={}", userId, placeId, cachedStatus);
            return cachedStatus;
        }

        // 캐시 미스: DB 조회 후 캐시 저장
        boolean isBookmarked = bookmarkRepository.existsByUserIdAndPlaceId(userId, placeId);
        bookmarkCacheService.cacheBookmarkStatus(userId, placeId, isBookmarked);

        log.debug("북마크 상태 DB 조회: userId={}, placeId={}, status={}", userId, placeId, isBookmarked);
        return isBookmarked;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> getBookmarkStatusMap(Long userId, List<Long> placeIds) {
        if (userId == null || placeIds == null || placeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 캐시에서 배치 조회
        Map<Long, Boolean> cachedStatuses = bookmarkCacheService.getBookmarkStatuses(userId, placeIds);

        // 캐시 미스인 것들 추출
        List<Long> missedPlaceIds = placeIds.stream()
                .filter(placeId -> !cachedStatuses.containsKey(placeId))
                .collect(Collectors.toList());

        Map<Long, Boolean> result = new HashMap<>(cachedStatuses);

        if (!missedPlaceIds.isEmpty()) {
            // DB에서 사용자의 모든 북마크 조회
            List<Long> bookmarkedIds = bookmarkRepository.findPlaceIdsByUserId(userId);
            Set<Long> bookmarkedSet = new HashSet<>(bookmarkedIds);

            // 각 장소별 북마크 상태 확인
            Map<Long, Boolean> missedStatuses = new HashMap<>();
            for (Long placeId : missedPlaceIds) {
                boolean isBookmarked = bookmarkedSet.contains(placeId);
                result.put(placeId, isBookmarked);
                missedStatuses.put(placeId, isBookmarked);
            }

            // 핵심 변경: 개별 비동기 호출 대신 배치 처리
            if (missedStatuses.size() > 10) {
                // 대량 데이터는 비동기 배치 처리
                bookmarkCacheService.cacheBookmarkStatusesAsync(userId, missedStatuses);
            } else {
                // 소량 데이터는 동기 배치 처리
                bookmarkCacheService.cacheBookmarkStatusesBatch(userId, missedStatuses);
            }

            log.debug("북마크 상태 배치 조회: 캐시 히트 {}/{}, DB 조회 {}/{}",
                    cachedStatuses.size(), placeIds.size(), missedPlaceIds.size(), placeIds.size());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getBookmarkedPlaceIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        List<Long> placeIds = bookmarkRepository.findPlaceIdsByUserId(userId);
        return new HashSet<>(placeIds);
    }


    private BookmarkResponse loadBookmarksFromDbAndCache(Long userId) {
        log.debug("북마크 목록 DB 조회: userId={}", userId);

        // DB에서 북마크 목록 조회
        List<PlaceBookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);

        if (bookmarks.isEmpty()) {
            // 빈 목록도 캐시에 저장 (불필요한 반복 DB 조회 방지)
            bookmarkCacheService.cacheUserBookmarkList(userId, Collections.emptyList());
            return BookmarkResponse.from(List.of());
        }

        // 장소 정보 조회
        List<Long> placeIds = bookmarks.stream()
                .map(PlaceBookmark::getPlaceId)
                .collect(Collectors.toList());

        List<Place> places = placeRepository.findByIdsWithKeywords(placeIds);
        Map<Long, Place> placeMap = places.stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));

        // 캐시 아이템 생성
        List<BookmarkCacheService.UserBookmarkCacheItem> cacheItems = bookmarks.stream()
                .map(bookmark -> {
                    Place place = placeMap.get(bookmark.getPlaceId());
                    if (place != null) {
                        List<String> keywords = place.getKeywords().stream()
                                .map(pk -> pk.getKeyword().getKeyword())
                                .collect(Collectors.toList());

                        return BookmarkCacheService.UserBookmarkCacheItem.builder()
                                .placeId(place.getId())
                                .placeName(place.getName())
                                .thumbnail(place.getImageUrl())
                                .keywords(keywords)
                                .bookmarkCreatedAt(bookmark.getCreatedAt())
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 캐시 저장
        bookmarkCacheService.cacheUserBookmarkList(userId, cacheItems);

        // DTO 변환 및 반환
        List<BookmarkResponse.BookmarkDto> bookmarkDtos = cacheItems.stream()
                .map(this::convertCacheItemToDto)
                .collect(Collectors.toList());

        log.debug("북마크 목록 DB 조회 완료: userId={}, count={}", userId, bookmarkDtos.size());
        return BookmarkResponse.from(bookmarkDtos);
    }

    private BookmarkResponse.BookmarkDto convertCacheItemToDto(
            BookmarkCacheService.UserBookmarkCacheItem cacheItem) {
        return BookmarkResponse.BookmarkDto.builder()
                .thumbnail(cacheItem.getThumbnail())
                .id(cacheItem.getPlaceId())
                .name(cacheItem.getPlaceName())
                .keywords(cacheItem.getKeywords())
                .isBookmarked(true)
                .createdAt(cacheItem.getBookmarkCreatedAt())
                .build();
    }
}
