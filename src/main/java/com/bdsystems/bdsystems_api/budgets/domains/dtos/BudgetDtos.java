package com.bdsystems.bdsystems_api.budgets.domains.dtos;

import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetStatus;
import com.bdsystems.bdsystems_api.compositions.domains.entities.ComponentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class BudgetDtos {
  private BudgetDtos() {
  }

  public record Request(
    @NotNull UUID clientId,
    @NotNull UUID projectId,
    @NotBlank @Size(max = 50) String number,
    @NotBlank @Size(max = 180) String title,
    LocalDate validUntil,
    String commercialTerms,
    String notes,
    @NotNull @DecimalMin("0") BigDecimal taxPercent,
    @NotNull @DecimalMin("0") BigDecimal marginPercent
  ) {
  }

  public record StageRequest(
    @NotBlank @Size(max = 180) String name,
    String description,
    Integer sortOrder
  ) {
  }

  public record ItemRequest(
    @NotNull UUID compositionId,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
    Integer sortOrder
  ) {
  }

  public record QuantityRequest(@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity) {
  }

  public record ComponentResponse(
    ComponentType type,
    String description,
    String unitLabel,
    BigDecimal quantity,
    BigDecimal unitCost,
    BigDecimal totalCost
  ) {
  }

  public record ItemResponse(
    UUID id,
    UUID sourceCompositionId,
    String code,
    String name,
    String description,
    String unitSymbol,
    BigDecimal quantity,
    BigDecimal unitCost,
    BigDecimal totalCost,
    BigDecimal unitSalePrice,
    BigDecimal totalSalePrice,
    int sortOrder,
    List<ComponentResponse> components
  ) {
  }

  public record StageResponse(
    UUID id,
    String name,
    String description,
    int sortOrder,
    BigDecimal totalCost,
    BigDecimal totalSalePrice,
    List<ItemResponse> items
  ) {
  }

  public record Response(
    UUID id,
    String number,
    String title,
    UUID clientId,
    String clientName,
    UUID projectId,
    String projectName,
    LocalDate validUntil,
    String commercialTerms,
    String notes,
    BigDecimal taxPercent,
    BigDecimal marginPercent,
    BudgetStatus status,
    OffsetDateTime finalizedAt,
    BigDecimal totalCost,
    BigDecimal totalSalePrice,
    List<StageResponse> stages
  ) {
  }

  public record ProposalItem(
    String name,
    String description,
    String unitSymbol,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal total
  ) {
  }

  public record ProposalStage(
    String name,
    String description,
    BigDecimal total,
    List<ProposalItem> items
  ) {
  }

  public record Proposal(
    String number,
    String title,
    String clientName,
    String projectName,
    LocalDate validUntil,
    String commercialTerms,
    String notes,
    BigDecimal total,
    List<ProposalStage> stages
  ) {
  }
}
