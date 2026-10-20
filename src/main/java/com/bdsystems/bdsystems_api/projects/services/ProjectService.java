package com.bdsystems.bdsystems_api.projects.services;

import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.clients.domains.entities.Client;
import com.bdsystems.bdsystems_api.clients.repositories.ClientRepository;
import com.bdsystems.bdsystems_api.projects.domains.dtos.ProjectDtos.Request;
import com.bdsystems.bdsystems_api.projects.domains.dtos.ProjectDtos.Response;
import com.bdsystems.bdsystems_api.projects.domains.entities.Project;
import com.bdsystems.bdsystems_api.projects.domains.entities.ProjectStatus;
import com.bdsystems.bdsystems_api.projects.repositories.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ProjectService {
  private final ProjectRepository projects;
  private final ClientRepository clients;
  private final CompanyRepository companies;

  public ProjectService(ProjectRepository projects,
    ClientRepository clients,
    CompanyRepository companies) {
    this.projects = projects;
    this.clients = clients;
    this.companies = companies;
  }

  @Transactional
  public Response create(Request request,
    UUID tenantId) {
    return save(new Project(companies.findById(tenantId).orElseThrow(() -> error(HttpStatus.FORBIDDEN,
        "Authenticated company not found"))),
      request,
      tenantId);
  }

  @Transactional
  public Response update(UUID id,
    Request request,
    UUID tenantId) {
    return save(find(id,
        tenantId),
      request,
      tenantId);
  }

  @Transactional(readOnly = true)
  public Response get(UUID id,
    UUID tenantId) {
    return map(find(id,
      tenantId));
  }

  @Transactional(readOnly = true)
  public Page<Response> list(UUID tenantId,
    Boolean active,
    UUID clientId,
    String search,
    Pageable pageable) {
    return projects.search(tenantId,
      active,
      clientId,
      search == null ? "" : search,
      pageable).map(this::map);
  }

  private Response save(Project project,
    Request request,
    UUID tenantId) {
    if (request.plannedStartDate() != null && request.plannedEndDate() != null && request.plannedEndDate().isBefore(request.plannedStartDate()))
      throw error(HttpStatus.BAD_REQUEST,
        "Planned end date cannot be before start date");
    if (projects.existsByCompanyIdAndCodeAndIdNot(tenantId,
      request.code().trim(),
      project.getId() == null ? new UUID(0,
        0) : project.getId())) throw error(HttpStatus.CONFLICT,
      "Project code already registered");
    Client client = clients.findByIdAndCompanyId(request.clientId(),
      tenantId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Client not found"));
    project.update(client,
      request.code().trim(),
      request.name().trim(),
      request.description(),
      request.street(),
      request.city(),
      request.state(),
      request.zipCode(),
      request.plannedStartDate(),
      request.plannedEndDate(),
      request.status() == null ? ProjectStatus.PLANNING : request.status(),
      request.active() == null || request.active());
    return map(projects.save(project));
  }

  Project find(UUID id,
    UUID tenantId) {
    return projects.findByIdAndCompanyId(id,
      tenantId).orElseThrow(() -> error(HttpStatus.NOT_FOUND,
      "Project not found"));
  }

  private Response map(Project project) {
    return new Response(project.getId(),
      project.getClient().getId(),
      project.getClient().getName(),
      project.getCode(),
      project.getName(),
      project.getDescription(),
      project.getStreet(),
      project.getCity(),
      project.getState(),
      project.getZipCode(),
      project.getPlannedStartDate(),
      project.getPlannedEndDate(),
      project.getStatus(),
      project.isActive(),
      project.getCreatedAt(),
      project.getUpdatedAt());
  }

  private ResponseStatusException error(HttpStatus status,
    String message) {
    return new ResponseStatusException(status,
      message);
  }
}
