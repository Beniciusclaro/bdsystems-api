package com.bdsystems.bdsystems_api.materials.domains.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "units_of_measure")
public class UnitOfMeasure {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(nullable = false, unique = true, length = 10)
  private String code;
  @Column(nullable = false, length = 100)
  private String name;
  @Column(nullable = false, length = 10)
  private String symbol;
  @Column(nullable = false)
  private boolean active = true;

  public UnitOfMeasure(String code,
    String name,
    String symbol) {
    this.code = code;
    this.name = name;
    this.symbol = symbol;
  }
}
