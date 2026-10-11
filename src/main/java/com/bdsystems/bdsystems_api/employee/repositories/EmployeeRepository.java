package com.bdsystems.bdsystems_api.employee.repositories;

import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
  boolean existsByCompanyIdAndEmployeeNumber(UUID companyId, String employeeNumber);

  Page<Employee> findAllByCompanyId(UUID companyId, Pageable pageable);
}
