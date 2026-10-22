package com.bdsystems.bdsystems_api.compositions.domains.entities;

import com.bdsystems.bdsystems_api.labor.domains.entities.LaborRole;
import com.bdsystems.bdsystems_api.materials.domains.entities.*;
import com.bdsystems.bdsystems_api.tools.domains.entities.ToolRental;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "composition_components")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompositionComponent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "composition_id")
  private Composition composition;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private ComponentType type;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_id")
  private Material material;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_material_id")
  private SupplierMaterial supplierMaterial;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "labor_role_id")
  private LaborRole laborRole;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tool_rental_id")
  private ToolRental toolRental;
  @Column(length = 220)
  private String description;
  @Column(name = "unit_label", length = 30)
  private String unitLabel;
  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal quantity;
  @Column(name = "custom_unit_cost", precision = 19, scale = 4)
  private BigDecimal customUnitCost;
  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public CompositionComponent(ComponentType type,
    Material material,
    SupplierMaterial supplierMaterial,
    LaborRole laborRole,
    ToolRental toolRental,
    String description,
    String unitLabel,
    BigDecimal quantity,
    BigDecimal customUnitCost,
    int sortOrder) {
    update(type,
      material,
      supplierMaterial,
      laborRole,
      toolRental,
      description,
      unitLabel,
      quantity,
      customUnitCost,
      sortOrder);
  }

  public void update(ComponentType type,
    Material material,
    SupplierMaterial supplierMaterial,
    LaborRole laborRole,
    ToolRental toolRental,
    String description,
    String unitLabel,
    BigDecimal quantity,
    BigDecimal customUnitCost,
    int sortOrder) {
    this.type = type;
    this.material = material;
    this.supplierMaterial = supplierMaterial;
    this.laborRole = laborRole;
    this.toolRental = toolRental;
    this.description = description;
    this.unitLabel = unitLabel;
    this.quantity = quantity;
    this.customUnitCost = customUnitCost;
    this.sortOrder = sortOrder;
  }

  void attach(Composition composition) {
    this.composition = composition;
  }
}
