package com.dolpin.domain.moment.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MomentUpdateRequest {

    @Size(max = 50, message = "제목은 50자 이내여야 합니다")
    private String title;

    @Size(max = 1000, message = "내용은 1000자 이내여야 합니다")
    private String content;

    private Long placeId;

    @Size(max = 100, message = "장소명은 100자 이내여야 합니다")
    private String placeName;

    private List<String> images;

    private Boolean isPublic;
}
