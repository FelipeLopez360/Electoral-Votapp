## Verification Report

**Change**: modificar-formulario-candidatos
**Version**: V5 schema
**Mode**: Standard

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 44 |
| Tasks complete | 44 |
| Tasks incomplete | 0 |

### Build & Tests Execution
**Build**: ✅ Passed
```text
./mvnw clean verify → BUILD SUCCESS (01:10 min)
```

**Tests**: ✅ 428 unit tests passed / ✅ 130 integration tests passed / ⚠️ 0 skipped
```text
./mvnw clean test → Tests run: 428, Failures: 0, Errors: 0, Skipped: 0
./mvnw clean verify → Tests run: 130, Failures: 0, Errors: 0, Skipped: 0
```

**Coverage**: ➖ Not available (no coverage threshold configured)

### Spec Compliance Matrix

#### Electoral Spec (`specs/electoral/spec.md`)

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| Candidate Registration by Funcionario | Registering a valid candidate | `AddCandidateUseCaseTest` + `ElectionControllerTest` | ✅ COMPLIANT |
| Candidate Registration by Funcionario | Duplicate candidate in same election (409) | `AddCandidateByFuncionarioIdUseCaseTest` — `existsByEleccionIdAndFuncionarioId` → DomainException | ✅ COMPLIANT |
| Alphabetical Ballot Ordering | Retrieve ballot/candidate list sorted by `nombre` | `CandidateRepositoryAdapterTest` — synthetics last, nombre ASC | ✅ COMPLIANT |
| REMOVED: Manual Candidate Ordering | `numeroOrden` dropped from all layers | `CandidateFuncionarioIdTest` — reflection asserts no `numeroOrden` method; `V5CandidateMigrationIT` — column verified dropped | ✅ COMPLIANT |
| REMOVED: Candidate Political Affiliation | `afiliacionPolitica` dropped from all layers | `CandidateFuncionarioIdTest` — reflection asserts no `afiliacionPolitica` method; `V5CandidateMigrationIT` — column verified dropped | ✅ COMPLIANT |

#### File Upload Spec (`specs/file-upload/spec.md`)

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| Standalone File Upload Endpoint | Successful image upload → 201 + `{url}` | `FileUploadControllerIT` — 201 on valid multipart | ✅ COMPLIANT |
| Standalone File Upload Endpoint | Invalid file format or size → 400 | `FileUploadControllerIT` — 400 on oversized/invalid type; `StoreFileUseCaseImplTest` — validation rules | ✅ COMPLIANT |

**Compliance summary**: 7/7 scenarios compliant

### Correctness (Static Evidence)

| Requirement | Status | Notes |
|------------|--------|-------|
| `numeroOrden` removed from domain model | ✅ Implemented | `Candidate.java` — no `numeroOrden` field; `CandidateFuncionarioIdTest` verifies via reflection |
| `afiliacionPolitica` removed from domain model | ✅ Implemented | `Candidate.java` — no `afiliacionPolitica` field; `CandidateFuncionarioIdTest` verifies via reflection |
| `funcionarioId` added to Candidate | ✅ Implemented | `Candidate.java` — `Integer funcionarioId` (nullable); `AddCandidateCommand.java` — `Integer funcionarioId` |
| `funcionarioId` uniqueness enforcement | ✅ Implemented | `AddCandidateUseCaseImpl.java` — `existsByEleccionIdAndFuncionarioId` → DomainException (mapped to 409) |
| DB schema V5 migration | ✅ Implemented | `V5__candidate_funcionario_and_ordering.sql` — drops `numero_orden`, `afiliacion_politica`; adds `funcionario_id` FK + partial unique index |
| `CandidatoEntity` updated | ✅ Implemented | No `numeroOrden`/`afiliacionPolitica` columns; `funcionarioId` mapped with `@Column(name="funcionario_id")` |
| `CandidateRepositoryPort` updated | ✅ Implemented | `existsByEleccionIdAndFuncionarioId` replaces old `numeroOrden` check; `findByEleccionIdOrderByNombre` replaces old ordering |
| `CandidateRepositoryAdapter` sorting | ✅ Implemented | Synthetic candidates (blank/null) last, then alphabetical `nombre` ASC via `Comparator` chain |
| ElectionController DTOs updated | ✅ Implemented | `AddCandidateRequest`, `CandidateResponse`, `CandidateFullRequest` — all use `funcionarioId`, no `numeroOrden`/`afiliacionPolitica` |
| CandidatesController (legacy) updated | ✅ Implemented | `CandidatoResponse` — no `numeroOrden`; `Candidato.java` domain — no `numeroOrden` |
| PortalVotingController updated | ✅ Implemented | `PortalCandidateItem` — no `numeroOrden`/`afiliacionPolitica`; ballot uses `nombre` ordering |
| FileUpload endpoint exists | ✅ Implemented | `POST /api/v1/files/upload` — `FileUploadController.java` → 201 with `{url}` |
| FileUpload domain validation | ✅ Implemented | `StoreFileUseCaseImpl` — validates empty content, unsupported type, 5MB max; zero Spring imports |
| FileUpload local storage adapter | ✅ Implemented | `LocalFileStorageAdapter` — writes to configured dir, returns `/files/{uuid}-{name}` |
| FileUpload config & wiring | ✅ Implemented | `DomainConfig.java` wires `StoreFileUseCaseImpl`; `FileUploadProperties` for upload dir/size/types |

### Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| `funcionarioId` type = `Integer` (nullable) | ✅ Yes | Matches `funcionarios.id` INTEGER FK |
| Synthetic ordering = sort last + `nombre` ASC | ✅ Yes | `CandidateRepositoryAdapter` uses `Comparator` chain |
| Uniqueness = app-level + DB partial unique index | ✅ Yes | `AddCandidateUseCaseImpl` + `V5` migration partial index |
| File storage = local filesystem behind port | ✅ Yes | `FileStoragePort` → `LocalFileStorageAdapter` |
| Upload context = new `fileupload` bounded context | ✅ Yes | Separate hexagonal context under `fileupload/` |
| Migration = V5 forward-only | ✅ Yes | Flyway V5 script present and applied |
| Domain purity = zero framework imports | ✅ Yes | `Candidate.java`, `StoreFileUseCaseImpl.java`, `StoreFileCommand.java` — no Spring/JPA imports |
| Use cases wired via `DomainConfig` | ✅ Yes | `StoreFileUseCaseImpl` bean in `DomainConfig` |

### Issues Found
**CRITICAL**: None
**WARNING**: `src/test/resources/vote-controller-test-data.sql` contains a stale INSERT referencing `numero_orden` column (dropped in V5). File is dead code — grep confirms no Java test references it via `@Sql` or any other mechanism. Should be cleaned up to avoid confusion.
**SUGGESTION**: None

### Verdict
PASS

All 44 tasks complete. All 558 tests (428 unit + 130 integration) pass. All spec scenarios compliant. `numeroOrden` and `afiliacionPolitica` fully removed from domain, persistence, DTOs, and schema. `funcionarioId` properly added with uniqueness enforcement (409 on duplicate). `POST /api/v1/files/upload` endpoint exists with full validation. Hexagonal architecture maintained — domain layers have zero Spring framework imports.
