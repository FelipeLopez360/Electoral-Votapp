# Design: Admin Operations Dashboard Metrics

> **Canonical change identity**: `panel-operaciones considero que debemos tener en el inicio al igual que elecciones total, programadas, activas y finalizadas, pero para funcionarios, asi sabremos cuantos funcionarios tenemos, su estado, votantes activos, etc [Image 1]`
> Engram topic key for this artifact: `sdd/panel-operaciones considero que debemos tener en el inicio al igual que elecciones total, programadas, activas y finalizadas, pero para funcionarios, asi sabremos cuantos funcionarios tenemos, su estado, votantes activos, etc [Image 1]/design`
> **Stale artifact warning**: `openspec/changes/admin-dashboard-metrics/proposal.md` is a SUPERSEDED orphan slug (proposal-only, no spec/design/tasks). Do NOT follow it. The canonical lineage is this folder.

## Technical Approach

Add one read-only aggregate endpoint `GET /api/v1/dashboard/metrics` that composes counts from the Electoral and Auth/Funcionario data already persisted. No new table, no schema change. Counts use `GROUP BY` JPQL aggregates in the existing JPA repositories, exposed through new count-only methods on existing output ports, orchestrated by a thin `@Service` placed inside the **electoral** bounded context's `application/service/` — the same home and pattern as `CreateElectionWithCandidatesAppService`. Reads bypass domain use cases, consistent with `ElectionController`/`FuncionarioController` calling output ports directly for ruleless queries.

`votantes activos` is fixed as the explicit assumption (carried from the spec, unchanged): globally eligible funcionarios where `estado_laboral = 'ACTIVO'` AND `puede_votar = true` — the exact predicate already used by `FuncionarioJpaRepository.findEligibleByDepartamento`.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| Package boundary | Compose inside the **electoral** context (`electoral/application/service/` + `electoral/infrastructure/adapter/in/web/`) | New top-level `dashboard` context | AGENTS.md lists exactly 4 bounded contexts; a 5th cross-cutting context owns no aggregate and violates "smallest architecture". Electoral ALREADY depends on auth's `FuncionarioRepositoryPort` (census bulk-add), so electoral→auth read reuse is an existing, sanctioned direction. |
| Endpoint shape | Single aggregate `GET /api/v1/dashboard/metrics` | Per-context count endpoints | One round-trip for the home panel; proposal goal "don't abuse paginated APIs". URL path ≠ package: lives in electoral web adapter, served by the admin chain. |
| Orchestration type | `@Service` app service (component-scanned) | New domain use case + input port | Pure read composition, zero business rule; mirrors existing app-service convention. No `DomainConfig` wiring. |
| Election port target | `ElectionRepositoryPort` → `ElectionRepositoryAdapter` (backed by `EleccionJpaRepository`) | `EleccionRepositoryPort`/`EleccionRepositoryAdapter` | CORRECTED: the legacy `EleccionRepositoryPort` only exposes `findByCodigo` on the deprecated `Eleccion` class and has NO status concept. `ElectionRepositoryPort` is the MVP aggregate that owns `ElectionStatus`. |
| Status grouping | `Map<String,Long>` keyed by status name, all enum values zero-filled | Fixed fields per status | Resilient to the 5 `ElectionStatus` values; empty DB yields zeros. |

## Data Flow

    GET /api/v1/dashboard/metrics  (admin Basic-auth chain, Order 2)
        │
        ▼
    DashboardController (electoral/infrastructure/adapter/in/web)
        │
        ▼
    DashboardMetricsService (@Service, electoral/application/service)
        ├──► FuncionarioRepositoryPort.countByEstadoLaboral()      ─► FuncionarioJpaRepository (GROUP BY estadoLaboral)
        ├──► FuncionarioRepositoryPort.countActiveEligibleVoters() ─► FuncionarioJpaRepository (ACTIVO + puedeVotar=true)
        └──► ElectionRepositoryPort.countByStatus()                ─► EleccionJpaRepository (GROUP BY estado)
        │
        ▼
    DashboardMetricsResponse (record) ──► JSON

