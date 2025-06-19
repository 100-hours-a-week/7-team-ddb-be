package com.dolpin.domain.moment.dto.response;

import com.dolpin.domain.user.entity.User;
import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.entity.MomentImage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentDetailResponse {

    private Long id;
    private String title;
    private String content;
    private List<String> images;
    private PlaceDetailDto place;
    private Boolean isPublic;
    private Boolean isOwner;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime createdAt;

    private Long commentCount;
    private Long viewCount;
    private AuthorDto author;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceDetailDto {
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

    // ✅ 올바른 수정 방법 1: from 메서드에 author 파라미터 추가
    public static MomentDetailResponse from(Moment moment, boolean isOwner,
                                            Long commentCount, Long viewCount, User author) {  // 👈 User author 추가
        List<String> imageUrls = moment.getImages().stream()
                .map(MomentImage::getImageUrl)
                .collect(Collectors.toList());

        return MomentDetailResponse.builder()
                .id(moment.getId())
                .title(moment.getTitle())
                .content(moment.getContent())
                .images(imageUrls)
                .place(moment.getPlaceId() != null ?
                        PlaceDetailDto.builder()
                                .id(moment.getPlaceId())
                                .name(moment.getPlaceName())
                                .build()
                        : null)
                .isPublic(moment.getIsPublic())
                .isOwner(isOwner)
                .createdAt(moment.getCreatedAt())
                .commentCount(commentCount)
                .viewCount(viewCount)
                .author(AuthorDto.builder()
                        .id(author.getId())
                        .nickname(author.getUsername())
                        .profileImage(author.getImageUrl())
                        .build())
                .build();
    }
}
