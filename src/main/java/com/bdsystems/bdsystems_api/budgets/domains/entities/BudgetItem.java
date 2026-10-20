package com.bdsystems.bdsystems_api.budgets.domains.entities;

import com.bdsystems.bdsystems_api.compositions.domains.entities.Composition;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "budget_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BudgetItem {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stage_id")
  private BudgetStage stage;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_composition_id")
  private Composition sourceComposition;
  @Column(name = "composition_code_snapshot", length = 50)
  private String compositionCodeSnapshot;
  @Column(nullable = false, length = 180)
  private String name;
  @Column(columnDefinition = "text")
  private String description;
  @Column(name = "unit_symbol", nullable = false, length = 20)
  private String unitSymbol;
  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal quantity;
  @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
  private BigDecimal unitCost;
  @Column(name = "total_cost", nullable = false, precision = 19, scale = 4)
  private BigDecimal totalCost;
  @Column(name = "unit_sale_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal unitSalePrice;
  @Column(name = "total_sale_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal totalSalePrice;
  @Column(name = "sort_order", nullable = false)
  private int sortOrder;
  @OneToMany(mappedBy = "budgetItem", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder asc")
  private List<BudgetItemComponent> components = new ArrayList<>();

  public BudgetItem(Composition c,
    String code,
    String n,
    String d,
    String unit,
    BigDecimal q,
    BigDecimal unitCost,
    int order) {
    sourceComposition = c;
    compositionCodeSnapshot = code;
    name = n;
    description = d;
    unitSymbol = unit;
    quantity = q;
    this.unitCost = unitCost;
    sortOrder = order;
  }

  void attach(BudgetStage s) {
    stage = s;
  }

  public void replaceComponents(List<BudgetItemComponent> values) {
    components.clear();
    values.forEach(v -> {
      v.attach(this);
      components.add(v);
    });
  }

  public void recalculate(BigDecimal denominator) {
    totalCost = unitCost.multiply(quantity).setScale(4,
      java.math.RoundingMode.HALF_UP);
    unitSalePrice = unitCost.divide(denominator,
      4,
      java.math.RoundingMode.HALF_UP);
    totalSalePrice = unitSalePrice.multiply(quantity).setScale(4,
      java.math.RoundingMode.HALF_UP);
  }

  public void changeQuantity(BigDecimal q,
    BigDecimal d) {
    quantity = q;
    recalculate(d);
  }

  public void refresh(Composition c,
    String code,
    String n,
    String d,
    String unit,
    BigDecimal cost,
    BigDecimal denominator) {
    sourceComposition = c;
    compositionCodeSnapshot = code;
    name = n;
    description = d;
    unitSymbol = unit;
    unitCost = cost;
    recalculate(denominator);
  }
}
