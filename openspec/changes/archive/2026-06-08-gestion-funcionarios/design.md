# Design: Gestión de Funcionarios (CRUD admin + lookup de cargos)

## Technical Approach

Extend the existing `auth` context to own funcionario CRUD and add a `Cargo`
read-model to `organization`. Follow the established split: **reads go
controller → output port directly** (like `OrganizationController`), **commands
go through domain use cases** (like `IssueVotingTokenUseCaseImpl`). The
`funcionarios` table already has every column needed (V1 schema); no migration
is required — we only widen the JPA entity mapping. `debeCambiarPassword` is the
ONE field with no column yet, so V1 needs a column add (see Open Questions).

## Architecture Decisions

| Decision | Options | Chosen | Rationale |
|---|---|---|---|
| Home for funcionario CRUD | new module / extend `auth` | extend `auth` | `auth` already owns the `Funcionario` aggregate + JPA repo |
| Command layer | app service / domain use case | domain use case | matches existing pattern; keeps validation in pure domain |
| Read layer | use case / direct port | direct port from controller | matches `OrganizationController`; reads carry no business rule |
| Password hashing | manual in use case / output port | **`PasswordEncoderPort` (out)** | BCrypt is `org.springframework.security.*` — CANNOT enter `domain/`. Port keeps domain pure |
| Search query | Specification / `@Query` LIKE | `@Query` LIKE (lower) | three-field OR search is trivial; Specification is overkill |
| 404 / 400 / 409 mapping | reuse `DomainException` (409) / new exceptions | **new `NotFoundException` + validation exceptions** | current handler maps ALL `DomainException`→409; spec needs distinct 404/400/409 |
| `departamento_id` / `cargo_id` mapping | `@ManyToOne` / `Integer` FK | `Integer` FK column | avoids cross-context JPA coupling; reads resolve names via separate lookups |

## Critical Constraint: Domain Purity vs BCrypt

`BCryptPasswordEncoder` lives in `org.springframework.security.crypto.*`.
Importing it into `auth/domain/usecase/CreateFuncionarioUseCaseImpl` would break
the non-negotiable law (`domain/` has ZERO Spring imports). Therefore:

- Define `auth/domain/port/out/PasswordEncoderPort` with `String encode(String raw)`.
- Implement it in `auth/infrastructure/adapter/out/security/BCryptPasswordEncoderAdapter` (`@Component`).
- The use case generates the random raw password (pure `SecureRandom`) and calls
  `passwordEncoderPort.encode(...)`. Hashing stays behind the port.

## Data Flow

```
POST /api/v1/funcionarios
  Controller → CreateFuncionarioUseCase
    → validate required fields            (400 via ValidationException)
    → existsByDocumentoIdentidad?         (409 via DuplicateException)
    → rawPwd = SecureRandom(12 alnum)
    → hash = PasswordEncoderPort.encode(rawPwd)
    → FuncionarioRepositoryPort.save(new Funcionario(debeCambiarPassword=true,...))
    → FuncionarioResponse (NO password)   201

GET /api/v1/funcionarios?search=Juan
  Controller → FuncionarioRepositoryPort.findAll(search)
    → JpaRepository.search("%juan%")      → List<FuncionarioResponse> 200

GET /api/v1/funcionarios/{id}
  Controller → FuncionarioRepositoryPort.findById(id)
    → orElseThrow NotFoundException       404 | FuncionarioResponse 200

PUT /api/v1/funcionarios/{id}
  Controller → UpdateFuncionarioUseCase
    → findById or NotFoundException        404
    → apply editable fields (never password)
    → save                                 → FuncionarioResponse 200
```

## File Changes

### Backend — `auth`

| File | Action | Description |
|---|---|---|
| `auth/domain/Funcionario.java` | Modify | Add nombres, apellidos, tipoDocumento, telefono, departamentoId, cargoId, fechaIngreso, debeCambiarPassword. Relax ctor: `id` nullable for pre-persist create |
| `auth/domain/port/out/FuncionarioRepositoryPort.java` | Modify | Add `findAll(String search)`, `findById(Integer)`, `save(Funcionario)`, `existsByDocumentoIdentidad(String)` |
| `auth/domain/port/out/PasswordEncoderPort.java` | Create | `String encode(String raw)` — keeps domain Spring-free |
| `auth/domain/port/in/CreateFuncionarioUseCase.java` | Create | Input port |
| `auth/domain/port/in/UpdateFuncionarioUseCase.java` | Create | Input port |
| `auth/domain/usecase/CreateFuncionarioUseCaseImpl.java` | Create | Validate, dup-check, gen+encode pwd, save |
| `auth/domain/usecase/UpdateFuncionarioUseCaseImpl.java` | Create | Exists-check, apply fields, save |
| `auth/infrastructure/.../persistence/FuncionarioEntity.java` | Modify | Map all new columns (additive — safe for eligibility reuse) |
| `auth/infrastructure/.../persistence/FuncionarioJpaRepository.java` | Modify | `@Query` search + `existsByDocumentoIdentidad` |
| `auth/infrastructure/.../persistence/FuncionarioRepositoryAdapter.java` | Modify | Implement new methods; bi-directional mapping |
| `auth/infrastructure/.../security/BCryptPasswordEncoderAdapter.java` | Create | `@Component implements PasswordEncoderPort` |
| `auth/infrastructure/.../web/FuncionarioController.java` | Create | 4 endpoints |
| `auth/application/dto/CreateFuncionarioRequest.java` | Create | Request record |
| `auth/application/dto/UpdateFuncionarioRequest.java` | Create | Request record |
| `auth/application/dto/FuncionarioResponse.java` | Create | Response record (no password) |

