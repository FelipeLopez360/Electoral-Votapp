# Tasks: bulk-token-on-activation

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350-450 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 |
| Delivery strategy | ask-always |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: Medium

**Resolved delivery**: single PR (user approved)

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Domain & Persistence (Ports, Adapters, Use Case) | PR 1 | Base: main; includes unit and integration tests for use case and adapters |
| 2 | Application Integration (AppService, Controller) | PR 2 | Base: PR 1; updates app service, controller, and their tests |

## Phase 1: Domain Ports & Persistence Adapters

- [x] 1.1 Add `findAllFuncionarioIdsByEleccionId(UUID)` to `CensoRepositoryPort`.
- [x] 1.2 Implement `findAllFuncionarioIdsByEleccionId` in `CensoRepositoryAdapter` using a new query in `JpaCensoRepository`.
- [x] 1.3 Add `saveAllIssued(List<VotingToken>)` to `VotingTokenRepository`.
- [x] 1.4 Implement `saveAllIssued` in `VotingTokenRepositoryAdapter` with `ON CONFLICT DO NOTHING` on the partial unique index.
- [x] 1.5 Write/update integration tests for `CensoRepositoryAdapter` and `VotingTokenRepositoryAdapter`.
- [x] 1.5-R1 (Remediation) Add `VotingTokenRepositoryAdapterTest` unit test proving `saveAllIssued` returns only inserted tokens.
- [x] 1.5-R2 (Remediation) Add `VotingTokenRepositoryAdapterIT` proving duplicate skipping, complete retry safety (0 new tokens), and rollback/no-partial-tokens.
- [x] 2.2-R1 (Remediation) Fix `BulkIssueTokensUseCaseImpl.issueForElection` to derive `issued` from `saveAllIssued` return value (not `tokensToSave.size()`).
- [x] 2.4-R1 (Remediation) Add unit tests proving retry reports `issued=0` and DB-level partial conflicts are reflected in counts.

## Phase 2: Core Domain Logic

- [x] 2.1 Create `BulkIssueTokensUseCase` input port with `BulkIssueResult(int issued, int skipped, int total)` record.
- [x] 2.2 Create `BulkIssueTokensUseCaseImpl` implementing the bulk issuance logic (check census, fallback to global, iterate, check eligibility, generate tokens, saveAllIssued).
- [x] 2.3 Wire `BulkIssueTokensUseCase` in `DomainConfig`.
- [x] 2.4 Write unit tests for `BulkIssueTokensUseCaseImpl` using Mockito.

## Phase 3: Application & Web Integration

- [x] 3.1 Inject `BulkIssueTokensUseCase` into `ElectionTransitionAppService`.
- [x] 3.2 Update `ElectionTransitionAppService.activate(UUID)` to call `bulkIssueTokensUseCase.issueForElection(UUID)` after activation.
- [x] 3.3 Update `ElectionController` to inject and call `ElectionTransitionAppService.activate(UUID)` instead of `ActivateElectionUseCase`.
- [x] 3.4 Update `ElectionTransitionAppServiceTest` to verify the new orchestration order.
- [x] 3.5 Update `ElectionControllerTest` slice tests to reflect the new dependency and behavior.
