package com.bdsystems.bdsystems_api.materials.repositories;

import com.bdsystems.bdsystems_api.materials.domains.entities.SupplierMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplierMaterialRepository extends JpaRepository<SupplierMaterial, UUID> {
  @EntityGraph(attributePaths = "supplier")
  Page<SupplierMaterial> findByMaterialIdAndMaterialCompanyId(UUID materialId,
    UUID companyId,
    Pageable pageable);

  Optional<SupplierMaterial> findByIdAndMaterialIdAndMaterialCompanyId(UUID id,
    UUID materialId,
    UUID companyId);

  Optional<SupplierMaterial> findByMaterialIdAndSupplierId(UUID materialId,
    UUID supplierId);

  boolean existsByMaterialIdAndSupplierId(UUID materialId,
    UUID supplierId);
}
