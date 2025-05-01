package com.dolpin.domain.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    @Size(min = 2, max = 10, message = "닉네임은 2~10자 이내여야 합니다")
    private String nickname;

    private String profile_image;

    @Size(max = 70, message = "소개글은 70자 이내여야 합니다")
    private String introduction;
}