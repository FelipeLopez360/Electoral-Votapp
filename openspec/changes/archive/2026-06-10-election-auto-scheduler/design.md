# Design: Election Auto Scheduler

## Technical Approach

A Spring `@Scheduled` poller in `adapter/in/scheduler/` runs every 60s, queries due elections via new derived-query methods, and delegates state transitions to a `@Transactional` application service. The service wraps the EXISTING pure domain use cases (`ActivateElectionUseCase`, `FinalizeElectionUseCase`) — no domain validation is duplicated, and the domain stays Spring-free. Each election is processed in its own try/catch so one failure never aborts the batch. Implements spec requirements Auto-Activate, Auto-Finalize, Fault Isolation, Atomicity, Pure Domain, Timezone Consistency.

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|----------|--------|----------------------|-----------|
| Transaction boundary | `@Transactional` app service wrapping pure use cases | `@Transactional` in domain use case | Domain MUST stay Spring-free (AGENTS.md law). Mirrors existing `CastVoteAppService` pattern. |
| Atomicity granularity | One transaction PER election (app-service method called per-id from loop) | One transaction for whole batch | Fault isolation: a rollback affects only the failing election; loop continues. |
| Scheduler placement | `adapter/in/scheduler/` `@Component` | App-service `@Scheduled`, domain | Scheduler is a driving adapter (input side). Keeps app service transport-agnostic and unit-testable. |
| Time source | `LocalDateTime.now(ZoneOffset.UTC)` in scheduler | `LocalDateTime.now()` (system TZ) | Election dates persist as UTC `Instant`; query must compare in UTC. Adapter already maps via `ZoneOffset.UTC`. |
| Wiring of app service | `@Service` (component-scanned) | Manual bean in `DomainConfig` | App services use Spring annotations (allowed in `application/`). `DomainConfig` only wires pure use cases; no edit needed there. |
| `createdAt` fix | Read existing entity on update; preserve its `createdAt` | Rely on JPA `updatable=false` quirk | Quirk silently masks the bug but in-memory record is wrong. Explicit preservation is correct and test-provable. |

## Data Flow

    @Scheduled(60s)
    ElectionScheduler ──findByStatusAndFechaInicioBefore(PROGRAMADA, nowUtc)──► ElectionRepositoryPort
         │                                                                          │
         │  for each due election (try/catch):                                      ▼
         └──► ElectionTransitionAppService.activate(id) ──► ActivateElectionUseCase ──► save + blank candidate
                       [@Transactional]                          (pure domain)
         (same flow for ACTIVA → FINALIZADA via finalize)

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `electoral/domain/port/out/ElectionRepositoryPort.java` | Modify | Add `List<Election> findByStatusAndFechaInicioBefore(ElectionStatus, LocalDateTime)` and `...FechaFinBefore(...)`. |
| `.../adapter/out/persistence/EleccionJpaRepository.java` | Modify | Add derived queries on `estado` (String) + `fechaInicio`/`fechaFin` (Instant): `findByEstadoAndFechaInicioBefore`, `findByEstadoAndFechaFinBefore`. |
| `.../adapter/out/persistence/ElectionRepositoryAdapter.java` | Modify | Implement two new port methods (map status→`name()`, `LocalDateTime`→`Instant` UTC, entity→domain). Fix `createdAt`: on update, load existing entity and preserve its `createdAt`; only set on INSERT. |
| `electoral/application/service/ElectionTransitionAppService.java` | Create | `@Service @Transactional`; methods `activate(UUID)` / `finalize(UUID)` delegating to the two use-case input ports. No business logic. |
| `electoral/infrastructure/adapter/in/scheduler/ElectionScheduler.java` | Create | `@Component @Slf4j`; `@Scheduled(fixedRate=60000)` queries due PROGRAMADA/ACTIVA, loops with per-election try/catch, calls app service, logs errors. |
| `ElectoralVotappApplication.java` | Modify | Add `@EnableScheduling`. |

## Interfaces / Contracts

```java
// domain/port/out/ElectionRepositoryPort.java (additions — pure, domain types only)
List<Election> findByStatusAndFechaInicioBefore(ElectionStatus status, LocalDateTime now);
List<Election> findByStatusAndFechaFinBefore(ElectionStatus status, LocalDateTime now);

// adapter/out/persistence/EleccionJpaRepository.java (additions — entity uses String estado + Instant)
List<EleccionEntity> findByEstadoAndFechaInicioBefore(String estado, Instant now);
List<EleccionEntity> findByEstadoAndFechaFinBefore(String estado, Instant now);

// application/service/ElectionTransitionAppService.java
@Service
public class ElectionTransitionAppService {
    // ctor injects ActivateElectionUseCase + FinalizeElectionUseCase
    @Transactional public void activate(UUID id) { activateUseCase.activate(id); }
    @Transactional public void finalize(UUID id) { finalizeUseCase.finalize(id); }
}
```

`createdAt` fix in adapter `save()`: when `election.id() != null`, fetch existing entity and copy its `createdAt` into the entity being saved instead of `Instant.now()`. `updatedAt` always `Instant.now()`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|--------------|----------|
| Unit (`*Test`) | `ElectionScheduler` loop: due elections trigger app-service calls; one throwing election does not stop the rest; errors logged | Mockito; mock `ElectionRepositoryPort` + `ElectionTransitionAppService` |
| Unit (`*Test`) | `ElectionTransitionAppService` delegates to correct use-case port | Mockito; verify interactions only |
| Integration (`*IT`) | New derived queries return only due rows; UTC boundary correct; `createdAt` preserved on update, `updatedAt` changes | `@SpringBootTest` + Testcontainers (PostgreSQL) |
| Integration (`*IT`) | End-to-end: PROGRAMADA with past `fechaInicio` → scheduler tick → ACTIVA + blank candidate created atomically | Testcontainers; drive scheduler method directly |

## Migration / Rollout

No data migration. Rollback = remove `@EnableScheduling` + scheduler class; manual activate/finalize endpoints stay functional (Manual Override Preservation requirement).

## Open Questions

- [ ] None blocking. Multi-instance concurrency (duplicate ticks) is explicitly accepted MVP risk per proposal; ShedLock/Redis lock deferred.
