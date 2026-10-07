package com.bdsystems.bdsystems_api.auth.domains.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor
public class User extends Person {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  public User(UUID id, String name, String email, String password, Role role, Address address, Company company) {
    super(name, email, address);
    this.id = id;
    this.password = password;
    this.role = role;
    this.company = company;
  }

  public User(String email, String password) {
    super(null, email, null);
    this.password = password;
  }
}
