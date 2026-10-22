package com.bdsystems.bdsystems_api.compositions.services;

import com.bdsystems.bdsystems_api.auth.domains.entities.Company;
import com.bdsystems.bdsystems_api.auth.repositories.CompanyRepository;
import com.bdsystems.bdsystems_api.compositions.domains.dtos.CompositionDtos.ComponentRequest;
import com.bdsystems.bdsystems_api.compositions.domains.dtos.CompositionDtos.Response;
import com.bdsystems.bdsystems_api.compositions.domains.entities.ComponentType;
import com.bdsystems.bdsystems_api.compositions.domains.entities.Composition;
import com.bdsystems.bdsystems_api.compositions.domains.entities.CompositionComponent;
import com.bdsystems.bdsystems_api.compositions.repositories.CompositionRepository;
import com.bdsystems.bdsystems_api.labor.services.LaborService;
import com.bdsystems.bdsystems_api.materials.domains.entities.UnitOfMeasure;
import com.bdsystems.bdsystems_api.materials.repositories.MaterialRepository;
import com.bdsystems.bdsystems_api.materials.repositories.SupplierMaterialRepository;
import com.bdsystems.bdsystems_api.materials.repositories.UnitOfMeasureRepository;
import com.bdsystems.bdsystems_api.tools.repositories.ToolRentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositionServiceTest {
  @Mock
  private CompositionRepository compositionRepository;
  @Mock
  private CompanyRepository companyRepository;
  @Mock
  private UnitOfMeasureRepository unitOfMeasureRepository;
  @Mock
  private MaterialRepository materialRepository;
  @Mock
  private SupplierMaterialRepository supplierMaterialRepository;
  @Mock
  private ToolRentalRepository toolRentalRepository;
  @Mock
  private LaborService laborService;

  private CompositionService compositionService;

  @BeforeEach
  void setUp() {
    compositionService = new CompositionService(compositionRepository,
      companyRepository,
      unitOfMeasureRepository,
      materialRepository,
      supplierMaterialRepository,
      toolRentalRepository,
      laborService);
  }

  @Test
  void updatesComponentAndPreservesItsIdentifier() {
    UUID tenantId = UUID.randomUUID();
    UUID compositionId = UUID.randomUUID();
    UUID componentId = UUID.randomUUID();
    Composition composition = composition(compositionId,
      componentId);
    when(compositionRepository.findDetailedByIdAndCompanyId(compositionId,
      tenantId)).thenReturn(Optional.of(composition));

    Response response = compositionService.updateComponent(compositionId,
      componentId,
      customComponentRequest("Updated component",
        "m2",
        "3.5000",
        "12.2500",
        7),
      tenantId);

    assertThat(response.components()).singleElement().satisfies(componentResponse -> {
      assertThat(componentResponse.id()).isEqualTo(componentId);
      assertThat(componentResponse.description()).isEqualTo("Updated component");
      assertThat(componentResponse.unitLabel()).isEqualTo("m2");
      assertThat(componentResponse.quantity()).isEqualByComparingTo("3.5000");
      assertThat(componentResponse.unitCost()).isEqualByComparingTo("12.2500");
      assertThat(componentResponse.sortOrder()).isEqualTo(7);
    });
  }

  @Test
  void deletesOnlyTheRequestedComponent() {
    UUID tenantId = UUID.randomUUID();
    UUID compositionId = UUID.randomUUID();
    UUID componentId = UUID.randomUUID();
    Composition composition = composition(compositionId,
      componentId);
    when(compositionRepository.findDetailedByIdAndCompanyId(compositionId,
      tenantId)).thenReturn(Optional.of(composition));

    compositionService.deleteComponent(compositionId,
      componentId,
      tenantId);

    assertThat(composition.getComponents()).isEmpty();
  }

  @Test
  void returnsNotFoundWhenComponentDoesNotBelongToComposition() {
    UUID tenantId = UUID.randomUUID();
    UUID compositionId = UUID.randomUUID();
    Composition composition = composition(compositionId,
      UUID.randomUUID());
    when(compositionRepository.findDetailedByIdAndCompanyId(compositionId,
      tenantId)).thenReturn(Optional.of(composition));

    assertThatThrownBy(() -> compositionService.deleteComponent(compositionId,
      UUID.randomUUID(),
      tenantId))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("404 NOT_FOUND")
      .hasMessageContaining("Composition component not found");
  }

  private Composition composition(UUID compositionId,
    UUID componentId) {
    Company company = new Company();
    UnitOfMeasure unitOfMeasure = new UnitOfMeasure("UNIT",
      "Unit",
      "un");
    ReflectionTestUtils.setField(unitOfMeasure,
      "id",
      UUID.randomUUID());
    Composition composition = new Composition(company);
    ReflectionTestUtils.setField(composition,
      "id",
      compositionId);
    composition.update("COMP-001",
      "Composition",
      null,
      unitOfMeasure,
      BigDecimal.ONE,
      true);
    CompositionComponent compositionComponent = new CompositionComponent(ComponentType.CUSTOM,
      null,
      null,
      null,
      null,
      "Original component",
      "un",
      BigDecimal.ONE,
      BigDecimal.TEN,
      0);
    ReflectionTestUtils.setField(compositionComponent,
      "id",
      componentId);
    composition.replaceComponents(List.of(compositionComponent));
    return composition;
  }

  private ComponentRequest customComponentRequest(String description,
    String unitLabel,
    String quantity,
    String unitCost,
    Integer sortOrder) {
    return new ComponentRequest(ComponentType.CUSTOM,
      null,
      null,
      null,
      null,
      description,
      unitLabel,
      new BigDecimal(quantity),
      new BigDecimal(unitCost),
      sortOrder);
  }
}
