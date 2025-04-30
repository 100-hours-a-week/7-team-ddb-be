package com.dolpin.domain.user.service;

import com.dolpin.domain.user.entity.User;

import java.util.Optional;

public interface UserQueryService {
    Optional<User> findByProviderAndProviderId(String provider, Long providerId);
    User getUserById(Long id);
    boolean existsByUsername(String username);
}