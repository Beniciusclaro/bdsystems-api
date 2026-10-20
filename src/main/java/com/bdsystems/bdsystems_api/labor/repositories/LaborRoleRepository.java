package com.bdsystems.bdsystems_api.labor.repositories;

import com.bdsystems.bdsystems_api.labor.domains.entities.LaborRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LaborRoleRepository extends JpaRepository<LaborRole, UUID> {
  Optional<LaborRole> findByIdAndCompanyId(UUID id,
    UUID companyId);

  boolean existsByCompanyIdAndCodeAndIdNot(UUID companyId,
    String code,
    UUID id);

  Page<LaborRole> findByCompanyId(UUID companyId,
    Pageable pageable);
}
