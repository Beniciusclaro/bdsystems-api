package com.bdsystems.bdsystems_api.materials.domains.dtos;

import java.util.UUID;

public record SupplierMaterialResponse(
  UUID id,
  UUID materialId,
  UUID supplierId,
  String supplierName,
  String supplierCode,
  java.math.BigDecimal referencePrice,
  java.time.OffsetDateTime createdAt,
  java.time.OffsetDateTime updatedAt
) {
}
