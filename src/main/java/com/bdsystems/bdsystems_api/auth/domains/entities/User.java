package com.bdsystems.bdsystems_api.auth.domains.entities;

import jakarta.persistence.*;
import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "users", uniqueConstraints = {
  @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
  @UniqueConstraint(name = "uk_users_employee", columnNames = "employee_id")
})
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

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id", nullable = false, unique = true)
  private Employee employee;

  public User(UUID id,
    String name,
    String email,
    String password,
    Role role,
    Address address,
    Company company,
    Employee employee) {
    super(name,
      email,
      address);
    this.id = id;
    this.password = password;
    this.role = role;
    this.company = company;
    this.employee = employee;
  }

  public static User invited(
    String name,
    String email,
    String unusablePassword,
    Role role,
    Company company,
    Employee employee
  ) {
    User user = new User(null,
      name,
      email,
      unusablePassword,
      role,
      null,
      company,
      employee);
    user.deactivate();
    return user;
  }

  public void acceptInvitation(String encodedPassword) {
    this.password = encodedPassword;
    activate();
  }
}
