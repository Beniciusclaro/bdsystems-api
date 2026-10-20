package com.bdsystems.bdsystems_api.projects.repositories;

import com.bdsystems.bdsystems_api.projects.domains.entities.Project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  Optional<Project> findByIdAndCompanyId(UUID id,
    UUID companyId);

  boolean existsByCompanyIdAndCodeAndIdNot(UUID companyId,
    String code,
    UUID id);

  @Query("select p from Project p where p.company.id=:companyId and (:active is null or p.active=:active) " +
    "and (:clientId is null or p.client.id=:clientId) and lower(p.name) like lower(concat('%',:search,'%'))")
  Page<Project> search(UUID companyId,
    Boolean active,
    UUID clientId,
    String search,
    Pageable pageable);
}
