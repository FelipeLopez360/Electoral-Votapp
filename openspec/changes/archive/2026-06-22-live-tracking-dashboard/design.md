# Design: Live Tracking Dashboard Widget

## Technical Approach

Expose `GET /api/v1/dashboard/live-tracking` returning per-`ACTIVA` election progress
(`totalCastVotes`, `totalEligibleVoters`). A new application service
`DashboardLiveTrackingService` (in `electoral/application/service/`) orchestrates four
output ports — three already exist, two new methods are added. This mirrors the existing
`DashboardMetricsService` pattern: a pure-read `@Service` with zero business rules that
composes output-port calls and assembles a DTO for the web adapter. No domain use case is
introduced (consistent with `DashboardMetricsService` and `ElectionController` list queries).

The eligibility fallback (census-first, then global active voters) lives in the service as a
read-composition rule, matching how the metrics service already zero-fills and aggregates.

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|----------|--------|----------------------|-----------|
| Service placement | New `DashboardLiveTrackingService` in `electoral/application/service/` | Extend `DashboardMetricsService` (proposal default); new domain use case | Single responsibility: live-tracking polls frequently and has a distinct DTO/lifecycle from the static metrics aggregate. No invariant to enforce → not a domain use case. |
| Cross-context wiring | Inject `ParticipacionRepositoryPort` (voting) + `FuncionarioRepositoryPort` (auth) + `ElectionRepositoryPort`/`CensoRepositoryPort` (electoral) by interface | Call other contexts' services/controllers | `DashboardMetricsService` already injects `FuncionarioRepositoryPort` cross-context by port. Ports are the sanctioned seam; no module-bound violation. |
| `findByStatus` | Add `List<Election> findByStatus(ElectionStatus)` to `ElectionRepositoryPort` | Reuse `findByStatusAndFechaInicio/FinLessThanEqual` | Existing methods are time-bounded (scheduler-specific). Live tracking needs all `ACTIVA` rows regardless of dates. |
| Participation count | Add `long countByEleccionId(UUID)` to `ParticipacionRepositoryPort` + Spring Data derived `countByEleccionId` | Load all rows and count in memory | DB `COUNT` is O(1) index scan; in-memory load defeats the "lightweight polling" goal. |
| Eligibility fallback location | In `DashboardLiveTrackingService` | Push into a port/SQL join | Keeps the rule explicit and unit-testable by mocking ports; matches spec scenarios 1:1. |

## Data Flow

    DashboardController ──→ DashboardLiveTrackingService
                                  │
        ┌─────────────────────────┼──────────────────────────┐
        ▼                         ▼                          ▼
  ElectionRepositoryPort   ParticipacionRepositoryPort   CensoRepositoryPort
   .findByStatus(ACTIVA)    .countByEleccionId(id)        .countByEleccionId(id)
        │                                                     │ (== 0?)
        │                                                     ▼
        └──────────────────────────────► FuncionarioRepositoryPort
                                           .countActiveEligibleVoters()
                                  │
                                  ▼
                    List<LiveTrackingItem> → LiveTrackingResponse (JSON)

Per active election: `totalCastVotes = participacion.countByEleccionId(id)`;
`censusCount = censo.countByEleccionId(id)`; `totalEligibleVoters = censusCount > 0 ? censusCount
: funcionario.countActiveEligibleVoters()`. Global voters is queried ONLY when census == 0.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `voting/domain/port/out/ParticipacionRepositoryPort.java` | Modify | Add `long countByEleccionId(UUID eleccionId)` |
| `voting/.../persistence/ParticipacionJpaRepository.java` | Modify | Add derived `long countByEleccionId(UUID eleccionId)` |
| `voting/.../persistence/ParticipacionRepositoryAdapter.java` | Modify | Implement `countByEleccionId` delegating to JPA repo |
| `electoral/domain/port/out/ElectionRepositoryPort.java` | Modify | Add `List<Election> findByStatus(ElectionStatus status)` |
| `electoral/.../persistence/EleccionJpaRepository.java` | Modify | Add derived `List<EleccionEntity> findByEstado(String estado)` |
| `electoral/.../persistence/ElectionRepositoryAdapter.java` | Modify | Implement `findByStatus` mapping entities → domain |
| `electoral/application/dto/LiveTrackingResponse.java` | Create | DTO: `List<LiveTrackingItem>` (id, title, totalCastVotes, totalEligibleVoters) |
| `electoral/application/service/DashboardLiveTrackingService.java` | Create | Orchestrates the 4 ports + fallback rule |
| `electoral/.../web/DashboardController.java` | Modify | Add `GET /live-tracking` mapping → service |

`CensoRepositoryPort.countByEleccionId(UUID)` and `FuncionarioRepositoryPort.countActiveEligibleVoters()`
already exist — no change.

## Interfaces / Contracts

```java
// electoral/application/dto/LiveTrackingResponse.java — pure record, no Spring/JPA
public record LiveTrackingResponse(List<LiveTrackingItem> elections) {
    public LiveTrackingResponse { Objects.requireNonNull(elections); }
    public record LiveTrackingItem(UUID id, String title,
                                   long totalCastVotes, long totalEligibleVoters) {}
}

// new port methods
long ParticipacionRepositoryPort.countByEleccionId(UUID eleccionId);
List<Election> ElectionRepositoryPort.findByStatus(ElectionStatus status);
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (service) | Fallback: census>0 → census, NO global call; census==0 → global; empty list; zero votes | `@ExtendWith(MockitoExtension)`, mock all 4 ports, `verify(funcionario, never())` on census path |
| Unit (adapter) | `findByStatus` mapping; `countByEleccionId` delegation | Mock JPA repos |
| Integration | `GET /live-tracking` 200, empty list, multi-election, census vs global counts | `@SpringBootTest` + Testcontainers (Postgres) |

## Migration / Rollout

No migration required. No schema changes — both new queries read existing tables
(`participacion_electoral`, `elecciones`). Rollback = remove the mapping + new methods.

## Open Questions

- [ ] Endpoint security: assume it inherits the admin HTTP Basic chain (Order 2) like `/metrics`. Confirm during apply if a distinct auth tier is required.
