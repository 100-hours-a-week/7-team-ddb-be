package com.dolpin.domain.moment.dto.response;

import com.dolpin.domain.moment.entity.Moment;
import com.dolpin.domain.moment.entity.MomentImage;
import com.dolpin.domain.place.entity.Place;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private Map<String, Object> location;

    @JsonProperty("is_public")
    private Boolean isPublic;

    @JsonProperty("is_owner")
    private Boolean isOwner;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("comments_count")
    private Integer commentsCount;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceDetailDto {
        private Long id;
        private String name;
    }

    public static MomentDetailResponse from(Moment moment, Place place, boolean isOwner) {
        List<String> imageUrls = moment.getImages().stream()
                .map(MomentImage::getImageUrl)
                .collect(Collectors.toList());

        Map<String, Object> locationMap = null;
        if (place != null && place.getLocation() != null) {
            locationMap = Map.of(
                    "type", "Point",
                    "coordinates", new double[]{
                            place.getLocation().getX(),
                            place.getLocation().getY()
                    }
            );
        }

        return MomentDetailResponse.builder()
                .id(moment.getId())
                .title(moment.getTitle())
                .content(moment.getContent())
                .images(imageUrls)
                .place(PlaceDetailDto.builder()
                        .id(moment.getPlaceId())
                        .name(place != null ? place.getName() : "")
                        .build())
                .location(locationMap)
                .isPublic(moment.getIsPublic())
                .isOwner(isOwner)
                .createdAt(moment.getCreatedAt())
                .commentsCount(0) // TODO: 댓글 기능 구현 후 설정
                .build();
    }
}
