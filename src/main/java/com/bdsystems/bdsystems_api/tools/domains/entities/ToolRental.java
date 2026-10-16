package com.bdsystems.bdsystems_api.tools.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.supplyer.domains.entities.Supplier;
import com.bdsystems.bdsystems_api.tools.domains.RentalPeriod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "tool_rentals")
@NoArgsConstructor
public class ToolRental {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tool_id", nullable = false)
    private Tool tool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "rental_period", nullable = false, length = 20)
    private RentalPeriod rentalPeriod;

    @Column(name = "rental_start_date")
    private LocalDate rentalStartDate;

    @Column(name = "rental_end_date")
    private LocalDate rentalEndDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public ToolRental(
            Company company,
            Tool tool,
            Supplier supplier,
            BigDecimal price,
            RentalPeriod rentalPeriod,
            LocalDate rentalStartDate,
            LocalDate rentalEndDate
    ) {
        this.company = company;
        this.tool = tool;
        this.supplier = supplier;
        this.price = price;
        this.rentalPeriod = rentalPeriod;
        this.rentalStartDate = rentalStartDate;
        this.rentalEndDate = rentalEndDate;
    }

    public void updateDetails(
            Tool tool,
            Supplier supplier,
            BigDecimal price,
            RentalPeriod rentalPeriod,
            LocalDate rentalStartDate,
            LocalDate rentalEndDate
    ) {
        this.tool = tool;
        this.supplier = supplier;
        this.price = price;
        this.rentalPeriod = rentalPeriod;
        this.rentalStartDate = rentalStartDate;
        this.rentalEndDate = rentalEndDate;
    }

    void assignTool(Tool tool) {
        this.tool = tool;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

}