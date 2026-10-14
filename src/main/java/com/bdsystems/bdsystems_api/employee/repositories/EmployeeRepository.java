package com.bdsystems.bdsystems_api.employee.repositories;

import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
  boolean existsByCompanyIdAndEmployeeNumber(UUID companyId, String employeeNumber);

  boolean existsByCompanyIdAndEmployeeNumberAndIdNot(
      UUID companyId,
      String employeeNumber,
      UUID id
  );

  Optional<Employee> findByIdAndCompanyId(UUID id, UUID companyId);

  Page<Employee> findAllByCompanyId(UUID companyId, Pageable pageable);
}
