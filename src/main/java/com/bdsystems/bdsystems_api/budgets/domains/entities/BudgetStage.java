package com.bdsystems.bdsystems_api.budgets.domains.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "budget_stages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BudgetStage {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "budget_id")
  private Budget budget;
  @Column(nullable = false, length = 180)
  private String name;
  @Column(columnDefinition = "text")
  private String description;
  @Column(name = "sort_order", nullable = false)
  private int sortOrder;
  @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder asc")
  private List<BudgetItem> items = new ArrayList<>();

  public BudgetStage(String n,
    String d,
    int o) {
    name = n;
    description = d;
    sortOrder = o;
  }

  void attach(Budget b) {
    budget = b;
  }

  public void update(String n,
    String d,
    int o) {
    name = n;
    description = d;
    sortOrder = o;
  }

  public void addItem(BudgetItem i) {
    i.attach(this);
    items.add(i);
  }
}
