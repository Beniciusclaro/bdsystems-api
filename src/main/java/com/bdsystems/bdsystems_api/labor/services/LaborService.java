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
  private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
  private static final BigDecimal WEEKS_PER_YEAR = BigDecimal.valueOf(52);
  private static final BigDecimal MONTHLY_PRODUCTIVE_HOURS = BigDecimal.valueOf(160);
  private static final BigDecimal MONTHLY_WORK_DAYS = BigDecimal.valueOf(22);

  private final LaborRoleRepository roles;
  private final EmployeeLaborCostRepository profiles;
  private final EmployeeRepository employees;
  private final CompanyRepository companies;
  private final LaborCostCalculator calculator;

  public LaborService(LaborRoleRepository roles,
    EmployeeLaborCostRepository profiles,
    EmployeeRepository employees,
    CompanyRepository companies,
    LaborCostCalculator calculator) {
    this.roles = roles;
    this.profiles = profiles;
    this.employees = employees;
    this.companies = companies;
    this.calculator = calculator;
  }

  @Transactional
  public RoleResponse create(RoleRequest request,
    UUID tenantId) {
    LaborRole laborRole = new LaborRole(companies.findById(tenantId).orElseThrow(() -> error(HttpStatus.FORBIDDEN,
      "Authenticated company not found")));
    return save(laborRole,
      request,
      tenantId);
  }

  @Transactional
  public RoleResponse update(UUID id,
    RoleRequest request,
    UUID tenantId) {
    return save(find(id,
        tenantId),
      request,
      tenantId);
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
  public List<String> referencePositions(UUID tenantId) {
    return employees.findEligibleReferencePositions(tenantId,
      EmployeeStatus.ACTIVE);
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
    ProfileRequest request,
    UUID tenantId) {
    Employee employee = employees.findByIdAndCompanyIdAndEnableFalse(employeeId,
      tenantId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Employee not found"));
    LaborRole laborRole = find(request.laborRoleId(),
      tenantId);
    if (employee.getPaymentMethod() == null || employee.getPaymentAmount() == null) throw error(HttpStatus.BAD_REQUEST,
      "Employee payment method and amount are required");
    if (employee.getPaymentMethod() == PaymentMethod.DAILY && request.workDaysMonth() == null)
      throw error(HttpStatus.BAD_REQUEST,
        "Work days per month are required for daily payment");
    EmployeeLaborCost profile = profiles.findByEmployeeIdAndEmployeeCompanyId(employeeId,
      tenantId).orElseGet(() -> new EmployeeLaborCost(employee));
    profile.update(laborRole,
      request.productiveHoursMonth(),
      request.workDaysMonth(),
      request.employerChargesPercent(),
      request.monthlyBenefits(),
      request.otherMonthlyCosts());
    profiles.save(profile);
    return profileMap(profile);
  }

  @Transactional(readOnly = true)
  public ProfileResponse profile(UUID employeeId,
    UUID tenantId) {
    return profileMap(profiles.findByEmployeeIdAndEmployeeCompanyId(employeeId,
      tenantId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Labor cost profile not found")));
  }

  public BigDecimal hourlyCost(LaborRole laborRole,
    UUID tenantId) {
    if (laborRole.getCostingMode() == LaborCostingMode.MANUAL) {
      if (laborRole.getManualHourlyCost() == null) throw error(HttpStatus.UNPROCESSABLE_ENTITY,
        "Manual hourly cost is missing");
      return laborRole.getManualHourlyCost();
    }
    List<Employee> eligibleEmployees = eligible(laborRole,
      tenantId);
    if (eligibleEmployees.isEmpty()) throw error(HttpStatus.UNPROCESSABLE_ENTITY,
      "Labor role has no eligible employees");
    return averageHourlyCost(eligibleEmployees);
  }

  private BigDecimal averageHourlyCost(List<Employee> eligibleEmployees) {
    return eligibleEmployees.stream().map(this::employeeHourlyCost).reduce(BigDecimal.ZERO,
      BigDecimal::add).divide(BigDecimal.valueOf(eligibleEmployees.size()),
      4,
      RoundingMode.HALF_UP);
  }

  private RoleResponse save(LaborRole laborRole,
    RoleRequest request,
    UUID tenantId) {
    validateCostSource(request);
    if (roles.existsByCompanyIdAndCodeAndIdNot(tenantId,
      request.code().trim(),
      laborRole.getId() == null ? new UUID(0,
        0) : laborRole.getId())) throw error(HttpStatus.CONFLICT,
      "Labor role code already registered");
    String referencePosition = request.referencePosition() == null ? null : request.referencePosition().trim();
    laborRole.update(request.code().trim(),
      request.name().trim(),
      request.description(),
      request.costingMode(),
      request.manualHourlyCost(),
      referencePosition,
      request.active() == null || request.active());
    roles.save(laborRole);
    return map(laborRole,
      tenantId);
  }

  private void validateCostSource(RoleRequest request) {
    if (request.costingMode() == LaborCostingMode.MANUAL) {
      if (request.manualHourlyCost() == null) throw error(HttpStatus.BAD_REQUEST,
        "Manual hourly cost is required");
      if (request.referencePosition() != null) throw error(HttpStatus.BAD_REQUEST,
        "Reference position must be null for manual costing");
      return;
    }
    if (request.manualHourlyCost() != null) throw error(HttpStatus.BAD_REQUEST,
      "Manual hourly cost must be null for employee average costing");
    if (request.referencePosition() == null || request.referencePosition().isBlank())
      throw error(HttpStatus.BAD_REQUEST,
        "Reference position is required for employee average costing");
  }

  public LaborRole find(UUID id,
    UUID tenantId) {
    return roles.findByIdAndCompanyId(id,
      tenantId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Labor role not found"));
  }

  private List<Employee> eligible(LaborRole laborRole,
    UUID tenantId) {
    if (laborRole.getCostingMode() != LaborCostingMode.EMPLOYEE_AVERAGE
      || laborRole.getReferencePosition() == null
      || laborRole.getReferencePosition().isBlank()) return List.of();
    return employees.findEligibleForAverage(tenantId,
      EmployeeStatus.ACTIVE,
      laborRole.getReferencePosition());
  }

  private BigDecimal employeeHourlyCost(Employee employee) {
    return switch (employee.getPaymentMethod()) {
      case MONTHLY -> employee.getPaymentAmount().divide(MONTHLY_PRODUCTIVE_HOURS,
        12,
        RoundingMode.HALF_UP);
      case WEEKLY -> employee.getPaymentAmount().multiply(WEEKS_PER_YEAR).divide(MONTHS_PER_YEAR,
        12,
        RoundingMode.HALF_UP).divide(MONTHLY_PRODUCTIVE_HOURS,
        12,
        RoundingMode.HALF_UP);
      case DAILY -> employee.getPaymentAmount().multiply(MONTHLY_WORK_DAYS).divide(MONTHLY_PRODUCTIVE_HOURS,
        12,
        RoundingMode.HALF_UP);
      case HOURLY -> employee.getPaymentAmount();
    };
  }

  private RoleResponse map(LaborRole laborRole,
    UUID tenantId) {
    List<Employee> eligibleEmployees = eligible(laborRole,
      tenantId);
    BigDecimal currentHourlyCost = null;
    if (laborRole.getCostingMode() == LaborCostingMode.MANUAL) {
      currentHourlyCost = laborRole.getManualHourlyCost();
    } else if (!eligibleEmployees.isEmpty()) {
      currentHourlyCost = averageHourlyCost(eligibleEmployees);
    }
    return new RoleResponse(laborRole.getId(),
      laborRole.getCode(),
      laborRole.getName(),
      laborRole.getDescription(),
      laborRole.getCostingMode(),
      laborRole.getManualHourlyCost(),
      laborRole.getReferencePosition(),
      currentHourlyCost,
      eligibleEmployees.size(),
      laborRole.isActive());
  }

  private ProfileResponse profileMap(EmployeeLaborCost profile) {
    return new ProfileResponse(profile.getEmployee().getId(),
      profile.getLaborRole().getId(),
      profile.getProductiveHoursMonth(),
      profile.getWorkDaysMonth(),
      profile.getEmployerChargesPercent(),
      profile.getMonthlyBenefits(),
      profile.getOtherMonthlyCosts(),
      effective(profile));
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

  private BigDecimal effective(EmployeeLaborCost profile) {
    Employee employee = profile.getEmployee();
    return calculator.hourlyCost(employee.getPaymentMethod(),
      employee.getPaymentAmount(),
      profile.getProductiveHoursMonth(),
      profile.getWorkDaysMonth(),
      profile.getEmployerChargesPercent(),
      profile.getMonthlyBenefits(),
      profile.getOtherMonthlyCosts());
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

  private ResponseStatusException error(HttpStatus status,
    String message) {
    return new ResponseStatusException(status,
      message);
  }
}
