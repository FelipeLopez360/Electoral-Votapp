# Tasks: modificar-formulario-candidatos

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 1400–1800 |
| 800-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: FileUpload → PR 2: Electoral+Schema |
| Delivery strategy | auto-chain |
| Chain strategy | stacked-to-main |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | FileUpload context (new bounded context) — domain, adapter, controller, config | PR 1 → main | Fully independent, additive-only. Base = main. |
| 2 | V5 migration + Electoral domain changes + API cleanup | PR 2 → main | Depends on nothing. Base = main (stacked after PR 1 merges). |

---

## Phase 1: FileUpload — Domain Layer

- [x] 1.1 RED: Write unit test `StoreFileUseCaseImplTest` — validation rules (empty bytes, bad content type, oversized file → DomainException)
- [x] 1.2 Create `fileupload/domain/model/StoreFileCommand.java` (bytes, contentType, originalFilename)
- [x] 1.3 Create `fileupload/domain/port/in/StoreFileUseCase.java` — `String store(StoreFileCommand)`
- [x] 1.4 Create `fileupload/domain/port/out/FileStoragePort.java` — `String store(String filename, String contentType, byte[] content)`
- [x] 1.5 Create `fileupload/domain/usecase/StoreFileUseCaseImpl.java` — validate then delegate to storage port
- [x] 1.6 GREEN: 1.1 tests pass

## Phase 2: FileUpload — Adapters & API

- [x] 2.1 RED: Write unit test `LocalFileStorageAdapterTest` — file written to disk, URL returned, IOException handled
- [x] 2.2 Create `fileupload/adapter/out/storage/LocalFileStorageAdapter.java` — store to local fs, return `/files/{uuid}-{name}`
- [x] 2.3 GREEN: 2.1 tests pass
- [x] 2.4 RED: Write slice test `FileUploadControllerIT` (`@WebMvcTest`) — 201 on valid multipart, 400 on oversized/invalid type
- [x] 2.5 Create `fileupload/application/FileUploadAppService.java` — extract bytes from MultipartFile, delegate to use case
- [x] 2.6 Create `fileupload/adapter/in/web/FileUploadController.java` — `POST /api/v1/files/upload` → `{ "url": "..." }`
- [x] 2.7 GREEN: 2.4 tests pass

## Phase 3: FileUpload — Config & Integration

- [x] 3.1 RED: Write file round-trip IT `FileUploadIntegrationIT` (@Testcontainers, write file → read back via static serving)
- [x] 3.2 Create `config/FileUploadProperties.java` (upload dir, max size, allowed types)
- [x] 3.3 Wire `StoreFileUseCaseImpl` in `config/DomainConfig.java`
- [x] 3.4 Update `config/SecurityConfig.java` — permit `/files/**` GETs in admin chain
- [x] 3.5 Register static resource handler for `/files/**` → upload dir
- [x] 3.6 Add `application.yml` properties for `app.file-upload.*`
- [x] 3.7 GREEN: 3.1 IT passes

## Phase 4: V5 Schema Migration

- [ ] 4.1 RED: Write `V5CandidateMigrationIT` — verify `numero_orden`/`afiliacion_politica` dropped, `funcionario_id` added + FK + partial unique index
- [ ] 4.2 Create `V5__candidate_funcionario_and_ordering.sql` — drop `numero_orden`, `afiliacion_politica`, old UNIQUE; add `funcionario_id INTEGER REFERENCES funcionarios(id)` + partial unique index `WHERE funcionario_id IS NOT NULL`
- [ ] 4.3 GREEN: 4.1 IT passes

## Phase 5: Electoral Domain Changes

- [ ] 5.1 RED: Write unit test for `Candidate` record — constructor rejects null `funcionarioId`?, blank `numeroOrden`/`afiliacionPolitica` fields removed
- [ ] 5.2 Update `Candidate.java` — drop `numeroOrden`, `afiliacionPolitica`; add `Integer funcionarioId`
- [ ] 5.3 Update `AddCandidateCommand.java` — replace `int numeroOrden` with `Integer funcionarioId`; remove `numeroOrden >= 1` check
- [ ] 5.4 Update `CandidateRepositoryPort.java` — `existsByEleccionIdAndNumeroOrden` → `existsByEleccionIdAndFuncionarioId`; `findByEleccionIdOrderByNumeroOrden` → `findByEleccionIdOrderByNombre`
- [ ] 5.5 GREEN: 5.1 tests pass
- [ ] 5.6 RED: Write unit test for `AddCandidateUseCaseImpl` — uniqueness by `funcionarioId`, returns 409 on duplicate
- [ ] 5.7 Update `AddCandidateUseCaseImpl.java` — check `existsByEleccionIdAndFuncionarioId`, build Candidate with null `funcionarioId` fallback
- [ ] 5.8 Update `ActivateElectionUseCaseImpl.java` — synthetic candidates pass `null` for `funcionarioId`, no sentinel order values
- [ ] 5.9 GREEN: 5.6 tests pass

## Phase 6: Persistence Adapter Changes

- [ ] 6.1 RED: Write unit test for `CandidateRepositoryAdapter` — sorting: synthetic candidates last, nombre ASC
- [ ] 6.2 Update `CandidatoEntity.java` — drop `numeroOrden`, `afiliacionPolitica`; add `funcionarioId` + FK mapping
- [ ] 6.3 Update `CandidatoRepositoryAdapter.java` (candidates context mapper) — drop `numeroOrden`/`afiliacionPolitica` mapping
- [ ] 6.4 Update `CandidateRepositoryAdapter.java` — `findByEleccionIdOrderByNombre`: sort synthetic-last (esVotoEnBlanco/esVotoNulo flags) then nombre ASC; `existsByEleccionIdAndFuncionarioId`; map `funcionarioId`
- [ ] 6.5 GREEN: 6.1 tests pass

## Phase 7: API & Legacy Cleanup

- [ ] 7.1 RED: Write slice test `ElectionControllerIT` — updated DTOs (no `numeroOrden`, `afiliacionPolitica`; has `funcionarioId`)
- [ ] 7.2 Update `ElectionController.java` inner records — `AddCandidateRequest` drops `numeroOrden`/`afiliacionPolitica`, adds `funcionarioId`; `CandidateResponse` similar; `CandidateFullRequest` similar; `listCandidates`/`addCandidate` methods updated
- [ ] 7.3 Update `CreateElectionWithCandidatesAppService.java` — `CandidateCreationData` replaces `numeroOrden` with `funcionarioId`; `AddCandidateCommand` construction updated
- [ ] 7.4 Update `CandidatesController.java` — `CandidatoResponse` drops `numeroOrden`
- [ ] 7.5 Update `Candidato.java` — drop `numeroOrden`
- [ ] 7.6 Update `PortalVotingController.java` — `PortalCandidateItem` drops `numeroOrden`, `afiliacionPolitica`; ballot projection uses `nombre` ordering
- [ ] 7.7 GREEN: 7.1 tests pass, full `mvn clean verify` green
