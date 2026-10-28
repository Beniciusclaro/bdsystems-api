package com.bdsystems.bdsystems_api.auth.repositories;

import com.bdsystems.bdsystems_api.auth.domains.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByCompanyIdAndEmail(UUID companyId,
    String email);

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailAndActiveTrue(String email);

  boolean existsByEmail(String email);

  boolean existsByEmailAndEmployeeIdNot(String email,
    UUID employeeId);

  boolean existsByEmployeeIdAndActiveTrue(UUID employeeId);

  Optional<User> findByEmployeeId(UUID employeeId);
}
