package com.bdsystems.bdsystems_api.employee.repositories;

import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
  boolean existsByCompanyIdAndEmployeeNumber(UUID companyId, String employeeNumber);
}
