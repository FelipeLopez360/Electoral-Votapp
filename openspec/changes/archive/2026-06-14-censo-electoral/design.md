# Design: Censo Electoral (Per-Election Voter Census)

## Technical Approach

Materialized census (Option A). New `censo_electoral` table holds explicit `(eleccion_id, funcionario_id)` membership. The **electoral** context owns census configuration (bulk/individual add, remove, list) via a new `ManageCensoUseCase` + `CensoRepositoryPort`. The **votereligibility** context becomes election-aware: token issuance now asks "is this funcionario eligible *for this election*?" — which means global (`ACTIVO` + `puede_votar`) **AND** census membership when a census exists. Empty census → fall back to global-only (backward compatible). Filtered funcionario reads for bulk selection are added to the existing `FuncionarioRepositoryPort` (auth), reused by the electoral adapter following the established cross-context adapter precedent (`VoterEligibilityRepositoryAdapter` already reuses `FuncionarioJpaRepository`).

IDs follow existing types: `funcionarioId` is `Long`/`Integer` (SERIAL), `eleccionId` is `UUID`.

## Architecture Decisions

| Decision | Choice | Alternative rejected | Rationale |
|---|---|---|---|
| Census ownership | `electoral` context | `votereligibility` | Census IS election setup config; electoral already owns lifecycle + candidates. |
| Election-scoped eligibility boundary | Add overload `isEligibleForElection(funcionarioId, eleccionId)` to `VoterEligibilityRepositoryPort`; its adapter reads census | Inject an `electoral` census port into `voting` use case | Keeps `voting` depending on ONE eligibility port (no electoral→voting leak). The adapter is the natural seam to join global + census reads. |
| Census read seam for issuance | `votereligibility` adapter reuses `CensoJpaRepository` (cross-context reuse) | Call electoral `CensoRepositoryPort` from voting | Mirrors existing `VoterEligibilityRepositoryAdapter` reusing `FuncionarioJpaRepository`; no domain coupling. |
| Backward compatibility | If `count(censo for eleccion) == 0` → global-only check | Force census for every election | Preserves behavior for elections created before this feature (success criteria + rollback). |
| Filtered funcionario reads | Extend `FuncionarioRepositoryPort` (auth) with `findEligibleByFilters(...)` | New read port in electoral | Auth owns funcionarios; electoral adapter reuses its JPA repo, consistent with current pattern. |
| State guard | Use case loads election via `ElectionRepositoryPort`, rejects if `status != PROGRAMADA` | Guard in controller only | Matches `ActivateElectionUseCaseImpl` pattern; business rule belongs in domain. |
| Bulk insert dedup | DB `UNIQUE(eleccion_id, funcionario_id)` + `INSERT ... ON CONFLICT DO NOTHING` (native query in adapter) | Read-then-filter in Java | Atomic, race-safe, returns accurate `added` via affected rows. |
| Migration version | `V2__Censo_electoral.sql` | Extend V1 | Proposal explicitly accepts production-like history over the "V1 canonical" convention. |
| Token issuance fix | Add `ACTIVA` election check (drift noted in exploration) | Leave as-is | Exploration flagged spec drift: command/controller claim ACTIVA but impl never enforces it. Fix alongside census. |

## Data Flow

Bulk add (admin):

    CensoController ──> ManageCensoUseCase ──> ElectionRepositoryPort (guard PROGRAMADA)
                                          ├──> FuncionarioRepositoryPort.findEligibleByFilters(...)
                                          └──> CensoRepositoryPort.addAll(...)  [ON CONFLICT DO NOTHING]
                                                   returns {added, skipped, total}

Token issuance (election-scoped):

    TokenController ──> IssueVotingTokenUseCaseImpl
        ├──> ElectionRepositoryPort.findById  (must be ACTIVA)            [NEW guard]
        ├──> VoterEligibilityRepositoryPort.isEligibleForElection(fId, eId)
        │        └─(adapter)─> global ACTIVO+puede_votar AND (no census OR in census)
        ├──> VotingTokenRepository.existsIssuedTokenFor
        └──> ParticipacionRepositoryPort.hasParticipated

## File Changes

