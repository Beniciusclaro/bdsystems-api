package com.bdsystems.bdsystems_api.materials.repositories;

import com.bdsystems.bdsystems_api.materials.domains.entities.MaterialCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, UUID> {
  Optional<MaterialCategory> findByIdAndCompanyId(UUID id,
    UUID companyId);

  boolean existsByCompanyIdAndNameAndIdNot(UUID companyId,
    String name,
    UUID id);

  @Query("select c from MaterialCategory c where c.company.id = :companyId and (:active is null or c.active = :active)")
  Page<MaterialCategory> findByFilters(UUID companyId,
    Boolean active,
    Pageable pageable);
}
