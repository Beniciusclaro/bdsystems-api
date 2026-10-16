package com.bdsystems.bdsystems_api.supplyer.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Address;
import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "suppliers")
@NoArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    private String tradeName;

    @Column(name = "tax_number")
    private String taxNumber;

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    private Address address;

    private boolean active = true;

    public Supplier(
            Company company,
            String legalName,
            String tradeName,
            String taxNumber,
            String email,
            String phoneNumber,
            Address address
    ) {
        this.company = company;
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.taxNumber = taxNumber;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public void updateDetails(
            String legalName,
            String tradeName,
            String taxNumber,
            String email,
            String phoneNumber
    ) {
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.taxNumber = taxNumber;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public void markAsDeleted() {
        this.active = false;
    }
}