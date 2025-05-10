package com.dolpin.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long providerId;

    @Column(length = 10, nullable = false)
    private String provider;

    @Column(length = 10, nullable = false, unique = true)
    private String username;

    @Column(length = 255)
    private String imageUrl;

    @Column(length = 70)
    private String introduction;

    @Column(nullable = false)
    private boolean isPrivacyAgreed;

    @Column(nullable = false)
    private boolean isLocationAgreed;

    @Column
    private LocalDateTime privacyAgreedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 생성 전 이벤트
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.isPrivacyAgreed = false;
        this.isLocationAgreed = false;
    }

    // 수정 전 이벤트
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 프로필 업데이트 메서드
    // 프로필 업데이트 메서드
    public void updateProfile(String username, String imageUrl, String introduction) {
        if (username != null && !username.isEmpty()) {
            this.username = username;
        }
        if (imageUrl != null) {  // imageUrl이 null이 아닐 때만 업데이트
            this.imageUrl = imageUrl;
        }
        if (introduction != null) {  // introduction도 동일하게 처리
            this.introduction = introduction;
        }
    }

    // 개인정보 동의 설정 메서드
    public void agreeToTerms(boolean isPrivacyAgreed, boolean isLocationAgreed) {
        this.isPrivacyAgreed = isPrivacyAgreed;
        this.isLocationAgreed = isLocationAgreed;

        if (isPrivacyAgreed) {
            this.privacyAgreedAt = LocalDateTime.now();
        }
    }
}