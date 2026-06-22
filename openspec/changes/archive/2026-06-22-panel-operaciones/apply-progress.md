# Apply Progress: panel-operaciones

**Change**: panel-operaciones (Admin Operations Dashboard Metrics)
**Mode**: Strict TDD (RED → GREEN → REFACTOR)
**Date**: 2026-06-22
**Artifact store**: hybrid (Engram + filesystem)

## Status

**4/4 phases complete — Ready for verify**

## TDD Cycle Evidence

| Task | Phase | RED (test written first) | GREEN (implementation passes) | REFACTOR |
|------|-------|--------------------------|-------------------------------|----------|
| 1.1 | Persistence | ✅ Added `countByStatus()` tests to `ElectionRepositoryAdapterIT` → compile error (method missing) | ✅ Added JPQL + adapter impl; 9 IT tests pass | No structural change needed |
| 1.2 | Persistence | (GREEN for 1.1 RED) | ✅ `ElectionRepositoryPort.countByStatus()`, `EleccionJpaRepository.countGroupByEstado()`, `ElectionRepositoryAdapter.countByStatus()` all implemented | — |
| 1.3 | Persistence | ✅ Created `FuncionarioRepositoryAdapterCountsIT` with 5 test methods → compile error (methods missing) | ✅ After 1.4 GREEN: 5 IT tests pass | Fix to `insertFuncionario` to avoid `numero_empleado` constraint collision (systematic debug) |
| 1.4 | Persistence | (GREEN for 1.3 RED) | ✅ `FuncionarioRepositoryPort.countByEstadoLaboral()` + `countActiveEligibleVoters()`, `FuncionarioJpaRepository` (2 JPQL), `FuncionarioRepositoryAdapter` (2 methods) | — |
| 2.1 | Application | ✅ Created `DashboardMetricsServiceTest` (7 test cases) → compile error (service + DTO missing) | ✅ After 2.2+2.3 GREEN: 7 unit tests pass | — |
| 2.2 | Application | (GREEN for 2.1 RED) | ✅ `DashboardMetricsResponse` record with `FuncionarioBlock`, `ElectionBlock` nested records | — |
| 2.3 | Application | (GREEN for 2.1 RED) | ✅ `DashboardMetricsService` — zero-fill logic, 3 port calls, total computation | — |
| 3.1 | Web | ✅ Created `DashboardControllerWebMvcTest` (2 test cases) → compile error (controller + service missing) | ✅ After 3.2 GREEN: 2 WebMvc tests pass | — |
| 3.2 | Web | (GREEN for 3.1 RED) | ✅ `DashboardController` — `GET /api/v1/dashboard/metrics`, delegates to service | — |
| 4.1 | Verify | — | ✅ `./mvnw verify` → 389 unit + 104 IT = **493 total, 0 failures** | — |
| 4.2 | Verify | — | ✅ `grep -r "import org.springframework"` in `domain/` → no matches | — |

## Files Changed

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/co/com/votapp/ws/electoral/domain/port/out/ElectionRepositoryPort.java` | Modified | Added `Map<String,Long> countByStatus()` |
| `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/EleccionJpaRepository.java` | Modified | Added `@Query countGroupByEstado()` JPQL |
| `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/ElectionRepositoryAdapter.java` | Modified | Implemented `countByStatus()` with zero-fill |
| `src/main/java/co/com/votapp/ws/auth/domain/port/out/FuncionarioRepositoryPort.java` | Modified | Added `countByEstadoLaboral()` + `countActiveEligibleVoters()` |
| `src/main/java/co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioJpaRepository.java` | Modified | Added 2 JPQL aggregate queries |
| `src/main/java/co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioRepositoryAdapter.java` | Modified | Implemented 2 count methods |
| `src/main/java/co/com/votapp/ws/electoral/application/dto/DashboardMetricsResponse.java` | Created | Record with nested FuncionarioBlock + ElectionBlock |
| `src/main/java/co/com/votapp/ws/electoral/application/service/DashboardMetricsService.java` | Created | @Service composing 3 port calls + zero-fill |
| `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/DashboardController.java` | Created | @RestController GET /api/v1/dashboard/metrics |
| `src/test/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/ElectionRepositoryAdapterIT.java` | Modified | Added countByStatus() test methods (2 new tests) |
| `src/test/java/co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioRepositoryAdapterCountsIT.java` | Created | 5 IT tests for countByEstadoLaboral + countActiveEligibleVoters |
| `src/test/java/co/com/votapp/ws/electoral/application/service/DashboardMetricsServiceTest.java` | Created | 7 unit tests (mocked ports, zero-fill verification) |
| `src/test/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/DashboardControllerWebMvcTest.java` | Created | 2 WebMvcTest tests (200 + JSON shape, 401 unauthenticated) |
| `openspec/changes/panel-operaciones/tasks.md` | Modified | All tasks marked [x] |

## Deviations from Design

None — implementation matches design.md exactly:
- Package boundaries: electoral context application/service + infrastructure/adapter/in/web ✅
- DashboardMetricsService is @Service (component-scanned, not in DomainConfig) ✅
- countByStatus() returns Map<String,Long> domain types only ✅
- All 5 ElectionStatus keys guaranteed in response ✅
- Security: /api/v1/dashboard/metrics falls under existing admin chain (Order 2) ✅
- No new schema migration ✅

## Bugs Found and Fixed

**FuncionarioRepositoryAdapterCountsIT — duplicate `numero_empleado` constraint**
- Root cause: `empNum` generation derived only 4 chars from nanosecond suffix — collided across the 5 `insertFuncionario()` calls within the same `@BeforeEach` (all sharing the same suffix value).
- Fix: derive `empNum` from the `documento` string itself (already unique per test run), truncated to 20 chars for the schema constraint. Removed `ON CONFLICT` on `numero_empleado` (not needed since the doc IS unique).

## Workload / PR Boundary

- Mode: single PR (within 800-line budget)
- Estimated changed lines: ~370 (production) + ~280 (tests) ≈ 650 total
- Budget: 800 lines — within range, single PR acceptable
- Delivery strategy: auto-forecast → single PR

## Next Recommended

`sdd-verify` — run structured verification against spec scenarios.
