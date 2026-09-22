package com.bdsystems.builder_api.auth.repositories;

import com.bdsystems.builder_api.auth.domains.entities.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
}