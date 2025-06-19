package com.dolpin.domain.comment.dto.response;

import com.dolpin.domain.comment.entity.Comment;
import com.dolpin.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateResponse {
    private Long id;
    private UserDto user;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime createdAt;

    private Boolean isOwner;
    private Long momentId;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String nickname;
        private String profileImage;

        public static UserDto from(User user) {
            return UserDto.builder()
                    .id(user.getId())
                    .nickname(user.getUsername())
                    .profileImage(user.getImageUrl())
                    .build();
        }
    }

    public static CommentCreateResponse from(Comment comment, User user, boolean isOwner) {
        return CommentCreateResponse.builder()
                .id(comment.getId())
                .user(UserDto.from(user))
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .isOwner(isOwner)
                .momentId(comment.getMomentId())
                .build();
    }
}
