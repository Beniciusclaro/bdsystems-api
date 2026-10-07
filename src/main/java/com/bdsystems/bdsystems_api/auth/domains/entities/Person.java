package com.bdsystems.bdsystems_api.auth.domains.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Person {
  @Column(name = "name", nullable = false, unique = true)
  private String fullName;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "document_number")
  private String documentNumber;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @OneToOne(optional = false)
  @JoinColumn(name = "address_id", nullable = false, unique = true)
  private Address address;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_date", nullable = false, updatable = false)
  private Date createdDate;

  @Column(name = "updated_at")
  private Date updatedAt;

  protected Person(String fullName, String email, Address address) {
    this(fullName, email, null, null, null, address);
  }

  protected Person(
      String fullName,
      String email,
      String phoneNumber,
      String documentNumber,
      LocalDate birthDate,
      Address address
  ) {
    this.fullName = fullName;
    this.email = email;
    this.phoneNumber = phoneNumber;
    this.documentNumber = documentNumber;
    this.birthDate = birthDate;
    this.address = address;
    this.createdDate = new Date();
  }

  @PrePersist
  protected void onCreate() {
    if (createdDate == null) {
      createdDate = new Date();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = new Date();
  }
}
