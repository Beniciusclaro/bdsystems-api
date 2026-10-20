package com.bdsystems.bdsystems_api.compositions.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.materials.domains.entities.UnitOfMeasure;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "compositions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Composition {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id")
  private Company company;
  @Column(nullable = false, length = 50)
  private String code;
  @Column(nullable = false, length = 180)
  private String name;
  @Column(columnDefinition = "text")
  private String description;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "unit_of_measure_id")
  private UnitOfMeasure unitOfMeasure;
  @Column(name = "base_yield", nullable = false, precision = 19, scale = 4)
  private BigDecimal baseYield;
  @Column(nullable = false)
  private boolean active = true;
  @OneToMany(mappedBy = "composition", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder asc")
  private List<CompositionComponent> components = new ArrayList<>();
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  public Composition(Company c) {
    company = c;
  }

  public void update(String code,
    String name,
    String desc,
    UnitOfMeasure unit,
    BigDecimal yield,
    boolean active) {
    this.code = code;
    this.name = name;
    description = desc;
    unitOfMeasure = unit;
    baseYield = yield;
    this.active = active;
  }

  public void replaceComponents(List<CompositionComponent> values) {
    components.clear();
    values.forEach(v -> {
      v.attach(this);
      components.add(v);
    });
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
