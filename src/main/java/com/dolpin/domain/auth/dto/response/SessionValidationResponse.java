package com.dolpin.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionValidationResponse {
    private boolean isValid;

    public static SessionValidationResponse of(boolean isValid) {
        return SessionValidationResponse.builder()
                .isValid(isValid)
                .build();
    }
}
