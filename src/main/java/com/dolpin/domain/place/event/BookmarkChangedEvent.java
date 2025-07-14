package com.dolpin.domain.place.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookmarkChangedEvent {
    private final Long userId;
    private final Long placeId;
    private final boolean added; // true: 추가, false: 제거

    public static BookmarkChangedEvent added(Long userId, Long placeId) {
        return new BookmarkChangedEvent(userId, placeId, true);
    }

    public static BookmarkChangedEvent removed(Long userId, Long placeId) {
        return new BookmarkChangedEvent(userId, placeId, false);
    }
}
