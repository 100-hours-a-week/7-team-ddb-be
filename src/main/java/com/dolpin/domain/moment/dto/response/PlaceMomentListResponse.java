package com.dolpin.domain.moment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceMomentListResponse {

    private Integer total;
    private List<PlaceMomentDto> moments;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceMomentDto {
        private Long id;
        private String title;
        private String thumbnail;
        private Integer imagesCount;
        private Boolean isPublic;
        private LocalDateTime createdAt;

        private PlaceDto place;
        private AuthorDto author;
        private String location;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceDto {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private Long id;
        private String nickname;
        private String profileImage;
    }
}
