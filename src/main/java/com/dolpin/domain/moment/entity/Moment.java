package com.dolpin.domain.moment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "moment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Moment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("imageSequence ASC")
    @Builder.Default
    private List<MomentImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 비즈니스 메서드
    public void updateContent(String title, String content, Boolean isPublic) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
        if (content != null && !content.trim().isEmpty()) {
            this.content = content;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }

    // 장소 정보 업데이트 (장소 이름 변경시 사용)
    public void updatePlaceInfo(Long placeId, String placeName) {
        if (placeId != null) {
            this.placeId = placeId;
        }
        if (placeName != null && !placeName.trim().isEmpty()) {
            this.placeName = placeName;
        }
    }

    // 이미지 추가
    public void addImage(MomentImage image) {
        this.images.add(image);
        image.setMoment(this);
    }

    // 이미지 제거
    public void removeImage(MomentImage image) {
        this.images.remove(image);
        image.setMoment(null);
    }

    // 이미지 순서 재정렬
    public void reorderImages(List<MomentImage> newOrderImages) {
        this.images.clear();
        for (int i = 0; i < newOrderImages.size(); i++) {
            MomentImage image = newOrderImages.get(i);
            image.updateSequence(i);
            this.images.add(image);
        }
    }

    // 공개 여부 변경
    public void togglePublic() {
        this.isPublic = !this.isPublic;
    }
}
