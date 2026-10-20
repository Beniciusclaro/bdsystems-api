package com.bdsystems.bdsystems_api.tools.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ToolRequest(
  @NotBlank @Size(max = 50) String code,
  @NotBlank @Size(max = 150) String name
) {
}
