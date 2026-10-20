package com.bdsystems.bdsystems_api.budgets.repositories;

import com.bdsystems.bdsystems_api.budgets.domains.entities.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
  @Query("select b from Budget b where b.id=:id and b.company.id=:companyId")
  Optional<Budget> findDetailedByIdAndCompanyId(UUID id,
    UUID companyId);

  boolean existsByCompanyIdAndNumberAndIdNot(UUID companyId,
    String number,
    UUID id);

  Page<Budget> findByCompanyId(UUID companyId,
    Pageable pageable);
}
