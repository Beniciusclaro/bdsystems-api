package com.bdsystems.bdsystems_api.materials.domains.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record MaterialRequest(
  @NotBlank @Size(max = 50) String code,
  @NotBlank @Size(max = 150) String name,
  String description,
  @NotNull UUID unitOfMeasureId,
  UUID categoryId,
  Boolean active,
  List<@NotNull @Valid SupplierMaterialRequest> suppliers) {
}
