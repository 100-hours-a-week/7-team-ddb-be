package com.dolpin.domain.place.service.command;

public interface PlaceBookmarkCommandService {

    boolean toggleBookmark(Long userId, Long placeId);
}
