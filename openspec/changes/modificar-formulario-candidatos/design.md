# Design: modificar-formulario-candidatos

## Technical Approach

Replace `numeroOrden` + `afiliacionPolitica` with `funcionarioId` across the `electoral`
candidate slice; enforce alphabetical ballot ordering by `nombre`; add a standalone
`POST /api/v1/files/upload` endpoint in a new `fileupload` bounded context. Migrations are
**Flyway** (next version `V5`). The `candidatos` table is shared by the `electoral` and legacy
`candidates` contexts via `CandidatoEntity`, so both must move together.

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|----------|--------|-----------------------|-----------|
| `funcionarioId` type | `Integer` (nullable) | UUID | `funcionarios.id` is `INTEGER` in V1; FK must match. |
| Synthetic ordering after `numeroOrden` removal | Sort by `esVotoEnBlanco`/`esVotoNulo` flags (synthetic last), then `nombre` ASC | Keep sentinel column | Sentinels (0/-1) only existed to drive ordering; flags already exist. |
| Uniqueness rule | App-level `existsByEleccionIdAndFuncionarioId` + DB partial unique index (`funcionario_id IS NOT NULL`) | DB-only | Synthetic candidates have NULL `funcionario_id`; partial index allows many NULLs while blocking real duplicates → 409. |
| File storage | Local filesystem adapter behind `FileStoragePort` | S3 now | MVP scope; port keeps S3 swap clean. |
| Upload context | New `fileupload` context, hexagonal | Add to `electoral` | Generic capability; decoupled per proposal. |
| Migration strategy | `V5` drops `numero_orden`/`afiliacion_politica`, adds nullable `funcionario_id` + FK + partial unique index | Backfill | Legacy order/affiliation data is discardable per spec. |

## Data Flow

    POST /files/upload (multipart)
      FileUploadController → FileUploadAppService → StoreFilePort(useCase)
         → FileStoragePort (LocalFsAdapter writes file) → returns {url}

    POST /elections/{id}/candidates (JSON, fotoUrl from prior upload)
      ElectionController → AddCandidateUseCase
         → existsByEleccionIdAndFuncionarioId? (409) → save → CandidatoEntity

    GET ballot/candidates
      Adapter → findByEleccionId → sort(synthetic last, nombre ASC) → DTO

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `db/migration/V5__candidate_funcionario_and_ordering.sql` | Create | Drop `numero_orden`, `afiliacion_politica`, old UNIQUE; add `funcionario_id INTEGER REFERENCES funcionarios(id)` + partial unique index `(eleccion_id, funcionario_id) WHERE funcionario_id IS NOT NULL`. |
| `electoral/domain/Candidate.java` | Modify | Drop `numeroOrden`/`afiliacionPolitica`; add `Integer funcionarioId`. |
| `electoral/application/command/AddCandidateCommand.java` | Modify | Same field swap; drop `numeroOrden>=1` check. |
| `electoral/domain/port/out/CandidateRepositoryPort.java` | Modify | Replace `existsByEleccionIdAndNumeroOrden` → `existsByEleccionIdAndFuncionarioId`; rename `findByEleccionIdOrderByNumeroOrden` → `findByEleccionIdOrderByNombre`. |
| `electoral/domain/usecase/AddCandidateUseCaseImpl.java` | Modify | Uniqueness by `funcionarioId`; throw `DomainException` (→409). |
| `electoral/domain/usecase/ActivateElectionUseCaseImpl.java` | Modify | Synthetic candidates pass `null` funcionarioId (no sentinel order). |
| `electoral/.../persistence/CandidateRepositoryAdapter.java` | Modify | Sort synthetic-last then `nombre` ASC; map `funcionarioId`. |
| `candidates/.../persistence/CandidatoEntity.java` | Modify | Drop `numeroOrden`/`afiliacionPolitica` columns; add `funcionarioId`. |
| `electoral/.../web/ElectionController.java` | Modify | Update `AddCandidateRequest`/`CandidateResponse`/`CandidateFullRequest` DTOs. |
| `electoral/application/service/CreateElectionWithCandidatesAppService.java` | Modify | Swap field in `CandidateCreationData`. |
| `candidates/.../web/CandidatesController.java` + `candidates/domain/Candidato.java` | Modify | Remove `numeroOrden` from legacy response/model. |
| `voting/.../portal/PortalVotingController.java` | Modify | Drop `numeroOrden`/`afiliacionPolitica` from ballot projection. |
| `fileupload/domain/port/in/StoreFileUseCase.java` + `out/FileStoragePort.java` | Create | Upload ports. |
| `fileupload/domain/usecase/StoreFileUseCaseImpl.java` | Create | Validates type/size, delegates to storage port. |
| `fileupload/application/service/FileUploadAppService.java` | Create | Orchestrates multipart → command. |
| `fileupload/.../in/web/FileUploadController.java` | Create | `POST /api/v1/files/upload`, returns `{url}`. |
| `fileupload/.../out/storage/LocalFileStorageAdapter.java` | Create | Writes to configured dir, returns URL. |
| `config/DomainConfig.java` | Modify | Wire `StoreFileUseCaseImpl`; update electoral beans (signatures unchanged). |
| `config/FileUploadProperties.java` + static resource handler | Create | `@ConfigurationProperties` (dir, max size, allowed types); serve `/files/**` publicly. |

## Interfaces / Contracts

```java
public interface FileStoragePort { String store(String filename, String contentType, byte[] data); }
public interface StoreFileUseCase { String store(StoreFileCommand cmd); } // returns hosted URL
// 400 on bad type/size; response: record UploadResponse(String url)
```

Domain stays pure: `StoreFileCommand` carries `byte[]`/contentType, NOT `MultipartFile`
(controller extracts bytes). `SecurityConfig` must permit `/files/**` GETs.

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | AddCandidate funcionarioId uniqueness; StoreFileUseCase validation | Mockito, mock ports |
| Unit | Adapter ballot sort (synthetic last, nombre ASC) | JUnit + mock JPA repo |
| Slice | FileUploadController multipart 201/400 | `@WebMvcTest` + MockMultipartFile |
| Integration | V5 migration (columns dropped/added, partial unique index → 409); LocalFs round-trip | Testcontainers `*IT` |

## Migration / Rollout

Forward `V5` only (Flyway is forward-only). Rollback = revert code + manual restore migration
re-adding dropped columns from backup. Existing rows get `funcionario_id = NULL`.

## Open Questions

- [ ] Allowed image types/max size for upload validation (assume jpeg/png/webp, 5 MB unless told otherwise).
- [ ] Legacy `candidates` context (`Candidato`, `CandidatesController`) — modify in place or confirm deprecated? Default: modify to keep build green.
