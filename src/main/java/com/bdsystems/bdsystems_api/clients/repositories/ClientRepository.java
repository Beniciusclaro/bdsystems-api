package com.bdsystems.bdsystems_api.clients.repositories;

import com.bdsystems.bdsystems_api.clients.domains.entities.Client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
  Optional<Client> findByIdAndCompanyId(UUID id,
    UUID companyId);

  boolean existsByCompanyIdAndDocumentAndIdNot(UUID companyId,
    String document,
    UUID id);

  @Query("select c from Client c where c.company.id=:companyId and (:active is null or c.active=:active) and lower(c.name) like lower(concat('%',:search,'%'))")
  Page<Client> search(UUID companyId,
    Boolean active,
    String search,
    Pageable pageable);
}