| File | Action | Description |
|---|---|---|
| `db/migration/V2__Censo_electoral.sql` | Create | `censo_electoral` table, UNIQUE+FK+indexes |
| `electoral/domain/model/CensoEntry.java` | Create | Pure record `(eleccionId, funcionarioId, agregadoPor, fechaAgregado)` |
| `electoral/domain/port/in/ManageCensoUseCase.java` | Create | Input port: bulkAdd, add, remove, clear |
| `electoral/domain/port/out/CensoRepositoryPort.java` | Create | Output port: addAll, add, remove, clear, list(page), countByEleccion |
| `electoral/domain/usecase/ManageCensoUseCaseImpl.java` | Create | No Spring annotations; PROGRAMADA guard |
| `electoral/application/dto/BulkAddCensoRequest.java` | Create | Self-validating record |
| `electoral/application/dto/BulkAddCensoResponse.java` | Create | `{added, skipped, total}` |
| `electoral/application/dto/CensoEntryResponse.java` | Create | List projection |
| `electoral/infrastructure/adapter/in/web/CensoController.java` | Create | REST under `/api/v1/elections/{electionId}/censo` |
| `electoral/infrastructure/adapter/out/persistence/CensoEntity.java` | Create | JPA entity for `censo_electoral` |
| `electoral/infrastructure/adapter/out/persistence/CensoJpaRepository.java` | Create | Spring Data + native upsert + paged list |
| `electoral/infrastructure/adapter/out/persistence/CensoRepositoryAdapter.java` | Create | Implements `CensoRepositoryPort` |
| `config/DomainConfig.java` | Modify | Wire `ManageCensoUseCaseImpl`; add deps to `IssueVotingTokenUseCaseImpl` bean |
| `votereligibility/domain/port/out/VoterEligibilityRepositoryPort.java` | Modify | Add `isEligibleForElection(Long, UUID)` |
| `votereligibility/.../VoterEligibilityRepositoryAdapter.java` | Modify | Reuse `CensoJpaRepository`; global AND census (empty-census fallback) |
| `auth/domain/port/out/FuncionarioRepositoryPort.java` | Modify | Add `findEligibleByFilters(departamentoId, estadoLaboral, puedeVotar)` |
| `auth/.../FuncionarioJpaRepository.java` | Modify | Add filtered query |
| `voting/domain/usecase/IssueVotingTokenUseCaseImpl.java` | Modify | Add `ElectionRepositoryPort` (ACTIVA guard) + use election-scoped eligibility |

## Interfaces / Contracts

```java
// electoral/domain/port/out
public interface CensoRepositoryPort {
    int addAll(UUID eleccionId, List<Long> funcionarioIds, Long agregadoPor); // returns inserted count
    boolean add(UUID eleccionId, Long funcionarioId, Long agregadoPor);
    boolean remove(UUID eleccionId, Long funcionarioId);
    int clear(UUID eleccionId);
    long countByEleccion(UUID eleccionId);
    List<CensoEntry> list(UUID eleccionId, int page, int size, String search);
}

// votereligibility/domain/port/out (added method)
boolean isEligibleForElection(Long funcionarioId, UUID eleccionId);
```

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `ManageCensoUseCaseImpl`: PROGRAMADA guard, bulk add delegates, added/skipped math | `@ExtendWith(MockitoExtension)`, mock `ElectionRepositoryPort`, `FuncionarioRepositoryPort`, `CensoRepositoryPort` |
| Unit | `IssueVotingTokenUseCaseImpl`: ACTIVA guard, election-scoped eligibility rejection | Mock ports incl. new `ElectionRepositoryPort` |
| Slice | `CensoController` HTTP codes (201/204/400/409) | `@WebMvcTest`, `@MockitoBean` use case |
| Integration | `CensoRepositoryAdapter` upsert ON CONFLICT, paged list; empty-census fallback in eligibility | `*IT.java` Testcontainers PostgreSQL, `@ServiceConnection` |

## Migration / Rollout

```sql
CREATE TABLE censo_electoral (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    eleccion_id    UUID    NOT NULL REFERENCES elecciones(id) ON DELETE CASCADE,
    funcionario_id INTEGER NOT NULL REFERENCES funcionarios(id),
    agregado_por   INTEGER REFERENCES funcionarios(id),
    created_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (eleccion_id, funcionario_id)
);
CREATE INDEX idx_censo_eleccion ON censo_electoral(eleccion_id);
```
Rollback: revert app code, `DROP TABLE censo_electoral;` → issuance resumes global-only.

## Open Questions

- [ ] `agregado_por` source: admin funcionario id is available from Basic auth principal? If not, store NULL in MVP.
