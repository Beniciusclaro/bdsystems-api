package com.bdsystems.bdsystems_api.tools.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(
  name = "tools",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_tools_company_code",
      columnNames = {"company_id", "code"}
    )
  }
)
public class Tool {

  @Id
  @UuidGenerator
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(nullable = false, length = 50)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "tool")
  private List<ToolRental> rentals = new ArrayList<>();

  protected Tool() {
  }

  public Tool(Company company,
    String code,
    String name) {
    this.company = company;
    this.code = code;
    this.name = name;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = OffsetDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public void addRental(ToolRental rental) {
    rentals.add(rental);
    rental.assignTool(this);
  }

  public void updateDetails(String code,
    String name) {
    this.code = code;
    this.name = name;
  }

  public void activate() {
    this.active = true;
  }

  public void deactivate() {
    this.active = false;
  }

}
