package com.bdsystems.bdsystems_api.supplyer.repositories;

import com.bdsystems.bdsystems_api.supplyer.domains.entities.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    Page<Supplier> findByCompanyIdAndActiveTrue(UUID companyId, Pageable pageable);

    Optional<Supplier> findByIdAndCompanyIdAndActiveTrue(UUID id, UUID companyId);

    boolean existsByCompanyIdAndTaxNumber(UUID companyId, String taxNumber);

    boolean existsByCompanyIdAndTaxNumberAndIdNot(UUID companyId, String taxNumber, UUID id);
}
