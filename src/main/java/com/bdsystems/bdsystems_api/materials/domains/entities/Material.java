package com.bdsystems.bdsystems_api.materials.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
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

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "materials", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
public class Material {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;
  @Column(nullable = false, length = 50)
  private String code;
  @Column(nullable = false, length = 150)
  private String name;
  @Column(columnDefinition = "text")
  private String description;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "unit_of_measure_id", nullable = false)
  private UnitOfMeasure unitOfMeasure;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private MaterialCategory category;
  @Column(nullable = false)
  private boolean active = true;
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

  public Material(Company company) {
    this.company = company;
  }

  public void updateDetails(String code,
    String name,
    String description,
    UnitOfMeasure unit,
    MaterialCategory category,
    boolean active) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.unitOfMeasure = unit;
    this.category = category;
    this.active = active;
  }

  public void changeStatus(boolean active) {
    this.active = active;
  }
}
