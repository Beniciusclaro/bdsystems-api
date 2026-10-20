package com.bdsystems.bdsystems_api.clients.services;

import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.clients.domains.dtos.ClientDtos.Request;
import com.bdsystems.bdsystems_api.clients.domains.dtos.ClientDtos.Response;
import com.bdsystems.bdsystems_api.clients.domains.entities.Client;
import com.bdsystems.bdsystems_api.clients.repositories.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ClientService {
  private final ClientRepository clients;
  private final CompanyRepository companies;

  public ClientService(ClientRepository clients,
    CompanyRepository companies) {
    this.clients = clients;
    this.companies = companies;
  }

  @Transactional
  public Response create(Request request,
    UUID tenantId) {
    return save(new Client(company(tenantId)),
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
    String search,
    Pageable pageable) {
    return clients.search(tenantId,
      active,
      search == null ? "" : search,
      pageable).map(this::map);
  }

  @Transactional
  public void status(UUID id,
    UUID tenantId,
    boolean active) {
    Client client = find(id,
      tenantId);
    client.update(client.getType(),
      client.getName(),
      client.getDocument(),
      client.getEmail(),
      client.getPhone(),
      client.getStreet(),
      client.getCity(),
      client.getState(),
      client.getZipCode(),
      active);
  }

  Client find(UUID id,
    UUID tenantId) {
    return clients.findByIdAndCompanyId(id,
      tenantId).orElseThrow(() -> notFound("Client not found"));
  }

  private Response save(Client client,
    Request request,
    UUID tenantId) {
    String document = blankToNull(request.document());
    if (document != null && clients.existsByCompanyIdAndDocumentAndIdNot(tenantId,
      document,
      client.getId() == null ? new UUID(0,
        0) : client.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT,
      "Client document already registered");
    client.update(request.type(),
      request.name().trim(),
      document,
      blankToNull(request.email()),
      blankToNull(request.phone()),
      blankToNull(request.street()),
      blankToNull(request.city()),
      blankToNull(request.state()),
      blankToNull(request.zipCode()),
      request.active() == null || request.active());
    return map(clients.save(client));
  }

  private com.bdsystems.bdsystems_api.auth.domains.entities.Company company(UUID id) {
    return companies.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
      "Authenticated company not found"));
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Response map(Client client) {
    return new Response(client.getId(),
      client.getType(),
      client.getName(),
      client.getDocument(),
      client.getEmail(),
      client.getPhone(),
      client.getStreet(),
      client.getCity(),
      client.getState(),
      client.getZipCode(),
      client.isActive(),
      client.getCreatedAt(),
      client.getUpdatedAt());
  }

  private ResponseStatusException notFound(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND,
      message);
  }
}
