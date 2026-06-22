# Tasks: Admin Operations Dashboard Metrics

## Review Workload Forecast

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

| Field | Value |
|-------|-------|
| Estimated changed lines | ~350–430 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR (backend only) |
| Delivery strategy | auto-forecast |

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Backend dashboard endpoint | PR 1 (sole) | Additive, no migration, frontend is separate repo |

## Phase 1: Persistence Layer — Ports + Queries (TDD)

- [x] 1.1 RED: Write `ElectionRepositoryAdapterIT` test method for `countByStatus()` with seeded `elecciones` (GROUP BY all 5 `ElectionStatus` values + empty DB zero-case)
- [x] 1.2 GREEN: Add `Map<String,Long> countByStatus()` to `ElectionRepositoryPort`, `EleccionJpaRepository` (`@Query GROUP BY estado`), and `ElectionRepositoryAdapter`
- [x] 1.3 RED: Write `FuncionarioRepositoryAdapterCountsIT` test methods for `countByEstadoLaboral()` (GROUP BY distinct labor statuses) and `countActiveEligibleVoters()` (exact predicate `ACTIVO + puede_votar=true`)
- [x] 1.4 GREEN: Add both count methods to `FuncionarioRepositoryPort`, `FuncionarioJpaRepository` (2 `@Query` methods), and `FuncionarioRepositoryAdapter`

## Phase 2: Application Service (TDD)

- [x] 2.1 RED: Write `DashboardMetricsServiceTest` — mock both output ports; verify service zero-fills all status keys (`ACTIVO`, `INACTIVO` for funcionarios; all 5 `ElectionStatus` enum values); verify totals = sum of map values
- [x] 2.2 GREEN: Create `DashboardMetricsResponse` record with nested `FuncionarioBlock`, `ElectionBlock`, and `activeVoters` fields
- [x] 2.3 GREEN: Create `DashboardMetricsService` (`@Service` in `electoral/application/service/`) that composes 3 port calls, maps results with zero-fill, returns `DashboardMetricsResponse`

## Phase 3: Web Adapter (TDD)

- [x] 3.1 RED: Write `DashboardControllerWebMvcTest` — `@WebMvcTest` + `@MockitoBean` service; verify 200 + JSON shape, verify 401 without auth
- [x] 3.2 GREEN: Create `DashboardController` (`@RestController` in `electoral/infrastructure/adapter/in/web/`) with `GET /api/v1/dashboard/metrics` delegating to `DashboardMetricsService`

## Phase 4: Verification

- [x] 4.1 Run `./mvnw verify` — all unit + integration tests pass (389 unit + 104 IT = 493 total)
- [x] 4.2 Verify hexagonal compliance: no `org.springframework` or `jakarta.persistence` imports in `domain/` packages