### Backend — `organization`

| File | Action | Description |
|---|---|---|
| `organization/domain/Cargo.java` | Create | id, codigo, nombre, nivelJerarquico, activo (Departamento pattern) |
| `organization/domain/port/out/CargoRepositoryPort.java` | Create | `findAllByActivoTrue()` |
| `organization/infrastructure/.../persistence/CargoEntity.java` | Create | Maps `cargos` table |
| `organization/infrastructure/.../persistence/CargoJpaRepository.java` | Create | `findAllByActivoTrue()` |
| `organization/infrastructure/.../persistence/CargoRepositoryAdapter.java` | Create | Adapter |
| `organization/infrastructure/.../web/OrganizationController.java` | Modify | Add `GET /cargos` + `CargoResponse` record |

### Config / common

| File | Action | Description |
|---|---|---|
| `config/DomainConfig.java` | Modify | Wire `CreateFuncionarioUseCase`, `UpdateFuncionarioUseCase` (inject `PasswordEncoderPort`) |
| `common/exception/NotFoundException.java` | Create | 404 |
| `common/exception/ValidationException.java` | Create | 400 |
| `common/exception/DuplicateResourceException.java` | Create | 409 (or reuse `DomainException`) |
| `common/exception/GlobalExceptionHandler.java` | Modify | Map new exceptions to 404/400/409 |
| `common/config/SecurityConfig.java` | Inspect only | `/api/**` already `authenticated()` → funcionarios covered. No change |

### Frontend

| File | Action | Description |
|---|---|---|
| `src/api/client.ts` | Modify | funcionarios CRUD + `getCargos()` |
| `src/pages/FuncionariosPage.tsx` | Create | List+search, create modal, edit, toggle puedeVotar |
| `src/App.tsx` | Modify | Route `/funcionarios` |
| `src/components/Layout.tsx` | Modify | Nav entry "Funcionarios" |

## Interfaces / Contracts

```java
// auth/domain/port/out/PasswordEncoderPort.java
public interface PasswordEncoderPort {
    String encode(String rawPassword);
}

// auth/domain/port/out/FuncionarioRepositoryPort.java (additions)
List<Funcionario> findAll(String search);          // null/blank search → all
Optional<Funcionario> findById(Integer id);
Funcionario save(Funcionario funcionario);
boolean existsByDocumentoIdentidad(String documento);
```

```java
// JPA search — case-insensitive across the three fields
@Query("""
  SELECT f FROM FuncionarioEntity f
  WHERE :search IS NULL OR :search = ''
     OR LOWER(f.nombres)            LIKE LOWER(CONCAT('%', :search, '%'))
     OR LOWER(f.apellidos)          LIKE LOWER(CONCAT('%', :search, '%'))
     OR LOWER(f.documentoIdentidad) LIKE LOWER(CONCAT('%', :search, '%'))""")
List<FuncionarioEntity> search(@Param("search") String search);
```

## Testing Strategy

| Layer | What | Approach |
|---|---|---|
| Unit | `CreateFuncionarioUseCaseImpl`: success, missing field (400), duplicate (409), pwd encoded + `debeCambiarPassword=true` | JUnit5 + Mockito; mock `FuncionarioRepositoryPort` + `PasswordEncoderPort` |
| Unit | `UpdateFuncionarioUseCaseImpl`: success, not-found (404), password never touched | JUnit5 + Mockito |
| Integration | `FuncionarioRepositoryAdapter`: save → find → search | Testcontainers PG |
| Integration | `CargoRepositoryAdapter`: `findAllByActivoTrue` returns seeded cargos | Testcontainers PG |
| Regression | `VoterEligibilityRepositoryAdapter` still green after entity widening | existing `*IT` |
| Frontend | Page renders, create, toggle | Manual / optional Playwright |

## Migration / Rollout

`funcionarios` already has nombres, apellidos, tipo_documento, telefono,
departamento_id, cargo_id, fecha_ingreso. The ONLY missing column is
`debe_cambiar_password`. Per AGENTS.md "V1 is canonical, no V2 until production
data exists" → add the column to V1 (`BOOLEAN DEFAULT true`). No data migration.
Seed funcionarios keep placeholder hashes.

## Open Questions

- [ ] `debe_cambiar_password` column: edit V1 (no prod data yet, allowed) vs new
      V2 migration? Recommendation: **edit V1** per AGENTS.md.
- [ ] Reuse `DomainException` (already 409) for duplicate, only adding 404/400
      exceptions? Reduces new classes. Recommendation: **reuse DomainException
      for 409**, add `NotFoundException` (404) + `ValidationException` (400).
