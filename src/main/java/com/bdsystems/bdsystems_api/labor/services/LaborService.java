package com.bdsystems.bdsystems_api.labor.services;

import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import com.bdsystems.bdsystems_api.employee.domains.entities.PaymentMethod;
import com.bdsystems.bdsystems_api.employee.repositories.EmployeeRepository;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.ProfileRequest;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.ProfileListResponse;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.ProfileResponse;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.RoleRequest;
import com.bdsystems.bdsystems_api.labor.domains.dtos.LaborDtos.RoleResponse;
import com.bdsystems.bdsystems_api.labor.domains.entities.EmployeeLaborCost;
import com.bdsystems.bdsystems_api.labor.domains.entities.LaborCostingMode;
import com.bdsystems.bdsystems_api.labor.domains.entities.LaborRole;
import com.bdsystems.bdsystems_api.labor.repositories.EmployeeLaborCostRepository;
import com.bdsystems.bdsystems_api.labor.repositories.LaborRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class LaborService {
  private final LaborRoleRepository roles;
  private final EmployeeLaborCostRepository profiles;
  private final EmployeeRepository employees;
  private final CompanyRepository companies;
  private final LaborCostCalculator calculator;

  public LaborService(LaborRoleRepository r,
    EmployeeLaborCostRepository p,
    EmployeeRepository e,
    CompanyRepository c,
    LaborCostCalculator calculator) {
    roles = r;
    profiles = p;
    employees = e;
    companies = c;
    this.calculator = calculator;
  }

  @Transactional
  public RoleResponse create(RoleRequest r,
    UUID t) {
    return save(new LaborRole(companies.findById(t).orElseThrow(() -> error(HttpStatus.FORBIDDEN,
        "Authenticated company not found"))),
      r,
      t);
  }

  @Transactional
  public RoleResponse update(UUID id,
    RoleRequest r,
    UUID t) {
    return save(find(id,
        t),
      r,
      t);
  }

  @Transactional(readOnly = true)
  public Page<RoleResponse> list(UUID tenantId,
    String search,
    Boolean active,
    Pageable pageable) {
    return roles.search(tenantId,
      normalizeSearch(search),
      active,
      pageable).map(laborRole -> map(laborRole,
      tenantId));
  }

  @Transactional(readOnly = true)
  public Page<ProfileListResponse> listProfiles(UUID tenantId,
    String search,
    Boolean configured,
    Pageable pageable) {
    return employees.findLaborCostProfiles(tenantId,
      EmployeeStatus.ACTIVE,
      normalizeSearch(search),
      configured,
      pageable).map(employee -> profileListMap(employee,
      tenantId));
  }

  @Transactional
  public ProfileResponse saveProfile(UUID employeeId,
    ProfileRequest r,
    UUID t) {
    Employee e = employees.findByIdAndCompanyIdAndEnableFalse(employeeId,
      t).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Employee not found"));
    LaborRole role = find(r.laborRoleId(),
      t);
    if (e.getPaymentMethod() == null || e.getPaymentAmount() == null) throw error(HttpStatus.BAD_REQUEST,
      "Employee payment method and amount are required");
    if (e.getPaymentMethod() == PaymentMethod.DAILY && r.workDaysMonth() == null) throw error(HttpStatus.BAD_REQUEST,
      "Work days per month are required for daily payment");
    EmployeeLaborCost p = profiles.findByEmployeeIdAndEmployeeCompanyId(employeeId,
      t).orElseGet(() -> new EmployeeLaborCost(e));
    p.update(role,
      r.productiveHoursMonth(),
      r.workDaysMonth(),
      r.employerChargesPercent(),
      r.monthlyBenefits(),
      r.otherMonthlyCosts());
    profiles.save(p);
    return profileMap(p);
  }

  @Transactional(readOnly = true)
  public ProfileResponse profile(UUID employeeId,
    UUID t) {
    return profileMap(profiles.findByEmployeeIdAndEmployeeCompanyId(employeeId,
      t).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Labor cost profile not found")));
  }

  public BigDecimal hourlyCost(LaborRole role,
    UUID t) {
    if (role.getCostingMode() == LaborCostingMode.MANUAL) {
      if (role.getManualHourlyCost() == null) throw error(HttpStatus.UNPROCESSABLE_ENTITY,
        "Manual hourly cost is missing");
      return role.getManualHourlyCost();
    }
    List<EmployeeLaborCost> ps = eligible(role,
      t);
    if (ps.isEmpty()) throw error(HttpStatus.UNPROCESSABLE_ENTITY,
      "Labor role has no eligible employee cost profiles");
    return ps.stream().map(this::effective).reduce(BigDecimal.ZERO,
      BigDecimal::add).divide(BigDecimal.valueOf(ps.size()),
      4,
      RoundingMode.HALF_UP);
  }

  private RoleResponse save(LaborRole role,
    RoleRequest r,
    UUID t) {
    if (r.costingMode() == LaborCostingMode.MANUAL && r.manualHourlyCost() == null) throw error(HttpStatus.BAD_REQUEST,
      "Manual hourly cost is required");
    if (roles.existsByCompanyIdAndCodeAndIdNot(t,
      r.code().trim(),
      role.getId() == null ? new UUID(0,
        0) : role.getId())) throw error(HttpStatus.CONFLICT,
      "Labor role code already registered");
    role.update(r.code().trim(),
      r.name().trim(),
      r.description(),
      r.costingMode(),
      r.manualHourlyCost(),
      r.active() == null || r.active());
    roles.save(role);
    return map(role,
      t);
  }

  public LaborRole find(UUID id,
    UUID t) {
    return roles.findByIdAndCompanyId(id,
      t).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Labor role not found"));
  }

  private List<EmployeeLaborCost> eligible(LaborRole r,
    UUID t) {
    return profiles.findByLaborRoleIdAndEmployeeCompanyIdAndEmployeeEnableFalseAndEmployeeStatus(r.getId(),
      t,
      EmployeeStatus.ACTIVE).stream().filter(p -> p.getEmployee().getPaymentAmount() != null && p.getEmployee().getPaymentMethod() != null).toList();
  }

  private BigDecimal effective(EmployeeLaborCost p) {
    Employee e = p.getEmployee();
    return calculator.hourlyCost(e.getPaymentMethod(),
      e.getPaymentAmount(),
      p.getProductiveHoursMonth(),
      p.getWorkDaysMonth(),
      p.getEmployerChargesPercent(),
      p.getMonthlyBenefits(),
      p.getOtherMonthlyCosts());
  }

  private RoleResponse map(LaborRole r,
    UUID t) {
    List<EmployeeLaborCost> ps = eligible(r,
      t);
    BigDecimal cost = null;
    try {
      cost = hourlyCost(r,
        t);
    } catch (ResponseStatusException ignored) {
    }
    return new RoleResponse(r.getId(),
      r.getCode(),
      r.getName(),
      r.getDescription(),
      r.getCostingMode(),
      r.getManualHourlyCost(),
      cost,
      ps.size(),
      r.isActive());
  }

  private ProfileResponse profileMap(EmployeeLaborCost p) {
    return new ProfileResponse(p.getEmployee().getId(),
      p.getLaborRole().getId(),
      p.getProductiveHoursMonth(),
      p.getWorkDaysMonth(),
      p.getEmployerChargesPercent(),
      p.getMonthlyBenefits(),
      p.getOtherMonthlyCosts(),
      effective(p));
  }

  private ProfileListResponse profileListMap(Employee employee,
    UUID tenantId) {
    EmployeeLaborCost profile = profiles.findByEmployeeIdAndEmployeeCompanyId(employee.getId(),
      tenantId).orElse(null);
    LaborRole laborRole = profile == null ? null : profile.getLaborRole();
    return new ProfileListResponse(employee.getId(),
      employee.getEmployeeNumber(),
      employee.getFullName(),
      employee.getPosition(),
      employee.getPaymentMethod(),
      employee.getPaymentAmount(),
      profile != null,
      laborRole == null ? null : laborRole.getId(),
      laborRole == null ? null : laborRole.getCode(),
      laborRole == null ? null : laborRole.getName(),
      profile == null ? null : profile.getProductiveHoursMonth(),
      profile == null ? null : profile.getWorkDaysMonth(),
      profile == null ? null : profile.getEmployerChargesPercent(),
      profile == null ? null : profile.getMonthlyBenefits(),
      profile == null ? null : profile.getOtherMonthlyCosts(),
      effectiveIfCalculable(profile));
  }

  private BigDecimal effectiveIfCalculable(EmployeeLaborCost profile) {
    if (profile == null || profile.getEmployee().getPaymentMethod() == null || profile.getEmployee().getPaymentAmount() == null)
      return null;
    if (profile.getEmployee().getPaymentMethod() == PaymentMethod.DAILY && profile.getWorkDaysMonth() == null)
      return null;
    return effective(profile);
  }

  private String normalizeSearch(String search) {
    return search == null || search.isBlank() ? "" : search.trim();
  }

  private ResponseStatusException error(HttpStatus s,
    String m) {
    return new ResponseStatusException(s,
      m);
  }
}
