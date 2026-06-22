# Apply Progress — live-tracking-dashboard

**Mode**: Strict TDD (RED → GREEN → REFACTOR)
**Date**: 2026-06-22
**Result**: ALL TASKS COMPLETE — 404 unit tests + 109 integration tests PASS

---

## TDD Cycle Evidence

| Task | RED | GREEN | REFACTOR | Notes |
|------|-----|-------|----------|-------|
| 1.1 | ✅ `ParticipacionRepositoryAdapterTest` — compile error (method not found) | — | — | RED confirmed: `symbol: method countByEleccionId(UUID)` |
| 1.2 | — | ✅ 3/3 tests pass | — | Added to port, JPA repo (`countByEleccionId`), adapter |
| 1.3 | ✅ `ElectionRepositoryAdapterFindByStatusTest` — compile error | — | — | RED confirmed: `symbol: method findByEstado(String)` |
| 1.4 | — | ✅ 4/4 tests pass | — | Added `findByStatus` to port, `findByEstado` to JPA, mapping in adapter |
| 2.1 | — | ✅ Record created | — | `LiveTrackingResponse` + nested `LiveTrackingItem` in `electoral/application/dto/` |
| 2.2–2.5 | ✅ `DashboardLiveTrackingServiceTest` — compile error (class not found) | — | — | 5 scenarios written before service existed |
| 2.6 | — | ✅ 5/5 service tests pass | — | `DashboardLiveTrackingService` with census-first fallback |
| 3.1 | ✅ `DashboardLiveTrackingControllerWebMvcTest` — 2/3 tests fail (404) | — | — | RED confirmed: endpoint missing |
| 3.2 | — | ✅ 3/3 WebMvc tests pass | — | Added `GET /live-tracking` to `DashboardController` |
| 3.3 | — | ✅ 5/5 IT tests pass | — | `DashboardLiveTrackingIT` with real Postgres |

---

## Files Changed

| File | Action | Description |
|------|--------|-------------|
| `src/main/java/co/com/votapp/ws/voting/domain/port/out/ParticipacionRepositoryPort.java` | Modified | Added `countByEleccionId(UUID)` |
| `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/out/persistence/ParticipacionJpaRepository.java` | Modified | Added derived `countByEleccionId` |
| `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/out/persistence/ParticipacionRepositoryAdapter.java` | Modified | Implemented `countByEleccionId` |
| `src/main/java/co/com/votapp/ws/electoral/domain/port/out/ElectionRepositoryPort.java` | Modified | Added `findByStatus(ElectionStatus)` |
| `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/EleccionJpaRepository.java` | Modified | Added `findByEstado(String)` |
| `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/ElectionRepositoryAdapter.java` | Modified | Implemented `findByStatus` |
| `src/main/java/co/com/votapp/ws/electoral/application/dto/LiveTrackingResponse.java` | Created | DTO with nested `LiveTrackingItem` |
| `src/main/java/co/com/votapp/ws/electoral/application/service/DashboardLiveTrackingService.java` | Created | Orchestrates 4 ports with census-first fallback |
| `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/DashboardController.java` | Modified | Added `GET /live-tracking` + injected `DashboardLiveTrackingService` |
| `src/test/java/co/com/votapp/ws/voting/infrastructure/adapter/out/persistence/ParticipacionRepositoryAdapterTest.java` | Created | 3 unit tests (TDD RED) |
| `src/test/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/ElectionRepositoryAdapterFindByStatusTest.java` | Created | 4 unit tests (TDD RED) |
| `src/test/java/co/com/votapp/ws/electoral/application/service/DashboardLiveTrackingServiceTest.java` | Created | 5 unit tests (TDD RED) |
| `src/test/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/DashboardLiveTrackingControllerWebMvcTest.java` | Created | 3 WebMvc tests (TDD RED) |
| `src/test/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/DashboardLiveTrackingIT.java` | Created | 5 integration tests with real Postgres |
| `src/test/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/DashboardControllerWebMvcTest.java` | Modified | Added `@MockitoBean DashboardLiveTrackingService` (regression fix) |
| `openspec/changes/live-tracking-dashboard/tasks.md` | Modified | All tasks marked [x] |

---

## Deviations from Design

None — implementation matches design.md exactly.

## Issues Found During Implementation

1. **JSONPath filter behavior in MockMvc**: `[?(@.xxx == 'val')]` filter expressions return a `List` not a scalar. The `.value(List.of(...))` assertion works but fails because JSON integers deserialize as `Integer` not `Long`. Fixed by using `andReturn()` + manual Jackson parsing for reliable assertions in integration tests.
2. **DashboardControllerWebMvcTest regression**: Existing `@WebMvcTest` test failed after adding `DashboardLiveTrackingService` to `DashboardController`'s constructor (unsatisfied dependency). Fixed by adding `@MockitoBean DashboardLiveTrackingService` to the existing test class.
3. **Stale bytecode in failsafe**: Failsafe picked up stale `.class` files. Fixed with `mvn clean test-compile` before running integration tests.

## Test Results

- **Unit tests** (surefire): 404 tests, 0 failures, 0 errors
- **Integration tests** (failsafe): 109 tests, 0 failures, 0 errors
- **Overall**: BUILD SUCCESS

## Status

13/13 tasks complete. **Ready for verify.**
