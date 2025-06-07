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

    @Column(name = "place_id", nullable = true)
    private Long placeId;

    @Column(name = "place_name", nullable = true, length = 100)
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

    // 조회수 컬럼 - 기본값 설정하고 nullable로 변경
    @Column(name = "view_count", nullable = true, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long viewCount = 0L;

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

        // view_count가 null이면 0으로 초기화
        if (this.viewCount == null) {
            this.viewCount = 0L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();

        // view_count가 null이면 0으로 초기화
        if (this.viewCount == null) {
            this.viewCount = 0L;
        }
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

    public void updatePlaceInfo(Long placeId, String placeName) {
        if (placeId != null) {
            this.placeId = placeId;
        }
        if (placeName != null && !placeName.trim().isEmpty()) {
            this.placeName = placeName;
        }
    }

    // 조회수 증가 메서드
    public void incrementViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 0L;
        }
        this.viewCount++;
    }

    // 이미지 관련 도메인 메서드들
    public void addImage(String imageUrl) {
        int nextSequence = this.images.size();
        MomentImage image = MomentImage.builder()
                .moment(this)
                .imageUrl(imageUrl)
                .imageSequence(nextSequence)
                .build();
        this.images.add(image);
    }

    public void addImages(List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                addImage(imageUrl);
            }
        }
    }

    public void clearImages() {
        this.images.clear();
    }

    public void replaceImages(List<String> newImageUrls) {
        clearImages();
        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            addImages(newImageUrls);
        }
    }

    public void removeImage(MomentImage image) {
        this.images.remove(image);
        image.setMoment(null);
        // 순서 재정렬
        reorderImageSequences();
    }

    public void reorderImages(List<MomentImage> newOrderImages) {
        this.images.clear();
        for (int i = 0; i < newOrderImages.size(); i++) {
            MomentImage image = newOrderImages.get(i);
            image.updateSequence(i);
            this.images.add(image);
            image.setMoment(this);
        }
    }

    public void togglePublic() {
        this.isPublic = !this.isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    // 내부 헬퍼 메서드
    private void reorderImageSequences() {
        for (int i = 0; i < this.images.size(); i++) {
            this.images.get(i).updateSequence(i);
        }
    }

    // 비즈니스 로직 메서드
    public boolean hasImages() {
        return !this.images.isEmpty();
    }

    public String getThumbnailUrl() {
        return hasImages() ? this.images.get(0).getImageUrl() : null;
    }

    public int getImageCount() {
        return this.images.size();
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public boolean canBeViewedBy(Long userId) {
        return this.isPublic || isOwnedBy(userId);
    }

    // view_count getter - null 체크 추가
    public Long getViewCount() {
        return this.viewCount != null ? this.viewCount : 0L;
    }
}
