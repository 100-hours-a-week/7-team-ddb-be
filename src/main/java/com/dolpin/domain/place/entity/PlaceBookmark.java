package com.dolpin.domain.place.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "place_bookmark")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PlaceBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    // 정적 팩토리 메서드
    public static PlaceBookmark create(Long userId, Long placeId) {
        return PlaceBookmark.builder()
                .userId(userId)
                .placeId(placeId)
                .build();
    }
}
