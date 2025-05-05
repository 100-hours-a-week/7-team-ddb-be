package com.dolpin.global.storage.dto.reseponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {
    private String signedUrl;
    private String objectUrl;
    private int expiresIn;
}