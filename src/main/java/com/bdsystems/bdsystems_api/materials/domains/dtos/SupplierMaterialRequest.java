package com.bdsystems.bdsystems_api.materials.domains.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierMaterialRequest(
  @NotNull UUID supplierId,
  @Size(max = 100) String supplierCode,
  @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal referencePrice) {
}
