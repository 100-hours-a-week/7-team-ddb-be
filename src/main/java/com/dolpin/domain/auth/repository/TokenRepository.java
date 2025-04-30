package com.dolpin.domain.auth.repository;

import com.dolpin.domain.auth.entity.Token;
import com.dolpin.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    List<Token> findAllByUser(User user);
    void deleteAllByUser(User user);

    @Query("SELECT t FROM Token t WHERE t.user.id = :userId AND t.isRevoked = false AND t.expiredAt > CURRENT_TIMESTAMP")
    List<Token> findValidTokensByUserId(@Param("userId") Long userId);
}