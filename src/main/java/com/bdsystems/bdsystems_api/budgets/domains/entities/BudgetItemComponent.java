package com.bdsystems.bdsystems_api.budgets.domains.entities;

import com.bdsystems.bdsystems_api.compositions.domains.entities.ComponentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_item_components")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BudgetItemComponent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "budget_item_id")
  private BudgetItem budgetItem;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private ComponentType type;
  @Column(nullable = false, length = 220)
  private String description;
  @Column(name = "unit_label", nullable = false, length = 30)
  private String unitLabel;
  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal quantity;
  @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
  private BigDecimal unitCost;
  @Column(name = "total_cost", nullable = false, precision = 19, scale = 4)
  private BigDecimal totalCost;
  @Column(name = "source_reference_id")
  private UUID sourceReferenceId;
  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public BudgetItemComponent(ComponentType t,
    String d,
    String u,
    BigDecimal q,
    BigDecimal cost,
    BigDecimal total,
    UUID ref,
    int order) {
    type = t;
    description = d;
    unitLabel = u;
    quantity = q;
    unitCost = cost;
    totalCost = total;
    sourceReferenceId = ref;
    sortOrder = order;
  }

  void attach(BudgetItem i) {
    budgetItem = i;
  }
}
