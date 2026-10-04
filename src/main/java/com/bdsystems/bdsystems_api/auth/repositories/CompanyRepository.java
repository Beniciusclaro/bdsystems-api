package com.bdsystems.bdsystems_api.auth.repositories;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

  Optional<Company> findByName(String name);
}
