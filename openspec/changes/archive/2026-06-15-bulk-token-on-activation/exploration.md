## Exploration: bulk-token-on-activation

### Current State
`ActivateElectionUseCase` exposes `void activate(UUID electionId)`. Its implementation (`ActivateElectionUseCaseImpl`) is a pure domain use case that loads the election, validates `PROGRAMADA`, persists the `ACTIVA` transition, and creates the synthetic `Voto en Blanco` candidate.

There is already a transactional wrapper: `ElectionTransitionAppService`. The scheduler uses it, but the manual REST endpoint does not. `ElectionScheduler` runs every 60 seconds, finds due elections, and calls `electionTransitionAppService.activate(election.id())` per election inside isolated `try/catch` blocks. `ElectionController` still injects `ActivateElectionUseCase` directly and calls `activateElectionUseCase.activate(...)`, so the manual path currently bypasses the app-service transaction boundary.

`IssueVotingTokenUseCaseImpl` only supports single-token issuance and returns an `IssuedVotingToken` containing the ephemeral `rawToken`. That shape does not fit activation-time bulk issuance, because activation must discard the raw token. It also depends on `isEligibleForElection`, `existsIssuedTokenFor`, and `hasParticipated`, but there is no bulk variant.

`CensoRepositoryPort` does not expose a "list all funcionario IDs" method. It only offers paginated reads via `findByEleccionId(UUID, int, int)`. `VotingTokenRepository` only exposes single-row `saveIssued(...)`; its adapter uses plain JPA `save`, not `ON CONFLICT DO NOTHING`. The database already has a partial unique index on `(eleccion_id, funcionario_id)` for `status = 'ISSUED'`, so the schema supports idempotency, but the current repository contract does not.

The portal already resolves voting by session, not by raw token: `PortalVotingController` uses `findIssuedByFuncionarioAndEleccion(funcionarioId, eleccionId)` and `findAllByFuncionarioId(funcionarioId)`. That means bulk-issued tokens can remain fully opaque to the user-facing flow.

### Affected Areas
- `src/main/java/co/com/votapp/ws/electoral/domain/usecase/ActivateElectionUseCaseImpl.java` — current activation logic only changes status and creates blank vote candidate.
- `src/main/java/co/com/votapp/ws/electoral/domain/port/in/ActivateElectionUseCase.java` — current input port is `void activate(UUID)`.
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/ElectionController.java` — manual activate endpoint calls the domain use case directly, bypassing the transactional wrapper.
- `src/main/java/co/com/votapp/ws/electoral/application/service/ElectionTransitionAppService.java` — existing `@Transactional` orchestration point used by the scheduler.
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/scheduler/ElectionScheduler.java` — auto-activation already routes through the app service.
- `src/main/java/co/com/votapp/ws/voting/domain/usecase/IssueVotingTokenUseCaseImpl.java` — single-token issuance logic that returns `rawToken` and is not bulk-oriented.
- `src/main/java/co/com/votapp/ws/electoral/domain/port/out/CensoRepositoryPort.java` — missing bulk-read/list-all method for census members.
- `src/main/java/co/com/votapp/ws/voting/domain/port/out/VotingTokenRepository.java` — no bulk/idempotent insert contract.
- `src/main/java/co/com/votapp/ws/config/DomainConfig.java` — bean wiring will need updates for any new bulk issuance use case or app-service dependencies.
- `src/test/java/co/com/votapp/ws/electoral/domain/usecase/ActivateElectionUseCaseTest.java` — covers activation and blank-vote creation only.
- `src/test/java/co/com/votapp/ws/electoral/application/service/ElectionTransitionAppServiceTest.java` — proves the transactional wrapper exists.
- `src/test/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/scheduler/ElectionSchedulerTest.java` — proves scheduler uses the app service and isolates failures per election.
- `src/test/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/ElectionControllerTest.java` — proves manual activation still delegates directly to `ActivateElectionUseCase`.

### Approaches
1. **Hook in `ActivateElectionUseCaseImpl`** — after the status change, iterate the census and issue tokens inside the activation use case.
   - Pros: one obvious activation entry point; keeps scheduler and controller behavior aligned if both continue calling the same use case.
   - Cons: widens an electoral domain use case with voting responsibilities; current controller path is still not transactionally wrapped; `CensoRepositoryPort` lacks bulk read support; `IssueVotingTokenUseCaseImpl` cannot be reused as-is because it returns `rawToken`; likely forces duplicated issuance logic or awkward cross-use-case coupling.
   - Effort: Medium

2. **Application service orchestration** — keep `ActivateElectionUseCase` focused on election transition, and let `ElectionTransitionAppService` orchestrate activation plus a new bulk token issuance use case inside one transaction.
   - Pros: best fit with current architecture; reuses the existing `@Transactional` boundary; scheduler already uses this path; manual endpoint can be moved to the same orchestration; activation and token issuance can rollback atomically per election; bulk use case can return counts instead of `rawToken`.
   - Cons: requires new orchestration in the application layer, a new bulk issuance use case/port contract, controller wiring changes, and new census/token repository methods.
   - Effort: Medium

3. **Domain event** — activation emits an event and a listener performs token issuance.
   - Pros: decouples activation from token issuance and leaves room for future side effects.
   - Cons: highest complexity for MVP; async listeners break atomicity; synchronous transactional events pull Spring/event concerns into a flow that is currently simple; failure semantics become harder to reason about.
   - Effort: High

### Recommendation
Use **Option B**. Extend `ElectionTransitionAppService.activate(...)` so it becomes the single orchestration path for both manual and scheduled activation, and add a dedicated bulk token issuance use case that reads census members and inserts missing `ISSUED` tokens inside the same transaction.

Do **not** reuse `IssueVotingTokenUseCase` as-is for the activation flow. Its current contract is single-item and returns `rawToken`, which activation explicitly discards. If code reuse is needed, extract shared token-generation/hash creation into a smaller collaborator or helper owned by the voting domain, then let single-issue and bulk-issue use cases call that shared logic with different return semantics.

### Risks
- Manual activation is currently non-uniform with scheduled activation: the controller bypasses `ElectionTransitionAppService`, so atomic activation + token issuance would be unreliable unless that path is changed.
- `CensoRepositoryPort` cannot currently enumerate all census members without pagination loops or a new projection method.
- `VotingTokenRepository.saveIssued(...)` is not idempotent at the repository boundary; retries will rely on pre-checks and can still race into the partial unique index.
- The current single-issue eligibility flow has an empty-census fallback (`countByEleccionId == 0` → globally eligible). Bulk issuance needs an explicit business rule for empty census: issue zero tokens, reject activation, or intentionally preserve a fallback. The current requirement text implies "issue for census members only," so this needs to be nailed down in proposal/spec.

### Ready for Proposal
Yes — but the proposal should explicitly state that:
- both manual and scheduled activation MUST go through `ElectionTransitionAppService`;
- bulk issuance MUST be atomic with activation per election;
- token creation for activation MUST be idempotent on retry (prefer repository-level `ON CONFLICT DO NOTHING` or equivalent contract);
- bulk issuance MUST use census membership as the source of truth, not the single-token empty-census fallback.
