package com.dolpin.domain.moment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "moment_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MomentImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moment_id", nullable = false)
    @Setter
    private Moment moment;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "image_sequence", nullable = false)
    @Builder.Default
    private Integer imageSequence = 0;

    // 실제 사용되는 비즈니스 메서드
    public void updateImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            this.imageUrl = imageUrl;
        }
    }

    public void updateSequence(Integer sequence) {
        if (sequence != null && sequence >= 0) {
            this.imageSequence = sequence;
        }
    }

    // 비즈니스 로직 메서드
    public boolean hasValidUrl() {
        return this.imageUrl != null && !this.imageUrl.trim().isEmpty();
    }
}
