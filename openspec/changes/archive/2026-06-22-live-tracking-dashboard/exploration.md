## Exploration: Live Tracking Dashboard Widget

### Current State
Currently, the admin dashboard fetches aggregate metrics via `GET /api/v1/dashboard/metrics`, which relies on `DashboardMetricsService`. It returns total elections by status and total funcionarios by labor status, plus the global `activeVoters` count. 

Voter eligibility per election uses `CensoEntity`. If the census is empty, the election falls back to the global `activeVoters` count. Participation is recorded in `ParticipacionEntity` (voting context). Currently, there is no logic to fetch the total cast votes per election in the existing dashboard APIs.

### Affected Areas
- `co.com.votapp.ws.voting.infrastructure.adapter.out.persistence.ParticipacionJpaRepository` — Needs a `long countByEleccionId(UUID)` method.
- `co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort` (and Adapter) — Needs the count exposed.
- `co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort` (and Adapter) — Needs a `findByStatus` method to easily fetch `ACTIVA` elections (existing methods are tailored for the scheduler).
- `co.com.votapp.ws.electoral.application.service.DashboardMetricsService` (or a new Service) — Needs to orchestrate these calls.
- `co.com.votapp.ws.electoral.infrastructure.adapter.in.web.DashboardController` — Needs a new endpoint `GET /api/v1/dashboard/live-tracking`.

### Approaches
1. **Extend `/api/v1/dashboard/metrics`** — Add a list of active elections with their progress to the existing metrics response.
   - Pros: Single request for the whole dashboard; fast implementation.
   - Cons: Progress bars encourage frequent polling. Polling this endpoint would unnecessarily trigger heavy global aggregations (`countByEstadoLaboral`, `countByStatus`).
   - Effort: Low

2. **Dedicated `/api/v1/dashboard/live-tracking`** — Create a new endpoint and DTO specifically for active elections and their progress.
   - Pros: Highly efficient polling. Clear separation of static totals from real-time data. Avoids heavy DB queries.
   - Cons: Slight boilerplate (new DTO, new endpoint).
   - Effort: Low/Medium

### Recommendation
Approach 2 (Dedicated endpoint) is strongly recommended. Polling a dedicated lightweight endpoint ensures the database isn't burdened with unnecessary global queries when the admin only needs to see the participation progress bar update.

### Risks
- **Eligibility Fallback Logic:** The "total eligible" calculation must exactly replicate the rule in `VoterEligibilityRepositoryAdapter`: `long eligible = censoRepository.countByEleccionId(id); return eligible > 0 ? eligible : globalActiveVoters;`. Missing this fallback will break percentages for elections without a custom census.
- **Cross-context Access:** The `electoral` application layer will inject `ParticipacionRepositoryPort` from the `voting` context. This is allowed in our hexagonal orchestration pattern but needs to be wired properly.

### Ready for Proposal
Yes. Inform the user and the orchestrator to proceed with the dedicated endpoint design.