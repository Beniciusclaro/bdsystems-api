package com.bdsystems.bdsystems_api.clients.domains.entities;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Client {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id")
  private Company company;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ClientType type;
  @Column(nullable = false, length = 180)
  private String name;
  @Column(length = 32)
  private String document;
  @Column(length = 180)
  private String email;
  @Column(length = 40)
  private String phone;
  @Column(length = 180)
  private String street;
  @Column(length = 100)
  private String city;
  @Column(length = 100)
  private String state;
  @Column(name = "zip_code", length = 20)
  private String zipCode;
  @Column(nullable = false)
  private boolean active = true;
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  public Client(Company company) {
    this.company = company;
  }

  public void update(ClientType type,
    String name,
    String document,
    String email,
    String phone,
    String street,
    String city,
    String state,
    String zipCode,
    boolean active) {
    this.type = type;
    this.name = name;
    this.document = document;
    this.email = email;
    this.phone = phone;
    this.street = street;
    this.city = city;
    this.state = state;
    this.zipCode = zipCode;
    this.active = active;
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
