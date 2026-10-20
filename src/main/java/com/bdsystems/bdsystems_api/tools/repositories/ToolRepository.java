package com.bdsystems.bdsystems_api.tools.repositories;

import com.bdsystems.bdsystems_api.tools.domains.entities.Tool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ToolRepository extends JpaRepository<Tool, UUID> {
  @Query("""
    SELECT tool FROM Tool tool
    WHERE tool.company.id = :companyId
      AND (:active IS NULL OR tool.active = :active)
      AND (:query = '' OR LOWER(tool.code) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(tool.name) LIKE LOWER(CONCAT('%', :query, '%')))
    """)
  Page<Tool> findByFilters(
    @Param("companyId") UUID companyId,
    @Param("active") Boolean active,
    @Param("query") String query,
    Pageable pageable
  );

  Optional<Tool> findByIdAndCompanyId(UUID id,
    UUID companyId);

  Optional<Tool> findByIdAndCompanyIdAndActiveTrue(UUID id,
    UUID companyId);

  boolean existsByCompanyIdAndCode(UUID companyId,
    String code);

  boolean existsByCompanyIdAndCodeAndIdNot(UUID companyId,
    String code,
    UUID id);
}
