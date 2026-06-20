# Design: Bulk Token Issuance on Activation

## Technical Approach

Implement proposal Option B (Application Service Orchestration). `ElectionTransitionAppService.activate()` becomes the single `@Transactional` entry point for BOTH manual (Controller) and scheduled (Scheduler) paths. It calls the existing `ActivateElectionUseCase.activate()`, then a NEW domain use case `BulkIssueTokensUseCase.issueForElection()`. A new voting-domain use case owns bulk token generation, reusing existing ports (`VoterEligibilityRepositoryPort.isEligibleForElection`, `VotingTokenRepository.existsIssuedTokenFor`) plus two new bulk port methods. rawToken is generated per funcionario, hashed (SHA-256), and immediately discarded.

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|---|---|---|---|
| Where bulk logic lives | New `BulkIssueTokensUseCase` in voting domain | Hook into `ActivateElectionUseCaseImpl` (electoral) | Token issuance is a voting concern; mixing it into electoral widens the use case and crosses bounded contexts. Matches exploration recommendation. |
| Eligibility check | Reuse `VoterEligibilityRepositoryPort.isEligibleForElection(Long, UUID)` per funcionario | Re-derive ACTIVO + puede_votar inline | Port already encapsulates global + census-aware rule. No duplication. |
| Idempotency | `VotingTokenRepository.saveAllIssued(...)` uses `INSERT ... ON CONFLICT DO NOTHING` on the partial unique index | Pre-check only via `existsIssuedTokenFor` | DB-level guard is race-safe on retry; pre-check alone races into the partial unique index. Mirrors existing `CensoRepositoryPort.saveAll` precedent. |
| Empty census | Fall back to ALL globally eligible (`FuncionarioRepositoryPort.findEligibleByFilters(null,"ACTIVO",true)`) | Reject activation / issue zero | Matches proposal scope ("if census empty, issue for all globally eligible") and preserves existing single-issue fallback behavior. |
| funcionarioId type boundary | Convert `Integer` (census/funcionario reads) → `Long` at the use-case boundary before calling `VotingToken`/eligibility/exists | Change `VotingToken.funcionarioId` to Integer | `VotingToken`, `existsIssuedTokenFor`, `isEligibleForElection` all use `Long`. Census and `Funcionario` use `Integer`. Convert at the seam — do NOT touch persisted domain types. |
| Transaction boundary | Keep `@Transactional` on app service; bulk runs in same tx | Separate tx per phase | All-or-nothing: if issuance fails, activation + blank candidate roll back together. |

## Data Flow

    Controller.activate ─┐
                         ├─→ ElectionTransitionAppService.activate() @Transactional
    Scheduler.activate ──┘         │
                                   ├─→ ActivateElectionUseCase.activate()   (status→ACTIVA, blank candidate)
                                   └─→ BulkIssueTokensUseCase.issueForElection()
                                            │
                                            ├─ CensoRepositoryPort.findAllFuncionarioIdsByEleccionId(id)
                                            │     (empty? → FuncionarioRepositoryPort.findEligibleByFilters)
                                            ├─ per id: isEligibleForElection + existsIssuedTokenFor
                                            │     eligible & no token → build VotingToken(ISSUED)
                                            └─ VotingTokenRepository.saveAllIssued(batch)  [ON CONFLICT DO NOTHING]

## File Changes

| File | Action | Description |
|---|---|---|
| `voting/domain/port/in/BulkIssueTokensUseCase.java` | Create | Input port + `BulkIssueResult(int issued, int skipped, int total)` record |
| `voting/domain/usecase/BulkIssueTokensUseCaseImpl.java` | Create | Bulk issuance logic; no Spring annotations |
| `electoral/domain/port/out/CensoRepositoryPort.java` | Modify | Add `List<Integer> findAllFuncionarioIdsByEleccionId(UUID)` (non-paginated) |
| `electoral/.../persistence/...CensoAdapter.java` | Modify | Implement new projection method |
| `voting/domain/port/out/VotingTokenRepository.java` | Modify | Add `List<VotingToken> saveAllIssued(List<VotingToken>)` |
| `voting/.../persistence/...VotingTokenAdapter.java` | Modify | Implement batch insert with `ON CONFLICT DO NOTHING` |
| `electoral/application/service/ElectionTransitionAppService.java` | Modify | Inject `BulkIssueTokensUseCase`; call after activate |
| `electoral/.../web/ElectionController.java` | Modify | Inject + route `/activate` through `ElectionTransitionAppService` instead of `ActivateElectionUseCase` |
| `config/DomainConfig.java` | Modify | Wire `BulkIssueTokensUseCaseImpl` bean |
| `infrastructure/.../scheduler/ElectionScheduler.java` | None | Already uses app service — inherits bulk issuance automatically |

## Interfaces / Contracts

```java
public interface BulkIssueTokensUseCase {
    BulkIssueResult issueForElection(UUID eleccionId);
    record BulkIssueResult(int issued, int skipped, int total) {}
}

// CensoRepositoryPort (new)
List<Integer> findAllFuncionarioIdsByEleccionId(UUID eleccionId);

// VotingTokenRepository (new) — adapter MUST use ON CONFLICT DO NOTHING
List<VotingToken> saveAllIssued(List<VotingToken> tokens);
```

`BulkIssueTokensUseCaseImpl` deps: `CensoRepositoryPort`, `VotingTokenRepository`, `VoterEligibilityRepositoryPort`, `FuncionarioRepositoryPort`. Token gen mirrors `IssueVotingTokenUseCaseImpl`: `SecureRandom` 32 bytes → hex → SHA-256. `Integer` ids converted to `Long` before `VotingToken`/eligibility/exists calls.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit (`BulkIssueTokensUseCaseImpl`) | issues for populated census; falls back to global on empty census; skips ineligible; skips existing ISSUED; correct counts; rawToken never returned/logged | `@ExtendWith(MockitoExtension)`, mock all 4 ports, AssertJ |
| Unit (`ElectionTransitionAppService`) | activate then bulk-issue called in order; issuance failure propagates (rollback) | Mock both use cases, `InOrder` verify |
| Slice (`ElectionController` *IT*) | `/activate` delegates to app service, returns 204 | `@WebMvcTest`, `@MockitoBean` app service |
| Integration *IT* | activation → ISSUED tokens for census; double-activate inserts no duplicates (ON CONFLICT) | `@SpringBootTest` + Testcontainers PostgreSQL |

## Migration / Rollout

No data migration. Schema unchanged — relies on existing partial unique index on `(eleccion_id, funcionario_id) WHERE status='ISSUED'`. Reverting the code restores prior activation-only behavior.

## Open Questions

- [ ] Confirm partial unique index column type matches `funcionario_id` used in `saveAllIssued` (Integer in DB vs Long in domain — verify adapter mapping during apply).
