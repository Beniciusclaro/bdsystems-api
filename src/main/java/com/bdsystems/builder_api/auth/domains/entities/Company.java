package com.bdsystems.builder_api.auth.domains.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToOne(optional = false)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    private Address address;

    public Company(String name, Address address) {
        this.name = name;
        this.address = address;
        this.createdAt = Instant.now();
    }
}
