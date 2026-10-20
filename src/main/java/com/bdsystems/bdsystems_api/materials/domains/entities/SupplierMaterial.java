package com.bdsystems.bdsystems_api.materials.domains.entities;

import com.bdsystems.bdsystems_api.supplyer.domains.entities.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "supplier_materials", uniqueConstraints = @UniqueConstraint(columnNames = {"material_id", "supplier_id"}))
public class SupplierMaterial {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "material_id", nullable = false)
  private Material material;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "supplier_id", nullable = false)
  private Supplier supplier;
  @Column(length = 100)
  private String supplierCode;
  @Column(precision = 12, scale = 2)
  private BigDecimal referencePrice;
  @Column(nullable = false)
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = OffsetDateTime.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public SupplierMaterial(Material material,
    Supplier supplier) {
    this.material = material;
    this.supplier = supplier;
  }

  public void updateDetails(String code,
    BigDecimal price) {
    this.supplierCode = code;
    this.referencePrice = price;
  }
}
