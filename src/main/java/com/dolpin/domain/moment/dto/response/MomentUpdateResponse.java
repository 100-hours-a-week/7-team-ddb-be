package com.dolpin.domain.moment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentUpdateResponse {
    private Long id;

    private LocalDateTime updatedAt;
}
