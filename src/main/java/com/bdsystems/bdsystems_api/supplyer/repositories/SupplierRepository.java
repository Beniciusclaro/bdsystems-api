package com.bdsystems.bdsystems_api.supplyer.repositories;

import com.bdsystems.bdsystems_api.supplyer.domains.entities.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
  @Query("""
    select supplier
    from Supplier supplier
    where supplier.company.id = :companyId
      and supplier.active = true
      and (
        :name = ''
        or lower(supplier.legalName) like lower(concat('%', :name, '%'))
        or lower(coalesce(supplier.tradeName, '')) like lower(concat('%', :name, '%'))
      )
      and (
        :taxNumber = ''
        or lower(coalesce(supplier.taxNumber, '')) like lower(concat('%', :taxNumber, '%'))
      )
    """)
  Page<Supplier> findByFilters(UUID companyId,
    String name,
    String taxNumber,
    Pageable pageable);

  Optional<Supplier> findByIdAndCompanyIdAndActiveTrue(UUID id,
    UUID companyId);

  boolean existsByCompanyIdAndTaxNumber(UUID companyId,
    String taxNumber);

  boolean existsByCompanyIdAndTaxNumberAndIdNot(UUID companyId,
    String taxNumber,
    UUID id);
}
