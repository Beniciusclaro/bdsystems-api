package com.bdsystems.bdsystems_api.tools.domains.dtos;

import com.bdsystems.bdsystems_api.tools.domains.RentalPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ToolRentalRequest(
  @NotNull UUID toolId,
  @NotNull UUID supplierId,
  @NotNull @DecimalMin("0.00") BigDecimal price,
  @NotNull RentalPeriod rentalPeriod,
  LocalDate rentalStartDate,
  LocalDate rentalEndDate
) {
}
