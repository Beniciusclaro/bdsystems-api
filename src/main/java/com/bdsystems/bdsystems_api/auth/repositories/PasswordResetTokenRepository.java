package com.bdsystems.bdsystems_api.auth.repositories;

import com.bdsystems.bdsystems_api.auth.domains.entities.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  Optional<PasswordResetToken> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

  @Modifying
  @Query("""
    update PasswordResetToken resetToken
    set resetToken.consumedAt = :invalidatedAt
    where resetToken.user.id = :userId
      and resetToken.consumedAt is null
    """)
  void invalidateUnconsumedByUserId(@Param("userId") UUID userId,
    @Param("invalidatedAt") Instant invalidatedAt);
}
