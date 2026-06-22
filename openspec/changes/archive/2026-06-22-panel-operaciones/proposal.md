# Proposal: Admin Operations Dashboard Metrics

> **Canonical change identity**: `panel-operaciones considero que debemos tener en el inicio al igual que elecciones total, programadas, activas y finalizadas, pero para funcionarios, asi sabremos cuantos funcionarios tenemos, su estado, votantes activos, etc [Image 1]`
> **Stale artifact warning**: `openspec/changes/admin-dashboard-metrics/proposal.md` is a SUPERSEDED orphan slug. Do NOT follow it. The canonical lineage is this folder.

## Intent

To support the admin operations home panel by providing a single aggregate summary endpoint (`GET /api/v1/dashboard/metrics`) that returns total/active funcionarios, active voters, and election statuses, ensuring we don't abuse paginated APIs for UI cards.

## Scope

### In Scope
- Add a new dashboard summary endpoint `GET /api/v1/dashboard/metrics` returning funcionario counts by status, election counts by status, and globally eligible active voters.
- Add count-oriented read contracts to `FuncionarioRepositoryPort` and `ElectionRepositoryPort`.
- Implement aggregate JPQL `GROUP BY` queries in the respective JPA repositories.
- Place the application service and web adapter strictly within the existing **electoral** bounded context.

### Out of Scope
- Creating a new top-level `dashboard` bounded context (must use existing `electoral` context).
- Modifying existing paginated list endpoints (`/api/v1/funcionarios` or `/api/v1/elections`).
- Frontend UI implementation (the `Electoral-Votapp-Frontend` repo is a companion consumer, but frontend implementation is a completely separate PR).

## Capabilities

> This section is the CONTRACT between proposal and specs phases.
> The sdd-spec agent reads this to know exactly which spec files to create or update.

### New Capabilities
- `admin-dashboard-metrics`: Read-only aggregate endpoint providing counts for funcionarios (by state), elections (by state), and eligible active voters.

### Modified Capabilities
- None

## Approach

Create a single read-only aggregate endpoint `GET /api/v1/dashboard/metrics` that composes counts from the Electoral and Auth/Funcionario data already persisted. We will add count-oriented output ports, orchestrated by a thin `@Service` placed strictly inside the existing **electoral** bounded context (`electoral/application/service/`). "Votantes activos" will be defined explicitly as globally eligible funcionarios (`estado_laboral = 'ACTIVO'` AND `puede_votar = true`).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `co.com.votapp.ws.electoral.infrastructure.adapter.in.web` | New | `DashboardController` (`GET /api/v1/dashboard/metrics`) |
| `co.com.votapp.ws.electoral.application.service` | New | `DashboardMetricsService` to compose the read-only output ports |
| `co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort` | Modified | Add count methods by labor status and eligibility |
| `co.com.votapp.ws.auth...FuncionarioJpaRepository` | Modified | Add aggregate JPQL/native SQL |
| `co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort` | Modified | Add count methods by `ElectionStatus` |
| `co.com.votapp.ws.electoral...EleccionJpaRepository` | Modified | Add aggregate grouped counts |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Ambiguity of "votantes activos" | Medium | Explicitly define it as globally eligible funcionarios (Active + puede_votar) and document this clearly for frontend use. |
| Context proliferation | Low | Enforce that this goes into the existing `electoral` bounded context, avoiding a new `dashboard` root package that violates the architecture. |
| Frontend dependency | Low | Contract is clear (`GET /api/v1/dashboard/metrics`). Frontend implementation is deferred to a companion PR. |

## Rollback Plan

Revert the commit adding the dashboard endpoint and repository count methods. The existing paginated APIs are untouched, so rolling back is a safe deletion of new files and methods.

## Dependencies

- None for backend implementation. The companion frontend repository will consume this endpoint in a separate PR.

## Success Criteria

- [ ] `GET /api/v1/dashboard/metrics` returns accurate counts of funcionarios by state.
- [ ] Endpoint returns accurate counts of elections by state.
- [ ] Endpoint returns accurate count of active eligible voters (`estado_laboral = 'ACTIVO'` AND `puede_votar = true`).
- [ ] Implementation is contained within the `electoral` context without creating a new `dashboard` root package.
