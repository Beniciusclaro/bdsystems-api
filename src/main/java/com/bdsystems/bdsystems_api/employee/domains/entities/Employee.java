package com.bdsystems.bdsystems_api.employee.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.domains.entities.Person;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(
  name = "employees",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_employees_company_number", columnNames = {"company_id", "employee_number"}),
    @UniqueConstraint(name = "uk_employees_email", columnNames = "email")
  }
)
@NoArgsConstructor
public class Employee extends Person {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(name = "employee_number", length = 100)
  private String employeeNumber;

  @Column(nullable = false)
  private boolean enable = false;

  @Column(length = 150)
  private String position;

  @Column(length = 150)
  private String department;

  @Column(name = "admission_date")
  private LocalDate admissionDate;

  @Column(name = "termination_date")
  private LocalDate terminationDate;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private EmployeeStatus status;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private PaymentMethod paymentMethod;

  private BigDecimal paymentAmount;

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
    EmployeeStatus status,
    PaymentMethod paymentMethod,
    BigDecimal paymentAmount
  ) {
    super(fullName,
      email,
      phoneNumber,
      documentNumber,
      birthDate,
      address);
    this.company = company;
    this.employeeNumber = employeeNumber;
    this.position = position;
    this.department = department;
    this.admissionDate = admissionDate;
    this.terminationDate = terminationDate;
    this.status = status;
    this.paymentMethod = paymentMethod;
    this.paymentAmount = paymentAmount;
  }

  public static Employee incomplete(String fullName,
    String email,
    Company company) {
    return new Employee(
      fullName,
      email,
      null,
      null,
      null,
      null,
      company,
      null,
      null,
      null,
      null,
      null,
      EmployeeStatus.ACTIVE,
      null,
      null
    );
  }

  public void updateEmploymentDetails(
    String employeeNumber,
    String position,
    String department,
    LocalDate admissionDate,
    LocalDate terminationDate,
    EmployeeStatus status,
    PaymentMethod paymentMethod,
    BigDecimal paymentAmount
  ) {
    this.employeeNumber = employeeNumber;
    this.position = position;
    this.department = department;
    this.admissionDate = admissionDate;
    this.terminationDate = terminationDate;
    this.status = status;
    this.paymentMethod = paymentMethod;
    this.paymentAmount = paymentAmount;
  }

  public void markAsDeleted() {
    this.enable = true;
  }
}
