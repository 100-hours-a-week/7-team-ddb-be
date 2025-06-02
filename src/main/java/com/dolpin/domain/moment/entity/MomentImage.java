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

    // 비즈니스 메서드
    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void updateSequence(Integer sequence) {
        this.imageSequence = sequence;
    }
}
