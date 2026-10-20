package com.bdsystems.bdsystems_api.labor.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "labor_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LaborRole {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id")
  private Company company;
  @Column(nullable = false, length = 50)
  private String code;
  @Column(nullable = false, length = 150)
  private String name;
  @Column(columnDefinition = "text")
  private String description;
  @Enumerated(EnumType.STRING)
  @Column(name = "costing_mode", nullable = false, length = 24)
  private LaborCostingMode costingMode;
  @Column(name = "manual_hourly_cost", precision = 19, scale = 4)
  private BigDecimal manualHourlyCost;
  @Column(nullable = false)
  private boolean active = true;
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  public LaborRole(Company c) {
    company = c;
  }

  public void update(String c,
    String n,
    String d,
    LaborCostingMode m,
    BigDecimal cost,
    boolean a) {
    code = c;
    name = n;
    description = d;
    costingMode = m;
    manualHourlyCost = cost;
    active = a;
  }

  @PrePersist
  void create() {
    createdAt = OffsetDateTime.now();
  }

  @PreUpdate
  void touch() {
    updatedAt = OffsetDateTime.now();
  }
}
