package com.bdsystems.bdsystems_api.labor.repositories;

import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import com.bdsystems.bdsystems_api.labor.domains.entities.EmployeeLaborCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeLaborCostRepository extends JpaRepository<EmployeeLaborCost, UUID> {
  Optional<EmployeeLaborCost> findByEmployeeIdAndEmployeeCompanyId(UUID employeeId,
    UUID companyId);

  List<EmployeeLaborCost> findByLaborRoleIdAndEmployeeCompanyIdAndEmployeeEnableFalseAndEmployeeStatus(
    UUID roleId,
    UUID companyId,
    EmployeeStatus status);
}
