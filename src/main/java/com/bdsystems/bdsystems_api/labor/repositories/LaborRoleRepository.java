package com.bdsystems.bdsystems_api.labor.repositories;

import com.bdsystems.bdsystems_api.labor.domains.entities.LaborRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface LaborRoleRepository extends JpaRepository<LaborRole, UUID> {
  Optional<LaborRole> findByIdAndCompanyId(UUID id,
    UUID companyId);

  boolean existsByCompanyIdAndCodeAndIdNot(UUID companyId,
    String code,
    UUID id);

  @Query("""
    select laborRole from LaborRole laborRole
    where laborRole.company.id = :companyId
      and (:active is null or laborRole.active = :active)
      and (:search = ''
        or lower(laborRole.code) like lower(concat('%', :search, '%'))
        or lower(laborRole.name) like lower(concat('%', :search, '%')))
    """)
  Page<LaborRole> search(UUID companyId,
    String search,
    Boolean active,
    Pageable pageable);
}
