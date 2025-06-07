package com.dolpin.domain.place.service.command;

import com.dolpin.domain.place.entity.PlaceBookmark;
import com.dolpin.domain.place.repository.PlaceBookmarkRepository;
import com.dolpin.domain.place.repository.PlaceRepository;
import com.dolpin.global.exception.BusinessException;
import com.dolpin.global.response.ResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceBookmarkCommandServiceImpl implements PlaceBookmarkCommandService {

    private final PlaceBookmarkRepository bookmarkRepository;
    private final PlaceRepository placeRepository;

    @Override
    @Transactional
    public boolean toggleBookmark(Long userId, Long placeId) {
        // 장소 존재 확인
        if (!placeRepository.existsById(placeId)) {
            throw new BusinessException(ResponseStatus.PLACE_NOT_FOUND);
        }

        // 기존 북마크 조회
        Optional<PlaceBookmark> existingBookmark = bookmarkRepository.findByUserIdAndPlaceId(userId, placeId);

        if (existingBookmark.isPresent()) {
            // 북마크가 존재하면 삭제
            bookmarkRepository.delete(existingBookmark.get());
            log.info("Bookmark removed: userId={}, placeId={}", userId, placeId);
            return false;
        } else {
            // 북마크가 없으면 생성
            PlaceBookmark bookmark = PlaceBookmark.create(userId, placeId);
            bookmarkRepository.save(bookmark);
            log.info("Bookmark added: userId={}, placeId={}", userId, placeId);
            return true;
        }
    }
}
