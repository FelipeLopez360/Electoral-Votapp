# Proposal: Live Tracking Dashboard Widget

## Intent

Provide the admin dashboard with real-time participation progress for active elections without burdening the database with heavy global metrics aggregations.

## Scope

### In Scope
- Create a dedicated `GET /api/v1/dashboard/live-tracking` endpoint.
- Retrieve all elections currently in `ACTIVA` status.
- Count cast votes (participation) per active election.
- Calculate the total eligible voters per active election, implementing the strict fallback logic (Census vs. Global Active Voters).
- Return a lightweight DTO tailored for frequent polling.

### Out of Scope
- Frontend UI changes or integration (will be handled in a separate frontend PR).
- Changes to the existing `GET /api/v1/dashboard/metrics` endpoint.
- WebSockets or Server-Sent Events (SSE) for push updates; the endpoint will support polling instead.

## Capabilities

### New Capabilities
- `dashboard-live-tracking`: Retrieve real-time participation progress for active elections via a dedicated polling endpoint.

### Modified Capabilities
- None

## Approach

Adopt **Approach 2** from the exploration. A dedicated endpoint (`GET /api/v1/dashboard/live-tracking`) will be exposed. A new service method will query the election repository for `ACTIVA` elections, fetch total cast votes from `ParticipacionRepositoryPort`, and calculate total eligible voters. 

**Core Rule - Eligibility Fallback Logic**: 
Total eligible voters per election MUST be calculated as:
1. Check custom census (`censoRepository.countByEleccionId(id)`).
2. If count > 0, return the census count.
3. If count == 0, fall back to global active voters (`count(estado_laboral='ACTIVO' AND puede_votar=true)`).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `co.com.votapp.ws.voting.infrastructure.adapter.out.persistence.ParticipacionJpaRepository` | Modified | Add `countByEleccionId(UUID)` method |
| `co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort` | Modified | Expose count method |
| `co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort` | Modified | Expose `findByStatus` |
| `co.com.votapp.ws.electoral.application.service.DashboardMetricsService` | Modified | Add logic to orchestrate live-tracking data |
| `co.com.votapp.ws.electoral.infrastructure.adapter.in.web.DashboardController` | Modified | Add `GET /api/v1/dashboard/live-tracking` endpoint |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Eligibility fallback omission | High | Explicitly document fallback rule and enforce it via integration tests. |
| Inefficient polling load | Low | Keep the query lightweight, return only minimal data for active elections. |
| Cross-context leakage | Low | Wire `ParticipacionRepositoryPort` properly without violating module bounds. |

## Rollback Plan

Remove the `GET /api/v1/dashboard/live-tracking` mapping from `DashboardController` and revert the new methods in repositories/services. The database schema is unaffected.

## Dependencies

- None (Independent backend change).

## Success Criteria

- [ ] `GET /api/v1/dashboard/live-tracking` returns a 200 OK with the list of active elections.
- [ ] Response includes total cast votes and accurate total eligible voters (respecting the census vs. global fallback).
- [ ] No regression or performance impact on the existing `GET /api/v1/dashboard/metrics` endpoint.