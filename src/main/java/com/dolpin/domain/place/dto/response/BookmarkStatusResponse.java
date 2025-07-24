package com.dolpin.domain.place.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookmarkStatusResponse {

    @JsonProperty("is_bookmarked")
    private final boolean isBookmarked;

    public static BookmarkStatusResponse of(boolean isBookmarked) {
        return BookmarkStatusResponse.builder()
                .isBookmarked(isBookmarked)
                .build();
    }
}
