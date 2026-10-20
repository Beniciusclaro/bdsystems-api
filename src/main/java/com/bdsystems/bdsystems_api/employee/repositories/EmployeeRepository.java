package com.bdsystems.bdsystems_api.employee.repositories;

import com.bdsystems.bdsystems_api.employee.domains.entities.Employee;
import com.bdsystems.bdsystems_api.employee.domains.entities.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
  boolean existsByCompanyIdAndEmployeeNumber(UUID companyId,
    String employeeNumber);

  boolean existsByCompanyIdAndEmployeeNumberAndIdNot(
    UUID companyId,
    String employeeNumber,
    UUID id
  );

  Optional<Employee> findByIdAndCompanyIdAndEnableFalse(UUID id,
    UUID companyId);

  @Query("""
    SELECT employee FROM Employee employee
    WHERE employee.company.id = :companyId
      AND employee.enable = false
      AND (:status IS NULL OR employee.status = :status)
      AND (:position = '' OR LOWER(employee.position)
           LIKE LOWER(CONCAT('%', :position, '%')))
      AND (:department = '' OR LOWER(employee.department)
           LIKE LOWER(CONCAT('%', :department, '%')))
    """)
  Page<Employee> findByFilters(
    @Param("companyId") UUID companyId,
    @Param("status") EmployeeStatus status,
    @Param("position") String position,
    @Param("department") String department,
    Pageable pageable
  );
}
