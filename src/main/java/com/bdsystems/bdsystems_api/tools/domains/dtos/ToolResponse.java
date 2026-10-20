package com.bdsystems.bdsystems_api.tools.domains.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ToolResponse(
  UUID id,
  UUID companyId,
  String code,
  String name,
  boolean active,
  OffsetDateTime createdAt,
  OffsetDateTime updatedAt
) {
}
