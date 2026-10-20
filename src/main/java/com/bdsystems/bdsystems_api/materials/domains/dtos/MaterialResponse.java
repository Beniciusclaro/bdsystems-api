package com.bdsystems.bdsystems_api.materials.domains.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MaterialResponse(
  UUID id,
  UUID companyId,
  String code,
  String name,
  String description,
  UnitOfMeasureResponse unitOfMeasure,
  MaterialCategoryResponse category,
  boolean active,
  OffsetDateTime createdAt,
  OffsetDateTime updatedAt) {
}
