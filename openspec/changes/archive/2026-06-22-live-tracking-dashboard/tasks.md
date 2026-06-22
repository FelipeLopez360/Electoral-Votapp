# Tasks: Live Tracking Dashboard Widget

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~380 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | auto-forecast |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

## Phase 1: Port Methods & Adapters

- [x] 1.1 (RED) Test: `ParticipacionRepositoryAdapter.countByEleccionId` delegates to JPA repo
- [x] 1.2 (GREEN) Add `countByEleccionId(UUID)` to port, JPA repo, and adapter
- [x] 1.3 (RED) Test: `ElectionRepositoryAdapter.findByStatus` maps entities → domain
- [x] 1.4 (GREEN) Add `findByStatus(ElectionStatus)` to port, JPA repo, and adapter

## Phase 2: DTO & Service

- [x] 2.1 Create `LiveTrackingResponse` and `LiveTrackingItem` in `electoral/application/dto/`
- [x] 2.2 (RED) Test: service uses census when census>0, never calls global voters
- [x] 2.3 (RED) Test: service falls back to global voters when census==0
- [x] 2.4 (RED) Test: service returns empty list when no active elections
- [x] 2.5 (RED) Test: service reports zero `totalCastVotes` when no participation
- [x] 2.6 (GREEN) Implement `DashboardLiveTrackingService` orchestrating 4 ports

## Phase 3: Controller & Integration

- [x] 3.1 (RED) WebMvc test: `GET /api/v1/dashboard/live-tracking` returns 200
- [x] 3.2 (GREEN) Add `GET /live-tracking` endpoint in `DashboardController`
- [x] 3.3 (IT) Integration test: end-to-end with real Postgres — census and global fallback
