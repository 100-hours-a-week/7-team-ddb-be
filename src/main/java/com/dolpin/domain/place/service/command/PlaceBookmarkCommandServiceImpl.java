package com.dolpin.domain.place.service.command;

import com.dolpin.domain.place.entity.PlaceBookmark;
import com.dolpin.domain.place.event.BookmarkChangedEvent;
import com.dolpin.domain.place.repository.PlaceBookmarkRepository;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceBookmarkCommandServiceImpl implements PlaceBookmarkCommandService {

    private final PlaceBookmarkRepository bookmarkRepository;
    private final PlaceRepository placeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public boolean toggleBookmark(Long userId, Long placeId) {
        // 장소 존재 확인
        if (!placeRepository.existsById(placeId)) {
            throw new BusinessException(ResponseStatus.PLACE_NOT_FOUND);
        }

        Optional<PlaceBookmark> existingBookmark = bookmarkRepository.findByUserIdAndPlaceId(userId, placeId);

        if (existingBookmark.isPresent()) {
            // 북마크 제거
            bookmarkRepository.delete(existingBookmark.get());

            // 이벤트 발행 (추가)
            eventPublisher.publishEvent(BookmarkChangedEvent.removed(userId, placeId));

            log.info("북마크 제거: userId={}, placeId={}", userId, placeId);
            return false;
        } else {
            // 북마크 추가
            PlaceBookmark bookmark = PlaceBookmark.create(userId, placeId);
            bookmarkRepository.save(bookmark);

            // 이벤트 발행 (추가)
            eventPublisher.publishEvent(BookmarkChangedEvent.added(userId, placeId));

            log.info("북마크 추가: userId={}, placeId={}", userId, placeId);
            return true;
        }
    }
}
