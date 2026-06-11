# Proposal: election-auto-scheduler

## Intent
Elections are created with `fechaInicio` and `fechaFin` dates, but their lifecycle transitions (`PROGRAMADA → ACTIVA → FINALIZADA`) are entirely manual. Administrators must explicitly trigger these state changes via REST endpoints, meaning the stored dates are never evaluated automatically. This proposal introduces an automated scheduler to poll for due elections and transition their states, reducing manual operational overhead and ensuring timely election events.

## Scope

### In Scope
- Adding Spring `@EnableScheduling` to the application.
- Creating date-based and status-based queries in the persistence ports (`ElectionRepositoryPort`) and JPA repositories.
- Fixing the `createdAt` reset bug in `ElectionRepositoryAdapter.save()`.
- Implementing an application-layer transactional service to handle the transitions securely.
- Creating a scheduled infrastructure poller (`@Scheduled`) to activate and finalize elections individually (batch processed but isolated failures).

### Out of Scope
- Scheduling mechanisms outside of Spring `@Scheduled` (e.g., Quartz, external cron).
- Refactoring existing pure domain use cases (`ActivateElectionUseCaseImpl`, `FinalizeElectionUseCaseImpl`) to have transactional bounds; we will wrap them instead.
- UI/Frontend changes.

## Capabilities

### New Capabilities
- `election-auto-scheduler`: Automatically activates scheduled elections and finalizes active elections based on their configured start and end dates.

### Modified Capabilities
- None.

## Approach
We will utilize Spring's built-in `@Scheduled` annotation to run a lightweight poller (Approach 1 from exploration). A new infrastructure component (`ElectionScheduler`) will run every 60 seconds to query for elections that are due for state transition. Since the pure domain use cases (`ActivateElectionUseCaseImpl`, `FinalizeElectionUseCaseImpl`) lack transactional boundaries and perform multiple writes, we will wrap them in a new application-layer `@Service` annotated with `@Transactional`. This ensures atomicity. The scheduler will process each due election individually within a try-catch block, ensuring that one failing transition doesn't abort the whole batch.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ElectoralVotappApplication` | Modified | Add `@EnableScheduling` annotation |
| `ElectionRepositoryPort` | Modified | Add `findByStatusAndFecha...Before` methods |
| `EleccionJpaRepository` | Modified | Add Spring Data derived query methods |
| `ElectionRepositoryAdapter` | Modified | Fix `createdAt` being reset on updates |
| `ElectionTransitionAppService` | New | Application service with `@Transactional` wrapping use cases |
| `ElectionScheduler` | New | Infrastructure component with `@Scheduled` polling logic |
| `DomainConfig` | Modified | Wire new application service |

## Key Decisions
- **Decision 1:** Spring `@Scheduled` over external job scheduler. Reasoning: Keeps MVP architecture simple without external dependencies like Quartz or Temporal.
- **Decision 2:** Application-layer `@Transactional` wrapper. Reasoning: The domain use cases must remain pure and agnostic to Spring's `@Transactional`. Wrapping them in an application service maintains hexagonal architecture rules while ensuring atomic writes.
- **Decision 3:** Individual try/catch per election in batch. Reasoning: Avoids poison pill scenarios where one invalid election prevents others from transitioning.

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Scheduler runs concurrently on multiple app instances | Low (MVP is single instance) | In the future, we would need ShedLock or Redis distributed locks. Acceptable risk for MVP. |
| Timezone inconsistencies causing premature transitions | Medium | Ensure JVM and DB timezones match (UTC), and use `LocalDateTime.now(ZoneOffset.UTC)`. |
| Memory exhaustion if too many elections are returned | Low | Poller will run frequently, keeping batch sizes small. |

## Rollback Plan
Remove the `@EnableScheduling` annotation and the `ElectionScheduler` component, and revert to manual activation/finalization via the existing API endpoints.

## Dependencies
- None.

## Success Criteria
- [ ] Elections in `PROGRAMADA` state automatically transition to `ACTIVA` when `fechaInicio` passes.
- [ ] Elections in `ACTIVA` state automatically transition to `FINALIZADA` when `fechaFin` passes.
- [ ] A blank vote candidate is correctly and atomically generated when an election is activated by the scheduler.
- [ ] Existing endpoints for manual activation/finalization continue to work if needed.
- [ ] Unit and integration tests verify the transactional integrity and scheduler queries.