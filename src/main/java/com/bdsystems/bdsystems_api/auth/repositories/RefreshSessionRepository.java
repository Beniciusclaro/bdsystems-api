package com.bdsystems.bdsystems_api.auth.repositories;

import com.bdsystems.bdsystems_api.auth.domains.entities.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
  Optional<RefreshSession> findByTokenHash(String tokenHash);

  @Modifying
  @Query("update RefreshSession refreshSession set refreshSession.lastUsedAt = :usedAt where refreshSession.id = :sessionId and refreshSession.revokedAt is null")
  int updateLastUsedAt(@Param("sessionId") UUID sessionId,
    @Param("usedAt") Instant usedAt);

  @Modifying
  @Query("update RefreshSession refreshSession set refreshSession.revokedAt = :revokedAt where refreshSession.tokenHash = :tokenHash and refreshSession.revokedAt is null")
  int revokeByTokenHash(@Param("tokenHash") String tokenHash,
    @Param("revokedAt") Instant revokedAt);

  @Modifying
  @Query("update RefreshSession refreshSession set refreshSession.revokedAt = :revokedAt where refreshSession.user.id = :userId and refreshSession.revokedAt is null")
  int revokeAllByUserId(@Param("userId") UUID userId,
    @Param("revokedAt") Instant revokedAt);
}
