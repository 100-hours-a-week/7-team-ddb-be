package com.dolpin.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementResponse {
    private Long userId;
    private Boolean privacyAgreed;
    private Boolean locationAgreed;
    private LocalDateTime privacyAgreedAt;
}
