# Tasks: Flujo Detallado de Creación de Elecciones

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1200 - 1400 (Combined BE + FE) |
| 800-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | BE PR 1 (Domain/DB) → BE PR 2 (API/Adapters) → FE PR 3 (UI) |
| Delivery strategy | auto-forecast |
| Chain strategy | feature-branch-chain |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
800-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend Domain & Persistence | PR 1 | Adds DB migrations, entities, domain logic, and use cases with unit tests. |
| 2 | Backend API & Orchestration | PR 2 | Adds App services, controllers, and IT tests. Depends on PR 1. |
| 3 | Frontend Application | PR 3 | Updates API clients, CreateElectionPage wizard, and VotePage. Independent base, but depends on BE API contract. |

## Phase 1: Backend Foundation (Domain & Persistence)

- [x] 1.1 Create `V4__Ballot_config_and_candidate_profiles.sql` adding `permite_voto_blanco`, `max_votos_por_elector` to `elecciones`, rich fields to `candidatos`, and dropping `votos_token_id_key`.
- [x] 1.2 Update `EleccionEntity`, `CandidatoEntity`, `VoteEntity` (remove unique constraint) and map new fields in `ElectionRepositoryAdapter` and `CandidateRepositoryAdapter`.
- [x] 1.3 Update domain models (`Election.java`, `Candidate.java`) and commands (`CreateElectionCommand`, `AddCandidateCommand`) to include new properties with validation.
- [x] 1.4 Update `CreateElectionUseCaseImpl` and `AddCandidateUseCaseImpl` to persist the new properties.
- [x] 1.5 Update `ActivateElectionUseCaseImpl` to conditionally create "Voto en Blanco" only when `permiteVotoBlanco` is true. Existing logic for "Voto Nulo" must remain completely unchanged.
- [x] 1.6 Update `CastVoteByTokenIdPort` to accept `List<UUID> candidatoIds` instead of a single ID.
- [x] 1.7 Update `CastVoteByTokenIdUseCaseImpl` to deduplicate candidates, validate count against `maxVotosPorElector`, enforce blank-vote mutual exclusivity, insert N `Vote` objects, and emit audit logs.
- [x] 1.8 Write/update JUnit domain tests for new use case behaviors (especially multi-vote and blank-vote exclusivity).

## Phase 2: Backend API & Orchestration

- [x] 2.1 Create `CreateElectionWithCandidatesAppService.java` to orchestrate `CreateElectionUseCase` and N `AddCandidateUseCase` calls in one `@Transactional` method.
- [x] 2.2 Update `CastVoteAppService.java` to handle the list of candidate IDs.
- [x] 2.3 Update `ElectionController.java` with the new `POST /api/v1/elections/full` endpoint and update DTOs.
- [x] 2.4 Update `PortalVotingController.java` to expose ballot config/rich fields in `GET /api/v1/portal/ballot/{eleccionId}` and handle JSON array in `POST /api/v1/portal/votar/{eleccionId}`.
- [x] 2.5 Write `@WebMvcTest` slice tests for new controller endpoints.
- [x] 2.6 Write Testcontainers `*IT.java` integration tests to verify multi-row vote inserts and comprehensive election creation.
- [x] 2.7 [CORRECTIVE] Add `@ExceptionHandler(IllegalArgumentException.class)` to `GlobalExceptionHandler` to map domain invariant violations from compact constructors (e.g. `maxVotosPorElector < 1`) to HTTP 400. Add 3 TDD-cycle WebMvc tests proving invalid semantic payloads return 400, not 500.
- [x] 2.8 [CORRECTIVE — gate retry] Fix `CreateElectionRequest` (legacy `POST /api/v1/elections`) to accept `permiteVotoBlanco` and `maxVotosPorElector` fields. Update `createElection()` to forward them to `CreateElectionCommand` instead of hardcoding `true`/`1`. Add 3 unit tests proving non-default ballot config flows through the legacy path (TDD cycle: RED → GREEN → TRIANGULATE).
- [x] 2.9 [CORRECTIVE — gate retry 2] Fix backward-compatibility regression: change `CreateElectionRequest.permiteVotoBlanco` and `maxVotosPorElector` from Java primitives (`boolean`/`int`) to wrapper types (`Boolean`/`Integer`) so omitted JSON fields bind as `null` rather than `false`/`0`. Add `effectivePermiteVotoBlanco()` and `effectiveMaxVotosPorElector()` helpers applying historical defaults (`true`/`1`) when `null`. Update controller to call these helpers. Add 5 `@WebMvcTest` tests (TDD cycle: RED → GREEN → TRIANGULATE × 2) in `ElectionControllerLegacyBackCompatIT`.

## Phase 3: Frontend API & Types

- [x] 3.1 Update `/src/api/client.ts` to include `BallotConfig` and `CandidateProfile` interfaces, and add `elections.createFull(payload)` method.
- [x] 3.2 Update `/src/api/portalClient.ts` to expose `maxVotosPorElector`, `permiteVotoBlanco`, and rich candidate fields in `PortalBallotResponse`.
- [x] 3.3 Update `/src/api/portalClient.ts` `votar` method to send `{ candidatoIds: string[] }` body instead of path parameter.

## Phase 4: Frontend UI Implementation

- [x] 4.1 Refactor `CreateElectionPage.tsx` into a 4-step wizard (Basic Info, Candidates, Ballot Config, Review).
- [x] 4.2 Implement local state management (useState/useReducer) in `CreateElectionPage` to hold data across wizard steps.
- [x] 4.3 Wire the Final Review step to submit the comprehensive payload via `elections.createFull`.
- [x] 4.4 Update `VotePage.tsx` to render multi-select checkboxes (capped at `maxVotosPorElector`).
- [x] 4.5 Implement mutual exclusivity logic in `VotePage.tsx` (auto-clearing other selections if Blank vote is selected).
- [x] 4.6 Update frontend tests to cover wizard navigation and multi-vote constraints.