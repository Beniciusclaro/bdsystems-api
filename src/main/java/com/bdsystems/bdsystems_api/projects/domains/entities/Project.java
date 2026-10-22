package com.bdsystems.bdsystems_api.projects.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.clients.domains.entities.Client;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id")
  private Company company;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id")
  private Client client;
  @Column(nullable = false, length = 50)
  private String code;
  @Column(nullable = false, length = 180)
  private String name;
  @Column(columnDefinition = "text")
  private String description;
  @Column(length = 180)
  private String street;
  @Column(length = 100)
  private String city;
  @Column(length = 100)
  private String state;
  @Column(name = "zip_code", length = 20)
  private String zipCode;
  @Column(name = "planned_start_date")
  private LocalDate plannedStartDate;
  @Column(name = "planned_end_date")
  private LocalDate plannedEndDate;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private ProjectStatus status = ProjectStatus.PLANNING;
  @Column(nullable = false)
  private boolean active = true;
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  public Project(Company company) {
    this.company = company;
  }

  public static Project planning(Company company,
    Client client,
    String code,
    String name,
    String description,
    String street,
    String city,
    String state,
    String zipCode,
    LocalDate plannedStartDate,
    LocalDate plannedEndDate) {
    Project project = new Project(company);
    project.update(client,
      code,
      name,
      description,
      street,
      city,
      state,
      zipCode,
      plannedStartDate,
      plannedEndDate,
      ProjectStatus.PLANNING,
      true);
    return project;
  }

  public void update(Client client,
    String code,
    String name,
    String description,
    String street,
    String city,
    String state,
    String zip,
    LocalDate start,
    LocalDate end,
    ProjectStatus status,
    boolean active) {
    this.client = client;
    this.code = code;
    this.name = name;
    this.description = description;
    this.street = street;
    this.city = city;
    this.state = state;
    this.zipCode = zip;
    this.plannedStartDate = start;
    this.plannedEndDate = end;
    this.status = status;
    this.active = active;
  }

  @PrePersist
  void create() {
    createdAt = OffsetDateTime.now();
  }

  @PreUpdate
  void touch() {
    updatedAt = OffsetDateTime.now();
  }
}
