# Tasks: Censo Electoral

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 800 - 1000 lines |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 |
| Delivery strategy | ask-always |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Schema + Persistence Foundation | PR 1 | Base branch: main; adds V2 migration, JPA repos, and Auth port extensions. |
| 2 | Electoral Domain & Adapters | PR 2 | Base branch: PR 1; adds CensoEntry, UseCase, CensoRepositoryAdapter, and tests. |
| 3 | Eligibility & Token Issuance | PR 3 | Base branch: PR 2; updates VoterEligibilityAdapter and IssueVotingTokenUseCaseImpl with tests. |
| 4 | REST API & Controllers | PR 4 | Base branch: PR 3; adds CensoController, DTOs, and WebMvc tests. |

## Phase 1: Schema and Persistence Foundation

- [x] 1.1 Create migration script `src/main/resources/db/migration/V2__Censo_electoral.sql` to add `censo_electoral` table.
- [x] 1.2 Create `CensoEntity.java` in `co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence` mapping to the new table.
- [x] 1.3 Create `CensoJpaRepository.java` in `electoral/infrastructure/adapter/out/persistence` with standard CRUD and a native query for idempotent bulk insert (`INSERT ... ON CONFLICT DO NOTHING`).
- [x] 1.4 (PR1-scope) Create domain record `CensoEntry.java` in `co/com/votapp/ws/electoral/domain/model` and output port `CensoRepositoryPort.java` in `co/com/votapp/ws/electoral/domain/port/out`.
- [x] 1.5 (PR1-scope) Create `CensoRepositoryAdapter.java` implementing `CensoRepositoryPort` via `CensoJpaRepository`.

## Phase 2: Electoral Domain & Adapters

- [x] 2.1 Create domain record `CensoEntry.java` in `co/com/votapp/ws/electoral/domain/model`.
- [x] 2.2 Create output port `CensoRepositoryPort.java` in `co/com/votapp/ws/electoral/domain/port/out`.
- [x] 2.3 Create input port `ManageCensoUseCase.java` in `co/com/votapp/ws/electoral/domain/port/in`.
- [x] 2.4 Implement TDD RED -> GREEN for `CensoRepositoryAdapter.java` implementing `CensoRepositoryPort` by writing `CensoRepositoryAdapterIT.java` (Testcontainers).
- [x] 2.5 Implement TDD RED -> GREEN for `ManageCensoUseCaseImpl.java` enforcing `PROGRAMADA` status. Write tests in `ManageCensoUseCaseImplTest.java` mocking ports.
- [x] 2.6 Wire `ManageCensoUseCaseImpl` in `co/com/votapp/ws/config/DomainConfig.java`.

## Phase 3: Eligibility & Token Issuance Modification

- [ ] 3.1 Update `co/com/votapp/ws/votereligibility/domain/port/out/VoterEligibilityRepositoryPort.java` to add `isEligibleForElection(Long funcionarioId, UUID eleccionId)`.
- [ ] 3.2 Update `co/com/votapp/ws/votereligibility/infrastructure/adapter/out/persistence/VoterEligibilityRepositoryAdapter.java` to implement the new method checking global AND election-scoped census fallback (injecting `CensoJpaRepository`).
- [ ] 3.3 Update `co/com/votapp/ws/voting/domain/usecase/IssueVotingTokenUseCaseImplTest.java` (TDD RED): Add tests asserting rejection if not in census, and rejection if election is not `ACTIVA`.
- [ ] 3.4 Update `co/com/votapp/ws/voting/domain/usecase/IssueVotingTokenUseCaseImpl.java` (GREEN & REFACTOR): Add `ElectionRepositoryPort` to constructor to enforce `ACTIVA` state, and use `isEligibleForElection` from `VoterEligibilityRepositoryPort`.
- [ ] 3.5 Update `co/com/votapp/ws/config/DomainConfig.java` to supply the new dependency to `IssueVotingTokenUseCaseImpl`.

## Phase 4: API Layer and Controllers

- [ ] 4.1 Create DTO records `BulkAddCensoRequest.java`, `BulkAddCensoResponse.java`, and `CensoEntryResponse.java` in `co/com/votapp/ws/electoral/application/dto`.
- [ ] 4.2 Create `CensoController.java` in `co/com/votapp/ws/electoral/infrastructure/adapter/in/web` with endpoints for bulk add, individual add/remove, clear, and list.
- [ ] 4.3 Write WebMvc slice tests in `co/com/votapp/ws/electoral/infrastructure/adapter/in/web/CensoControllerIT.java` asserting HTTP 201/204/400/409 codes and proper delegation to the use case.
