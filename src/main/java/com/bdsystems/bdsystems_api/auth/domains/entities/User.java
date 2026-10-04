package com.bdsystems.bdsystems_api.auth.domains.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @OneToOne(optional = false)
  @JoinColumn(name = "address_id", nullable = false, unique = true)
  private Address address;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  public User(UUID id, String name, String email, String password, Role role, Address address, Company company) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = role;
    this.address = address;
    this.company = company;
  }

  public User(String email, String password) {
    this.email = email;
    this.password = password;
  }
}
