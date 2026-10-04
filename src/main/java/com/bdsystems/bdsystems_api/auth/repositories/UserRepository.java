package com.bdsystems.bdsystems_api.auth.repositories;

import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByCompanyIdAndEmail(UUID companyId, String email);
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
