# Tasks: Gestión de Funcionarios

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1100-1300 lines |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Cargo Read-Model & Schema Update | PR 1 | Base `mvp-base`. Adds Cargo read-model, Exceptions, Bcrypt adapter, and schema update. |
| 2 | Funcionario Domain & Persistence | PR 2 | Base PR 1 branch. Adds use cases, expands domain/entities, repository updates, and related tests. |
| 3 | Controller & Frontend | PR 3 | Base PR 2 branch. Adds REST Controller, DTOs, frontend API client, and Funcionarios page UI. |

## Phase 1: Foundation (exceptions + ports + password)

- [x] 1.1 Create `co/com/votapp/ws/common/exception/NotFoundException.java`
- [x] 1.2 Create `co/com/votapp/ws/common/exception/ValidationException.java`
- [x] 1.3 Modify `co/com/votapp/ws/common/exception/GlobalExceptionHandler.java` to map `NotFoundException` -> 404, `ValidationException` -> 400, and ensure `DomainException` remains 409
- [x] 1.4 Create `co/com/votapp/ws/auth/domain/port/out/PasswordEncoderPort.java` with `encode` method
- [x] 1.5 Create `co/com/votapp/ws/auth/infrastructure/adapter/out/security/BCryptPasswordEncoderAdapter.java` implementing `PasswordEncoderPort`

## Phase 2: Organization module — Cargos lookup

- [x] 2.1 Create `co/com/votapp/ws/organization/domain/Cargo.java`
- [x] 2.2 Create `co/com/votapp/ws/organization/infrastructure/adapter/out/persistence/CargoEntity.java`
- [x] 2.3 Create `co/com/votapp/ws/organization/infrastructure/adapter/out/persistence/CargoJpaRepository.java` with `findAllByActivoTrue()`
- [x] 2.4 Create `co/com/votapp/ws/organization/domain/port/out/CargoRepositoryPort.java`
- [x] 2.5 Create `co/com/votapp/ws/organization/infrastructure/adapter/out/persistence/CargoRepositoryAdapter.java`
- [x] 2.6 Create `co/com/votapp/ws/organization/application/dto/CargoResponse.java`
- [x] 2.7 Modify `co/com/votapp/ws/organization/infrastructure/adapter/in/web/OrganizationController.java` to add `GET /api/v1/organization/cargos`

## Phase 3: Schema update

- [x] 3.1 Edit `src/main/resources/db/migration/V1__Initial_schema.sql` to add `debe_cambiar_password BOOLEAN DEFAULT true` to `funcionarios` table

## Phase 4: Auth module — Expand domain + persistence

- [x] 4.1 Modify `co/com/votapp/ws/auth/domain/Funcionario.java` to add: `nombres`, `apellidos`, `tipoDocumento`, `telefono`, `departamentoId`, `cargoId`, `fechaIngreso`, `debeCambiarPassword`. Relax constructor so `id` can be null.
- [x] 4.2 Modify `co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioEntity.java` to map the new fields (with `Integer` for FKs)
- [x] 4.3 Modify `co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioJpaRepository.java` to add `search` query with `LOWER` / `LIKE`, and `existsByDocumentoIdentidad`
- [x] 4.4 Modify `co/com/votapp/ws/auth/domain/port/out/FuncionarioRepositoryPort.java` adding `findAll(search)`, `findById`, `save`, `existsByDocumentoIdentidad`
- [x] 4.5 Modify `co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioRepositoryAdapter.java` to implement new port methods and reverse mapping
- [x] 4.6 Create `co/com/votapp/ws/auth/domain/port/in/CreateFuncionarioUseCase.java` and `CreateFuncionarioUseCaseImpl.java` (generating/encoding temporary password)
- [x] 4.7 Create `co/com/votapp/ws/auth/domain/port/in/UpdateFuncionarioUseCase.java` and `UpdateFuncionarioUseCaseImpl.java`

## Phase 5: Auth module — Controller + DTOs

- [x] 5.1 Create `co/com/votapp/ws/auth/application/dto/CreateFuncionarioRequest.java`
- [x] 5.2 Create `co/com/votapp/ws/auth/application/dto/UpdateFuncionarioRequest.java`
- [x] 5.3 Create `co/com/votapp/ws/auth/application/dto/FuncionarioResponse.java`
- [x] 5.4 Create `co/com/votapp/ws/auth/infrastructure/adapter/in/web/FuncionarioController.java` with GET list, GET detail, POST create, PUT update endpoints
- [x] 5.5 Modify `co/com/votapp/ws/config/DomainConfig.java` to wire `CreateFuncionarioUseCase` and `UpdateFuncionarioUseCase` (injecting `PasswordEncoderPort`)

## Phase 6: Frontend

- [x] 6.1 Modify `/Users/felipe-tresesenta/Documents/Electoral-Votapp-Frontend/src/api/client.ts` adding `funcionarios.list`, `get`, `create`, `update` and `cargos.list`
- [x] 6.2 Create `/Users/felipe-tresesenta/Documents/Electoral-Votapp-Frontend/src/pages/FuncionariosPage.tsx` with list, filters, create/edit modals, and toggles
- [x] 6.3 Modify `/Users/felipe-tresesenta/Documents/Electoral-Votapp-Frontend/src/App.tsx` adding `/funcionarios` route
- [x] 6.4 Modify `/Users/felipe-tresesenta/Documents/Electoral-Votapp-Frontend/src/components/Layout.tsx` adding "Funcionarios" nav entry

## Phase 7: Tests

- [x] 7.1 Create `CreateFuncionarioUseCaseTest` verifying success, duplicate checks, missing fields, and password hashing
- [x] 7.2 Create `UpdateFuncionarioUseCaseTest` verifying success and 404
- [x] 7.3 Create/Modify `FuncionarioRepositoryAdapterIT` verifying save, find, search, and exists methods
- [x] 7.4 Create `CargoRepositoryAdapterIT` verifying `findAllByActivoTrue`

## Phase 8: Runtime controller tests (TDD evidence batch)

- [x] 8.1 Create `FuncionarioControllerTest` — 14 controller unit tests: GET list (no search, with search, empty), GET detail (200, 404), POST create (201+temporaryPassword, command fields, 400 propagation, 409 propagation), PUT update (200, puedeVotar toggle, debeCambiarPassword=false, command id from path, 404 propagation)
- [x] 8.2 Create `OrganizationControllerTest` — 4 controller unit tests: GET /cargos (list with items, empty list, all fields mapped), GET /departamentos (list returned)

## Phase 9: HTTP contract + persistence proofs (verify blockers batch)

- [x] 9.1 Create `FuncionarioControllerWebMvcTest` — 6 @WebMvcTest slice tests exercising real Spring MVC + GlobalExceptionHandler: GET detail non-existent → HTTP 404, POST create validation failure → HTTP 400, POST create duplicate documento → HTTP 409, PUT update non-existent → HTTP 404, PUT update debeCambiarPassword=false → HTTP 200 + debeCambiarPassword=false in response, POST create success → HTTP 201 + temporaryPassword in response
- [x] 9.2 Add `shouldPersistDebeCambiarPasswordFalseAfterUpdate` test to `FuncionarioRepositoryAdapterIT` — proves debeCambiarPassword=false is written to real PostgreSQL DB and read back correctly via adapter.save() + findById()
