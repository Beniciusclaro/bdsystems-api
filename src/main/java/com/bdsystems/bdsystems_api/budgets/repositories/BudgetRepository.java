package com.bdsystems.bdsystems_api.budgets.repositories;

import com.bdsystems.bdsystems_api.budgets.domains.entities.Budget;
import com.bdsystems.bdsystems_api.budgets.domains.entities.BudgetStatus;
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

  @Query("""
    select budget from Budget budget
    where budget.company.id = :companyId
      and (:status is null or budget.status = :status)
      and (:search = ''
        or lower(budget.number) like lower(concat('%', :search, '%'))
        or lower(budget.title) like lower(concat('%', :search, '%'))
        or lower(budget.client.name) like lower(concat('%', :search, '%'))
        or lower(budget.project.name) like lower(concat('%', :search, '%'))
        or lower(budget.proposedProjectName) like lower(concat('%', :search, '%')))
    """)
  Page<Budget> search(UUID companyId,
    String search,
    BudgetStatus status,
    Pageable pageable);
}
