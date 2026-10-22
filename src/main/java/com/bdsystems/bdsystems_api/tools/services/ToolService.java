package com.bdsystems.bdsystems_api.tools.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.tools.domains.dtos.ToolRequest;
import com.bdsystems.bdsystems_api.tools.domains.dtos.ToolResponse;
import com.bdsystems.bdsystems_api.tools.domains.entities.Tool;
import com.bdsystems.bdsystems_api.tools.repositories.ToolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ToolService {

  private final ToolRepository toolRepository;
  private final CompanyRepository companyRepository;

  public ToolService(ToolRepository toolRepository,
    CompanyRepository companyRepository) {
    this.toolRepository = toolRepository;
    this.companyRepository = companyRepository;
  }

  @Transactional
  public ToolResponse create(ToolRequest request,
    UUID companyId) {
    validate(request);
    String code = normalize(request.code());
    if (toolRepository.existsByCompanyIdAndCode(companyId,
      code)) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Tool code already registered for this company"
      );
    }

    Company company = companyRepository.findById(companyId)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "Authenticated company not found"
      ));
    Tool tool = new Tool(company,
      code,
      normalize(request.name()));
    return toResponse(toolRepository.save(tool));
  }

  @Transactional(readOnly = true)
  public Page<ToolResponse> findAll(
    UUID companyId,
    Pageable pageable,
    Boolean active,
    String query
  ) {
    String normalizedQuery = query == null || query.isBlank() ? "" : query.trim();
    Boolean activeFilter = active == null ? Boolean.TRUE : active;
    return toolRepository.findByFilters(companyId,
        activeFilter,
        normalizedQuery,
        pageable)
      .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public ToolResponse findById(UUID id,
    UUID companyId) {
    return toResponse(findTool(id,
      companyId));
  }

  @Transactional
  public ToolResponse update(UUID id,
    ToolRequest request,
    UUID companyId) {
    validate(request);
    Tool tool = findTool(id,
      companyId);
    String code = normalize(request.code());
    if (toolRepository.existsByCompanyIdAndCodeAndIdNot(companyId,
      code,
      id)) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Tool code already registered for this company"
      );
    }
    tool.updateDetails(code,
      normalize(request.name()));
    return toResponse(toolRepository.save(tool));
  }

  @Transactional
  public void deactivate(UUID id,
    UUID companyId) {
    findTool(id,
      companyId).deactivate();
  }

  @Transactional
  public void activate(UUID id,
    UUID companyId) {
    findTool(id,
      companyId).activate();
  }

  private Tool findTool(UUID id,
    UUID companyId) {
    return toolRepository.findByIdAndCompanyId(id,
        companyId)
      .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Tool not found"
      ));
  }

  private String normalize(String value) {
    return value.trim();
  }

  private void validate(ToolRequest request) {
    if (request == null
      || request.code() == null
      || request.code().isBlank()
      || request.code().trim().length() > 50
      || request.name() == null
      || request.name().isBlank()
      || request.name().trim().length() > 150) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Tool code and name are required and must fit their maximum lengths"
      );
    }
  }

  private ToolResponse toResponse(Tool tool) {
    return new ToolResponse(
      tool.getId(),
      tool.getCompany().getId(),
      tool.getCode(),
      tool.getName(),
      tool.isActive(),
      tool.getCreatedAt(),
      tool.getUpdatedAt()
    );
  }
}
