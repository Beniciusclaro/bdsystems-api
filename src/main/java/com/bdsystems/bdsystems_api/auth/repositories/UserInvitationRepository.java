package com.bdsystems.bdsystems_api.auth.repositories;

import com.bdsystems.bdsystems_api.auth.domains.entities.UserInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<UserInvitation> findByTokenHash(String tokenHash);

  Optional<UserInvitation> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

  @Modifying
  @Query("""
    update UserInvitation invitation
    set invitation.consumedAt = :invalidatedAt
    where invitation.user.id = :userId
      and invitation.consumedAt is null
    """)
  void invalidateUnconsumedByUserId(@Param("userId") UUID userId,
    @Param("invalidatedAt") Instant invalidatedAt);
}
