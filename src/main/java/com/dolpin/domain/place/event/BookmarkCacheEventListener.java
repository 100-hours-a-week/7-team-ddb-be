package com.dolpin.domain.place.event;

import com.dolpin.domain.place.service.cache.BookmarkCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookmarkCacheEventListener {

    private final BookmarkCacheService bookmarkCacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("bookmarkCacheExecutor")
    public void handleBookmarkChanged(BookmarkChangedEvent event) {
        try {
            // 새로운 북마크 상태 캐시
            bookmarkCacheService.cacheBookmarkStatus(event.getUserId(), event.getPlaceId(), event.isAdded());

            // 북마크 목록 캐시 무효화
            bookmarkCacheService.invalidateUserBookmarkList(event.getUserId());

            log.debug("북마크 캐시 동기화 완료: userId={}, placeId={}, added={}",
                    event.getUserId(), event.getPlaceId(), event.isAdded());

        } catch (Exception e) {
            log.error("북마크 캐시 동기화 실패: userId={}, placeId={}",
                    event.getUserId(), event.getPlaceId(), e);
        }
    }
}
