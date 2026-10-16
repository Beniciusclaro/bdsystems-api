package com.bdsystems.bdsystems_api.tools.repositories;

import com.bdsystems.bdsystems_api.tools.domains.entities.ToolRental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ToolRentalRepository extends JpaRepository<ToolRental, UUID> {
    Page<ToolRental> findByCompanyId(UUID companyId, Pageable pageable);

    Optional<ToolRental> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<ToolRental> findByCompanyIdAndToolId(UUID companyId, UUID toolId, Pageable pageable);

    boolean existsByCompanyIdAndToolId(UUID companyId, UUID toolId);
}
