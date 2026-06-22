# Proposal: Admin Dashboard Metrics

> **⚠️ SUPERSEDED — DO NOT IMPLEMENT FROM THIS FOLDER.**
> This `admin-dashboard-metrics/` slug is a stale, orphaned proposal-only artifact (no spec, design, or tasks). It was an early naming attempt that drifted from the canonical change.
> **Canonical change folder**: `openspec/changes/panel-operaciones considero que debemos tener en el inicio al igual que elecciones total, programadas, activas y finalizadas, pero para funcionarios, asi sabremos cuantos funcionarios tenemos, su estado, votantes activos, etc [Image 1]/` (contains proposal.md, specs/, design.md).
> **Canonical Engram topic root**: `sdd/panel-operaciones considero que debemos tener en el inicio al igual que elecciones total, programadas, activas y finalizadas, pero para funcionarios, asi sabremos cuantos funcionarios tenemos, su estado, votantes activos, etc [Image 1]/*`.
> Downstream phases (tasks, apply, verify) MUST use the canonical folder above, not this one.

## Intent
Add backend support for a unified admin dashboard panel showing aggregated metrics for both `funcionarios` (total, by status, active voters) and `elecciones` (total, scheduled, active, finished).

## Scope

### In Scope
- Single read-only endpoint (e.g., `/api/v1/admin/dashboard`) returning aggregated counts for the home panel.
- Count queries in `FuncionarioRepositoryPort` and `ElectionRepositoryPort` to efficiently aggregate data without loading lists.
- Using existing rule (`estado_laboral = ACTIVO` and `puede_votar = true`) as the default assumption for the "votantes activos" metric.

### Out of Scope
- Frontend UI implementation (handled in a separate companion PR for the SPA repo).
- Changes to existing paginated search endpoints (`/api/v1/funcionarios`, `/api/v1/elections`).
- New data collection; only aggregating existing persisted data.

## Capabilities

### New Capabilities
- `admin-dashboard`: Aggregated read-only metrics across business domains for the admin operations panel.

### Modified Capabilities
- `auth`: Exposing count-oriented methods in `FuncionarioRepositoryPort` and its JPA adapter.
- `electoral`: Exposing count-oriented methods in `ElectionRepositoryPort` and its JPA adapter.

## Approach
Implement **Approach 1** from the exploration. Add a dedicated read-only controller for the admin dashboard that aggregates data from multiple domains. To respect Hexagonal bounds, this will be implemented as an application-level orchestration service (or a new lightweight context) that coordinates read-only calls to `FuncionarioRepositoryPort` and `ElectionRepositoryPort`. Database layers will use JPQL/native `COUNT` queries to ensure high performance without loading entity rows.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `adapter/in/web/DashboardController` | New | Single read contract for the homepage metrics. |
| `domain/port/out/FuncionarioRepositoryPort` | Modified | Add metric count methods by status/eligibility. |
| `domain/port/out/ElectionRepositoryPort` | Modified | Add metric count methods by election state. |
| `adapter/out/persistence/*JpaRepository` | Modified | Add `COUNT` / `GROUP BY` JPQL queries. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Ambiguous "votantes activos" definition | High | Using existing eligibility rule (`ACTIVO` & `puede_votar=true`). Kept as an explicit assumption for product review. |
| Cross-context ownership | Med | Implement the dashboard as an application orchestration layer to avoid cyclical domain dependencies. |
| Missing Frontend | High | Backend only provides API; frontend PR must be scheduled concurrently. |

## Rollback Plan
Revert the PR. Since the feature only adds a new read-only endpoint and underlying count queries, rollback has zero impact on existing voting or admin list flows.

## Dependencies
- Frontend repository must implement the dashboard UI consuming the new endpoint.
- Product owner validation on the definition of "votantes activos".

## Success Criteria
- [ ] A single endpoint returns correct totals for all election states and funcionario states.
- [ ] Endpoint responds efficiently using count/aggregate SQL, without fetching full table rows.
- [ ] Existing `GET /api/v1/funcionarios` and `GET /api/v1/elections` remain cleanly separated and unmodified.