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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "material_categories", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "name"}))
public class MaterialCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;
  @Column(nullable = false, length = 150)
  private String name;
  @Column(columnDefinition = "text")
  private String description;
  @Column(nullable = false)
  private boolean active = true;

  public MaterialCategory(Company company,
    String name,
    String description,
    boolean active) {
    this.company = company;
    updateDetails(name,
      description,
      active);
  }

  public void updateDetails(String name,
    String description,
    boolean active) {
    this.name = name;
    this.description = description;
    this.active = active;
  }

  public void changeStatus(boolean active) {
    this.active = active;
  }
}
