package com.dolpin.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false, unique = true)
    private Long providerId;

    @Column(name = "provider", nullable = false, length = 10)
    private String provider;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "introduction", length = 70)
    private String introduction;

    @Column(name = "username", nullable = false, unique = true, length = 10)
    private String username;

    @Column(name = "is_location_agreed", nullable = false)
    private boolean isLocationAgreed = false;

    @Column(name = "is_privacy_agreed", nullable = false)
    private boolean isPrivacyAgreed = false;

    @Column(name = "privacy_agreed_at")
    private ZonedDateTime privacyAgreedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Builder
    public User(Long providerId, String provider, String username) {
        this.providerId = providerId;
        this.provider = provider;
        this.username = username;
        this.isLocationAgreed = false;
        this.isPrivacyAgreed = false;
    }

    public void updateProfile(String username, String imageUrl, String introduction) {
        this.username = username;
        this.imageUrl = imageUrl;
        this.introduction = introduction;
    }

    public void agreeToTerms(boolean isPrivacyAgreed, boolean isLocationAgreed) {
        this.isPrivacyAgreed = isPrivacyAgreed;
        this.isLocationAgreed = isLocationAgreed;
        if (isPrivacyAgreed) {
            this.privacyAgreedAt = ZonedDateTime.now();
        }
    }
}