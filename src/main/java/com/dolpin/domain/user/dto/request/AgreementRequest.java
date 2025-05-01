package com.dolpin.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AgreementRequest {
    @NotNull(message = "개인정보 동의 여부는 필수입니다")
    private Boolean privacyAgreed;

    @NotNull(message = "위치정보 동의 여부는 필수입니다")
    private Boolean locationAgreed;
}