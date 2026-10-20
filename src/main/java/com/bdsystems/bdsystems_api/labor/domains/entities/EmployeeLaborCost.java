package com.bdsystems.bdsystems_api.labor.domains.entities;

import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "employee_labor_costs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeLaborCost {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id", unique = true)
  private Employee employee;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "labor_role_id")
  private LaborRole laborRole;
  @Column(name = "productive_hours_month", nullable = false, precision = 10, scale = 2)
  private BigDecimal productiveHoursMonth;
  @Column(name = "work_days_month", precision = 10, scale = 2)
  private BigDecimal workDaysMonth;
  @Column(name = "employer_charges_percent", nullable = false, precision = 7, scale = 4)
  private BigDecimal employerChargesPercent;
  @Column(name = "monthly_benefits", nullable = false, precision = 19, scale = 4)
  private BigDecimal monthlyBenefits;
  @Column(name = "other_monthly_costs", nullable = false, precision = 19, scale = 4)
  private BigDecimal otherMonthlyCosts;
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  public EmployeeLaborCost(Employee e) {
    employee = e;
  }

  public void update(LaborRole r,
    BigDecimal h,
    BigDecimal d,
    BigDecimal charges,
    BigDecimal benefits,
    BigDecimal other) {
    laborRole = r;
    productiveHoursMonth = h;
    workDaysMonth = d;
    employerChargesPercent = charges;
    monthlyBenefits = benefits;
    otherMonthlyCosts = other;
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
