# Proposal: modificar-formulario-candidatos

## Intent

Simplify candidate registration by removing obsolete fields and linking candidates to actual officials. The system must remove the `numeroOrden` and `afiliacionPolitica` fields from the domain and database, automatically order the ballot alphabetically by name, and introduce `funcionarioId` to link candidates to internal officials. Furthermore, it must provide a standalone file upload endpoint for candidate photos to keep existing JSON creation endpoints clean.

## Scope

### In Scope
- Remove `numeroOrden` and `afiliacionPolitica` from candidate domain models, entities, DTOs, and DB schema.
- Add `funcionarioId` (UUID/Integer) to the Candidate model with validation to prevent duplicate officials in the same election.
- Enforce alphabetical ordering by `nombre` for candidates on the ballot.
- Implement a new `POST /api/v1/files/upload` endpoint that accepts a `MultipartFile` and returns a hosted URL string.
- Create database migrations to alter the `candidatos` table.

### Out of Scope
- Frontend UI modifications (handled in the frontend repo).
- Complex cloud storage integration (local storage or simple mock returning a URL is sufficient for the MVP).

## Capabilities

> This section is the CONTRACT between proposal and specs phases.
> The sdd-spec agent reads this to know exactly which spec files to create or update.
> Research `openspec/specs/` before filling this in.

### New Capabilities
<!-- Capabilities being introduced. Each becomes a new `openspec/specs/<name>/spec.md`.
     Use kebab-case names (e.g., user-auth, data-export, api-rate-limiting).
     Leave empty if no new capabilities. -->
- `file-upload`: Support uploading images/files to obtain a hosted URL string for use in JSON payloads.

### Modified Capabilities
<!-- Existing capabilities whose REQUIREMENTS are changing (not just implementation).
     Only list here if spec-level behavior changes. Each needs a delta spec.
     Use existing spec names from openspec/specs/. Leave empty if none. -->
- `electoral`: Update candidate registration rules to require `funcionarioId`, remove `numeroOrden` and `afiliacionPolitica`, and define alphabetical ballot ordering.

## Approach

**Approach 2: Separate Upload Endpoint + Pure JSON** (as recommended in exploration).
Instead of making the complex candidate and election creation endpoints (`POST /api/v1/elections/full`) accept mixed multipart forms, we will expose a standalone `POST /api/v1/files/upload` endpoint. The frontend will upload the photo first, obtain a URL, and submit it in the standard JSON payload under `fotoUrl`.

Additionally, we will refactor the candidate domain to replace `numeroOrden` with `funcionarioId`, update the JPA queries to validate uniqueness by `eleccionId` and `funcionarioId`, and adjust the ballot retrieval to explicitly sort candidates alphabetically by their `nombre`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `db/migration/` | New | Liquibase/Flyway script to drop obsolete columns and add `funcionario_id`. |
| `domain/Candidate.java` | Modified | Add `funcionarioId`, remove `numeroOrden` & `afiliacionPolitica`. |
| `persistence/CandidatoEntity.java` | Modified | Adjust mapped database columns. |
| `persistence/CandidatoJpaRepository.java` | Modified | Replace uniqueness check, implement alphabetical sorting. |
| `adapter/in/web/...` | Modified | Update request and response DTOs for candidates. |
| `adapter/in/web/FilesController.java` | New | Implement `POST /api/v1/files/upload` endpoint. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Breaking legacy candidate data | Medium | The DB migration should safely handle existing rows, allowing `funcionario_id` to be nullable or assigned a default value for old records. |
| Frontend integration issues | High | The frontend must be strictly updated to upload files first before submitting the candidate JSON. |

## Rollback Plan

Revert the application code commit and apply a downward DB migration script to re-add the `numero_orden` and `afiliacion_politica` columns, then restore any dropped data from a pre-deployment backup.

## Dependencies

- None (frontend changes are decoupled but must follow this new API contract).

## Success Criteria

- [ ] Candidates can be created and updated using `funcionarioId` instead of `numeroOrden` and `afiliacionPolitica`.
- [ ] Attempting to add the same `funcionarioId` to the same election returns a validation error.
- [ ] Ballot retrieval endpoints return candidates ordered alphabetically by `nombre`.
- [ ] Submitting a file to `/api/v1/files/upload` returns a valid accessible URL string.
