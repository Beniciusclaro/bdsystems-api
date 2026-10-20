package com.bdsystems.bdsystems_api.labor.domains.dtos;

import com.bdsystems.bdsystems_api.labor.domains.entities.LaborCostingMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public final class LaborDtos {
  private LaborDtos() {
  }

  public record RoleRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 150) String name,
    String description,
    @NotNull LaborCostingMode costingMode,
    @DecimalMin("0") BigDecimal manualHourlyCost,
    Boolean active
  ) {
  }

  public record RoleResponse(
    UUID id,
    String code,
    String name,
    String description,
    LaborCostingMode costingMode,
    BigDecimal manualHourlyCost,
    BigDecimal currentHourlyCost,
    int eligibleEmployees,
    boolean active
  ) {
  }

  public record ProfileRequest(
    @NotNull UUID laborRoleId,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal productiveHoursMonth,
    @DecimalMin(value = "0", inclusive = false) BigDecimal workDaysMonth,
    @NotNull @DecimalMin("0") BigDecimal employerChargesPercent,
    @NotNull @DecimalMin("0") BigDecimal monthlyBenefits,
    @NotNull @DecimalMin("0") BigDecimal otherMonthlyCosts
  ) {
  }

  public record ProfileResponse(
    UUID employeeId,
    UUID laborRoleId,
    BigDecimal productiveHoursMonth,
    BigDecimal workDaysMonth,
    BigDecimal employerChargesPercent,
    BigDecimal monthlyBenefits,
    BigDecimal otherMonthlyCosts,
    BigDecimal effectiveHourlyCost
  ) {
  }
}
