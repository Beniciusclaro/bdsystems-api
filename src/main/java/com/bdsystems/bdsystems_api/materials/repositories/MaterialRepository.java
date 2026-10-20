package com.bdsystems.bdsystems_api.materials.repositories;

import com.bdsystems.bdsystems_api.materials.domains.entities.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {
  @EntityGraph(attributePaths = {"unitOfMeasure", "category"})
  Optional<Material> findByIdAndCompanyId(UUID id,
    UUID companyId);

  Optional<Material> findByCodeAndCompanyId(String code,
    UUID companyId);

  boolean existsByCompanyIdAndCodeAndIdNot(UUID companyId,
    String code,
    UUID id);

  @EntityGraph(attributePaths = {"unitOfMeasure", "category"})
  @Query("""
    select m from Material m where m.company.id = :companyId
    and (:active is null or m.active = :active)
    and (:categoryId is null or m.category.id = :categoryId)
    and lower(m.name) like lower(concat('%', :name, '%'))
    and lower(m.code) like lower(concat('%', :code, '%'))
    """)
  Page<Material> findByFilters(UUID companyId,
    Boolean active,
    UUID categoryId,
    String name,
    String code,
    Pageable pageable);
}
