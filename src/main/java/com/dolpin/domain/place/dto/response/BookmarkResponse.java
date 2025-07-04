package com.dolpin.domain.place.dto.response;

import com.dolpin.domain.place.entity.Place;
import com.dolpin.domain.place.entity.PlaceBookmark;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {

    private List<BookmarkDto> bookmarks;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookmarkDto {
        private String thumbnail;
        private Long id;
        private String name;
        private List<String> keywords;
        private Boolean isBookmarked;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private LocalDateTime createdAt;

        public static BookmarkDto from(PlaceBookmark bookmark, Place place) {
            // 키워드 추출
            List<String> keywords = place.getKeywords().stream()
                    .map(pk -> pk.getKeyword().getKeyword())
                    .collect(Collectors.toList());

            return BookmarkDto.builder()
                    .thumbnail(place.getImageUrl())
                    .id(place.getId())
                    .name(place.getName())
                    .keywords(keywords)
                    .isBookmarked(true)
                    .createdAt(bookmark.getCreatedAt())
                    .build();
        }
    }

    public static BookmarkResponse from(List<BookmarkDto> bookmarks) {
        return BookmarkResponse.builder()
                .bookmarks(bookmarks)
                .build();
    }
}
