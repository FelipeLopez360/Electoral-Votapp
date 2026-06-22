## Exploration: modificar-formulario-candidatos

### Current State
Currently, candidates have `numeroOrden` and `afiliacionPolitica` fields. The candidate creation and update endpoints (`POST /api/v1/elections/{id}/candidates` and `POST /api/v1/elections/full`) expect JSON payloads containing these fields. The candidate does not currently link to a `Funcionario`. The `fotoUrl` field exists and expects a string URL. The frontend files do not live in this repository.

### Affected Areas
- `src/main/java/co/com/votapp/ws/electoral/domain/Candidate.java` — Needs to remove `numeroOrden` and `afiliacionPolitica`, and add `Integer funcionarioId`.
- `src/main/java/co/com/votapp/ws/electoral/application/command/AddCandidateCommand.java` — Needs similar field updates.
- `src/main/java/co/com/votapp/ws/electoral/domain/usecase/AddCandidateUseCaseImpl.java` — Must map the new `funcionarioId`, remove `numeroOrden` uniqueness validation (since order is removed), and add `funcionarioId` uniqueness validation per election.
- `src/main/java/co/com/votapp/ws/candidates/infrastructure/adapter/out/persistence/CandidatoEntity.java` — Remove columns `numero_orden` and `afiliacion_politica`. Add `funcionario_id` column.
- `src/main/java/co/com/votapp/ws/candidates/infrastructure/adapter/out/persistence/CandidatoJpaRepository.java` — Replace `existsByEleccionIdAndNumeroOrden` with `existsByEleccionIdAndFuncionarioId`. Update sorting strategy to order by `nombre`.
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/CandidateRepositoryAdapter.java` — Adapt the changes from the JPA repository and mappings.
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/ElectionController.java` — Update `AddCandidateRequest`, `CandidateFullRequest`, `CandidateResponse` DTOs.
- `src/main/java/co/com/votapp/ws/candidates/infrastructure/adapter/in/web/CandidatesController.java` — Update `CandidatoResponse` DTO.
- `src/main/resources/db/migration/...` (or equivalent) — A Liquibase/Flyway script is needed to alter the `candidatos` table.

### Approaches
1. **Mixed Multipart in Candidate Creation** — Change the candidate creation endpoints to accept `multipart/form-data` with the file and JSON data together.
   - Pros: A single request from the frontend.
   - Cons: Highly complex for the `POST /full` endpoint (which receives a complex JSON with nested arrays). Breaks REST API purity.
   - Effort: High

2. **Separate Upload Endpoint + Pure JSON** — Create a new endpoint `POST /api/v1/files/upload` that receives a `MultipartFile` and returns a URL. The frontend uses this URL to populate the existing `fotoUrl` field in the JSON payloads.
   - Pros: Keeps the complex candidate endpoints as pure JSON. Easy to implement. Decouples storage from domain logic.
   - Cons: Frontend requires an extra request to upload the file before submitting the candidate.
   - Effort: Low/Medium

### Recommendation
I recommend **Approach 2: Separate Upload Endpoint + Pure JSON**. Changing the `CreateElectionFullRequest` (which contains nested election and candidate objects) to `multipart/form-data` is extremely problematic in Spring Boot. A dedicated file upload endpoint provides a cleaner separation of concerns and allows the frontend to easily implement dual control (direct file upload vs URL text field) by uploading the file first. For the `funcionarioId`, the frontend should fetch the list of officials via the existing `GET /api/v1/funcionarios` endpoint and pass `funcionarioId` and `nombre` to the candidate creation endpoints.

### Risks
- Dropping the `numero_orden` column will require a strategy to order candidates in the ballot (e.g., alphabetically by `nombre`).
- The frontend must be coordinated to use the new file upload endpoint.
- Existing candidate data in the database will need a migration to handle `funcionario_id` or allow it to be nullable for legacy records.

### Ready for Proposal
Yes — the orchestrator should tell the user that the backend changes are straightforward but require a separate file upload endpoint to keep the `POST /full` wizard endpoint clean.
