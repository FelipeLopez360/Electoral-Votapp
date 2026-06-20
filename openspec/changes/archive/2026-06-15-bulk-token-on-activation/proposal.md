# Proposal: bulk-token-on-activation

## Intent
Automate bulk voting token issuance when an election is activated, ensuring no manual intervention is required. Tokens are issued opaquely, dropping raw secrets, and making the activation + issuance process atomic and idempotent.

## Scope

### In Scope
- New domain use case `BulkIssueTokensUseCase` for bulk token generation.
- Update `ElectionTransitionAppService.activate()` to orchestrate election activation and bulk token issuance atomically.
- Route manual activation in `ElectionController` through `ElectionTransitionAppService`.
- Add bulk read method to `CensoRepositoryPort` to retrieve all census `funcionarioIds`.
- Add bulk insert method `saveAllIssued` to `VotingTokenRepository`.
- If election census is empty, issue tokens for ALL globally eligible officials.

### Out of Scope
- No frontend changes.
- No changes to single-token issuance flow.
- No changes to vote casting flow.
- No rawToken exposure or download.

## Capabilities

### New Capabilities
- `bulk-token-issuance`: Automatically issue tokens in bulk for all eligible funcionarios upon election activation.

### Modified Capabilities
- `election-activation`: Transition to ACTIVA now triggers bulk token issuance within the same transaction. Both manual and scheduled activation use the same path.

## Approach
Implement Application Service Orchestration (Option B).
- `ElectionTransitionAppService.activate(UUID)` becomes the single transactional entry point for both manual (Controller) and scheduled (Scheduler) activations.
- It will first call `ActivateElectionUseCase.activate(UUID)`.
- It will then call `BulkIssueTokensUseCase.issueForElection(UUID)`.
- `BulkIssueTokensUseCase` evaluates census: if populated, iterate and issue tokens; if empty, iterate all globally eligible funcionarios and issue tokens.
- Bulk token insertions are persisted using a new `VotingTokenRepository.saveAllIssued(...)` which MUST utilize database-level idempotency (`ON CONFLICT DO NOTHING`) to ensure safety on retries.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `co.com.votapp.ws.electoral.application.service.ElectionTransitionAppService` | Modified | Adds orchestration for `BulkIssueTokensUseCase` inside `@Transactional` activate method. |
| `co.com.votapp.ws.electoral.infrastructure.adapter.in.web.ElectionController` | Modified | Delegates manual activation to `ElectionTransitionAppService` instead of `ActivateElectionUseCase`. |
| `co.com.votapp.ws.voting.domain.port.in.BulkIssueTokensUseCase` | New | Input port for bulk token issuance. |
| `co.com.votapp.ws.voting.domain.usecase.BulkIssueTokensUseCaseImpl` | New | Implementation of bulk issuance logic. |
| `co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort` | Modified | Add method to list all funcionarioIds for an election. |
| `co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository` | Modified | Add `saveAllIssued` for batch idempotency. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Controller and Scheduler divergence | Low | Controller is being updated to use `ElectionTransitionAppService`, unifying the paths. |
| Race conditions on retry | Medium | Use `ON CONFLICT DO NOTHING` in repository bulk insert for `ISSUED` tokens based on partial unique index. |
| OOM on large census | Medium | Ensure repository bulk methods use streaming or reasonable batch sizes under the hood if census > 100k. |

## Rollback Plan
Since the operation is wrapped in a single `@Transactional`, any error during token issuance will automatically roll back the election activation and the blank candidate creation. If the deployed artifact fails post-release, reverting the code will remove the bulk token issuance call, restoring the manual single-token issuance flow on activation.

## Dependencies
- `CensoRepositoryPort` and `VotingTokenRepository` adapters must be updated before the new use case can be wired.

## Success Criteria
- [ ] Manual and scheduled activation both execute atomically.
- [ ] Transitioning an election to ACTIVA results in `ISSUED` tokens for all census members (or global officials if census is empty).
- [ ] Calling activation twice safely rejects or performs no duplicate insertions.
- [ ] No raw tokens are logged or returned to the client during activation.