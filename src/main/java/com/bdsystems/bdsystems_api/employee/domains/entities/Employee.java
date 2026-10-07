package com.bdsystems.bdsystems_api.employee.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.domains.entities.Person;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(
    name = "employees",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_employees_company_number",
        columnNames = {"company_id", "employee_number"}
    )
)
@NoArgsConstructor
public class Employee extends Person {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(name = "employee_number", nullable = false, length = 100)
  private String employeeNumber;

  @Column(nullable = false, length = 150)
  private String position;

  @Column(nullable = false, length = 150)
  private String department;

  @Column(name = "admission_date", nullable = false)
  private LocalDate admissionDate;

  @Column(name = "termination_date")
  private LocalDate terminationDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private EmployeeStatus status;

  public Employee(
      String fullName,
      String email,
      String phoneNumber,
      String documentNumber,
      LocalDate birthDate,
      Address address,
      Company company,
      String employeeNumber,
      String position,
      String department,
      LocalDate admissionDate,
      LocalDate terminationDate,
      EmployeeStatus status
  ) {
    super(fullName, email, phoneNumber, documentNumber, birthDate, address);
    this.company = company;
    this.employeeNumber = employeeNumber;
    this.position = position;
    this.department = department;
    this.admissionDate = admissionDate;
    this.terminationDate = terminationDate;
    this.status = status;
  }
}
