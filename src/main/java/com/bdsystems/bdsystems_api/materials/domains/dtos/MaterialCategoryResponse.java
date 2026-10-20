package com.bdsystems.bdsystems_api.materials.domains.dtos;

import java.util.UUID;

public record MaterialCategoryResponse(
  UUID id,
  UUID companyId,
  String name,
  String description,
  boolean active) {
}
