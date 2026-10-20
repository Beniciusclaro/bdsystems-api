package com.bdsystems.bdsystems_api.budgets.domains.entities;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.clients.domains.entities.Client;
import com.bdsystems.bdsystems_api.projects.domains.entities.Project;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "budgets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id")
  private Company company;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id")
  private Client client;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id")
  private Project project;
  @Column(nullable = false, length = 50)
  private String number;
  @Column(nullable = false, length = 180)
  private String title;
  @Column(name = "valid_until")
  private LocalDate validUntil;
  @Column(name = "commercial_terms", columnDefinition = "text")
  private String commercialTerms;
  @Column(columnDefinition = "text")
  private String notes;
  @Column(name = "tax_percent", nullable = false, precision = 7, scale = 4)
  private BigDecimal taxPercent;
  @Column(name = "margin_percent", nullable = false, precision = 7, scale = 4)
  private BigDecimal marginPercent;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BudgetStatus status = BudgetStatus.DRAFT;
  @Column(name = "client_name_snapshot", length = 180)
  private String clientNameSnapshot;
  @Column(name = "client_document_snapshot", length = 32)
  private String clientDocumentSnapshot;
  @Column(name = "project_name_snapshot", length = 180)
  private String projectNameSnapshot;
  @Column(name = "project_address_snapshot", length = 500)
  private String projectAddressSnapshot;
  @Column(name = "finalized_at")
  private OffsetDateTime finalizedAt;
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;
  @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder asc")
  private List<BudgetStage> stages = new ArrayList<>();

  public Budget(Company c) {
    company = c;
  }

  public void update(Client c,
    Project p,
    String number,
    String title,
    LocalDate valid,
    String terms,
    String notes,
    BigDecimal tax,
    BigDecimal margin) {
    client = c;
    project = p;
    this.number = number;
    this.title = title;
    validUntil = valid;
    commercialTerms = terms;
    this.notes = notes;
    taxPercent = tax;
    marginPercent = margin;
  }

  public void addStage(BudgetStage s) {
    s.attach(this);
    stages.add(s);
  }

  public void finalizeBudget() {
    status = BudgetStatus.FINALIZED;
    finalizedAt = OffsetDateTime.now();
    clientNameSnapshot = client.getName();
    clientDocumentSnapshot = client.getDocument();
    projectNameSnapshot = project.getName();
    projectAddressSnapshot = String.join(", ",
      java.util.stream.Stream.of(project.getStreet(),
        project.getCity(),
        project.getState(),
        project.getZipCode()).filter(Objects::nonNull).filter(v -> !v.isBlank()).toList());
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
