package com.bdsystems.bdsystems_api.compositions.domains.dtos;

import com.bdsystems.bdsystems_api.compositions.domains.entities.ComponentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CompositionDtos {
  private CompositionDtos() {
  }

  public record ComponentRequest(
    @NotNull ComponentType type,
    UUID materialId,
    UUID supplierMaterialId,
    UUID laborRoleId,
    UUID toolRentalId,
    @Size(max = 220) String description,
    @Size(max = 30) String unitLabel,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
    @DecimalMin("0") BigDecimal unitCost,
    Integer sortOrder
  ) {
  }

  public record Request(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 180) String name,
    String description,
    @NotNull UUID unitOfMeasureId,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal baseYield,
    Boolean active,
    @NotEmpty List<@Valid ComponentRequest> components
  ) {
  }

  public record ComponentResponse(
    UUID id,
    ComponentType type,
    UUID referenceId,
    String description,
    String unitLabel,
    BigDecimal quantity,
    BigDecimal unitCost,
    BigDecimal totalCost,
    int sortOrder
  ) {
  }

  public record Response(
    UUID id,
    String code,
    String name,
    String description,
    UUID unitOfMeasureId,
    String unitSymbol,
    BigDecimal baseYield,
    boolean active,
    List<ComponentResponse> components,
    BigDecimal totalCost,
    BigDecimal unitCost
  ) {
  }
}
