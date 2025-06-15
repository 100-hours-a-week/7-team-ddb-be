package com.dolpin.domain.moment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MomentListResponse {

    private List<MomentSummaryDto> moments;
    private MetaDto meta;

    @JsonProperty("_links")
    private LinksDto links;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MomentSummaryDto {
        private Long id;
        private String title;
        private String content;
        private String thumbnail;
        private Integer imagesCount;
        private Boolean isPublic;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        private LocalDateTime createdAt;

        private Long viewCount;
        private Long commentCount;
        private AuthorDto author;
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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaDto {
        private PaginationDto pagination;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationDto {
        private Integer limit;

        private String nextCursor;

        private Boolean hasNext;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinksDto {
        private LinkDto self;
        private LinkDto next;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkDto {
        private String href;
    }
}
