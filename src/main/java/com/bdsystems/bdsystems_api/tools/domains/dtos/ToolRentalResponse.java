package com.bdsystems.bdsystems_api.tools.domains.dtos;

import com.bdsystems.bdsystems_api.tools.domains.RentalPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ToolRentalResponse(
  UUID id,
  UUID companyId,
  UUID toolId,
  UUID supplierId,
  BigDecimal price,
  RentalPeriod rentalPeriod,
  LocalDate rentalStartDate,
  LocalDate rentalEndDate,
  OffsetDateTime createdAt,
  OffsetDateTime updatedAt
) {
}
