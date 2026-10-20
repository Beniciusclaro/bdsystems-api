# Catálogo de materiais

O pacote `com.bdsystems.bdsystems_api.materials` segue a organização por domínio existente:
`controllers`, `services`, `repositories`, `domains/entities`, `domains/dtos` e um mapper explícito.
Não há estoque, movimentações, orçamento ou histórico de preços.

## Contrato HTTP

Todos os endpoints exigem Bearer JWT. O tenant vem exclusivamente de `tenant_id`.
IDs inexistentes ou pertencentes a outra empresa retornam 404. Duplicidades persistidas retornam
409; fornecedores repetidos dentro de um request retornam 400. As permissões seguem a regra
existente de usuário autenticado, sem novos papéis ou alterações no JWT.

| Método | Endpoint |
|---|---|
| POST, GET | `/api/materials` |
| GET, PUT | `/api/materials/{id}` |
| PATCH | `/api/materials/{id}/status` |
| GET, POST | `/api/materials/{id}/suppliers` |
| PUT, DELETE | `/api/materials/{id}/suppliers/{associationId}` |
| POST, GET | `/api/material-categories` |
| PUT | `/api/material-categories/{id}` |
| PATCH | `/api/material-categories/{id}/status` |
| GET | `/api/units-of-measure` |

Listagens são paginadas (`page`, `size`, `sort`). Materiais aceitam filtros `name`, `code`
(buscas parciais sem distinguir maiúsculas), `categoryId` e `active`. Categorias aceitam
`active`. Unidades listam as ativas por padrão; `active=false` lista as inativas.

Criação retorna 201, consulta/atualização retorna 200 e alteração de status/remoção de vínculo
retorna 204. Status recebe `{"active":false}` ou `{"active":true}`. Não há exclusão física
de materiais nem categorias. Desativar uma categoria não desativa os seus materiais.

Material recebe `code`, `name`, `description`, `unitOfMeasureId`, `categoryId`, `active` e
`suppliers`. Código e nome são obrigatórios e normalizados com trim. Código é único por
empresa, com comparação exata, conforme Tool. Unidade é obrigatória; categoria é opcional.
`active` omitido é true na criação e preserva o valor anterior na atualização.
Unidades e categorias desativadas continuam sendo referências válidas, preservando o catálogo
existente. Novos vínculos exigem fornecedores ativos da mesma empresa.

`suppliers` é opcional. Na criação, todos os vínculos são gravados na mesma transação do material.
No PUT, cada item é incluído ou atualizado por `supplierId`; vínculos omitidos são preservados,
inclusive quando a lista é vazia. DELETE é a única operação que remove um vínculo.
PUT do vínculo exige o mesmo `supplierId`; para substituir o fornecedor, remova e adicione.
`referencePrice` é opcional, não negativo, com até dez dígitos inteiros e duas casas decimais.
É apenas uma referência comercial, não o preço definitivo de um orçamento.

As respostas de material contêm unidade e categoria resumidas. Os fornecedores são consultados
pelo endpoint paginado próprio para evitar coleções ilimitadas e paginação com collection fetch.
Entity graphs carregam as referências necessárias nas listagens. Timestamps usam OffsetDateTime
e TIMESTAMP WITH TIME ZONE, seguindo Tool/ToolRental.

OpenAPI: `/material/openapi.yml`, integrado ao índice `/openapi.yml` e ao Swagger existente.

## Arquivos

- `materials/domains/entities`: Material, MaterialCategory, UnitOfMeasure, SupplierMaterial.
- `materials/domains/dtos`: requests, responses e StatusRequest (records com Bean Validation).
- `materials/domains/MaterialMapper.java`: conversão para DTOs, sem expor entidades.
- `materials/repositories`: quatro repositories, com consultas por tenant e filtros paginados.
- `materials/services/MaterialService.java`: catálogo, categorias, unidades e vínculos transacionais.
- `materials/controllers`: três controllers REST, parsing local do tenant e tratamento de conflito
  de integridade limitado ao módulo.
- `V8__create_materials_module.sql`: quatro tabelas, FKs, índices, unicidades compostas e check de preço.
- `V9__seed_units_of_measure.sql`: dez unidades globais com IDs fixos e ON CONFLICT(code) DO NOTHING.
- `MaterialsControllerTest`: fluxos HTTP com JWT assinado, validações, isolamento e rollback.
- `application-materials-postgres.yml`: perfil opcional para PostgreSQL descartável.
- `SecurityConfig.java`: somente liberação do documento OpenAPI, como nos demais módulos.
- `static/openapi.yml`: referências aos novos endpoints.

## Verificação

Java 25 e Maven, usando as dependências existentes, sem alterações no POM:

```bash
mvn verify
```

Os testes padrão usam H2, como os demais módulos. Para validar o mesmo fluxo no PostgreSQL,
a sequência completa de migrations Flyway, a idempotência do seed e constraints diretamente no banco:

```bash
docker run -d --rm --name bds-materials-test \
  -e POSTGRES_PASSWORD=materials-test -e POSTGRES_DB=materials_test \
  -p 127.0.0.1:55439:5432 postgres:18
mvn -Dtest=MaterialsControllerTest -Dspring.profiles.active=materials-postgres test
docker stop bds-materials-test
```

Aguarde o banco aceitar conexões antes de iniciar os testes. Use apenas um banco descartável.
O perfil não apaga schema e pode ser configurado com `MATERIALS_TEST_DATABASE_URL`,
`MATERIALS_TEST_DATABASE_USER` e `MATERIALS_TEST_DATABASE_PASSWORD`.
