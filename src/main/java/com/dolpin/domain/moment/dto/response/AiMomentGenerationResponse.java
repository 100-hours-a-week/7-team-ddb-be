package com.dolpin.domain.moment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiMomentGenerationResponse {
    private String title;
    private String content;
    private Long placeId;
    private List<String> images;
    private Boolean isPublic;
}
