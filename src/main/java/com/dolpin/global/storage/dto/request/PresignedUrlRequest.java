package com.dolpin.global.storage.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {

    @NotBlank(message = "파일 이름은 필수입니다.")
    private String fileName;

    @NotBlank(message = "컨텐츠 타입은 필수입니다.")
    private String contentType;

    @NotBlank(message = "업로드 타입은 필수입니다.")
    private String uploadType;
}