## File Changes (exact repository paths)

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/DashboardController.java` | Create | `@RestController` `GET /api/v1/dashboard/metrics`; admin Basic-auth (already covered by `/api/**` chain). |
| `src/main/java/co/com/votapp/ws/electoral/application/service/DashboardMetricsService.java` | Create | `@Service`; injects both output ports, assembles response, zero-fills status maps. |
| `src/main/java/co/com/votapp/ws/electoral/application/dto/DashboardMetricsResponse.java` | Create | Record: funcionarios block, `activeVoters`, elections block. |
| `src/main/java/co/com/votapp/ws/auth/domain/port/out/FuncionarioRepositoryPort.java` | Modify | Add `Map<String,Long> countByEstadoLaboral()`, `long countActiveEligibleVoters()`. |
| `src/main/java/co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioRepositoryAdapter.java` | Modify | Implement new methods; map JPQL rows → `Map<String,Long>`/primitive. |
| `src/main/java/co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioJpaRepository.java` | Modify | Add `GROUP BY estadoLaboral` count + eligible-voter count JPQL. |
| `src/main/java/co/com/votapp/ws/electoral/domain/port/out/ElectionRepositoryPort.java` | Modify | Add `Map<String,Long> countByStatus()`. |
| `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/ElectionRepositoryAdapter.java` | Modify | Implement `countByStatus()`; map `estado` String rows → counts. |
| `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/EleccionJpaRepository.java` | Modify | Add `GROUP BY estado` aggregate JPQL (table-backing repo, name unchanged). |

No `DomainConfig` change: `DashboardMetricsService` is a component-scanned `@Service`, not a domain use case. No `SecurityConfig` change: `/api/v1/dashboard/**` falls under the existing admin chain (`/api/**` → authenticated, Basic).

## Interfaces / Contracts

Response JSON (status maps include all enum keys, zero-filled):

```json
{
  "funcionarios": { "total": 120, "byStatus": { "ACTIVO": 100, "INACTIVO": 20 } },
  "activeVoters": 95,
  "elections": { "total": 8, "byStatus": {
      "PROGRAMADA": 2, "ACTIVA": 1, "FINALIZADA": 4, "CANCELADA": 1, "SUSPENDIDA": 0 } }
}
```

Port additions return primitive / `Map<String,Long>` domain types only — never JPA projections (hexagonal rule). `ElectionStatus` keys: PROGRAMADA, ACTIVA, FINALIZADA, CANCELADA, SUSPENDIDA.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|--------------|----------|
| Application | Service composes both ports; zero-fills missing status keys; totals = sum | `*Test` JUnit5 + Mockito, mock the two ports |
| Adapter (out) | Aggregate JPQL grouped counts; eligible predicate exact (`ACTIVO` & `puedeVotar=true`) | `*IT` Testcontainers PostgreSQL, seeded fixtures |
| Adapter (in) | `GET /metrics` 200 + JSON shape; 401 unauthenticated | `@WebMvcTest` + `@MockitoBean` service |

## Migration / Rollout

No migration. Additive read-only endpoint; existing paginated APIs (`/api/v1/elections`, `/api/v1/funcionarios`) untouched. Rollback = delete the 3 new files and the added port methods.

## Frontend Integration Boundary

Backend repo (`electoral-votapp`) is the SOLE implementation target for this change. The companion frontend SPA (`Electoral-Votapp-Frontend`, locally available) will consume this endpoint to render home-panel funcionario cards (totals, by status, active voters) mirroring the existing election cards — but that is a SEPARATE frontend PR, OUT OF SCOPE here. Contract owned by this change: `GET /api/v1/dashboard/metrics` + JSON shape above (admin Basic-auth; CORS already permits the SPA origin via `SecurityConfig`).

## Open Questions

- [ ] `funcionarios.total`: count ALL rows or only non-deleted? No soft-delete column exists today → counts all rows. Confirm acceptable with product.
