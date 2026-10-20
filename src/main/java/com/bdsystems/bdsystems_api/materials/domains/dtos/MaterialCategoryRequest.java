package com.bdsystems.bdsystems_api.materials.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaterialCategoryRequest(
  @NotBlank @Size(max = 150) String name,
  String description,
  Boolean active) {
}
