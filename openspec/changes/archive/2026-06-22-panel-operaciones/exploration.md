## Exploration: panel-operaciones considero que debemos tener en el inicio al igual que elecciones total, programadas, activas y finalizadas, pero para funcionarios, asi sabremos cuantos funcionarios tenemos, su estado, votantes activos, etc [Image 1]

### Current State
- The backend already exposes admin list/detail CRUD for funcionarios at `/api/v1/funcionarios` via `FuncionarioController`, but it is paginated/search-oriented only; there is no aggregate summary endpoint or count contract in `FuncionarioRepositoryPort`.
- The backend already exposes paginated elections at `/api/v1/elections` via `ElectionController`, but `ElectionRepositoryPort` and `EleccionJpaRepository` only support list/search and scheduler date queries, not status totals.
- The data model already contains the fields needed for dashboard cards: `funcionarios.estado_laboral`, `funcionarios.puede_votar`, `elecciones.estado`, `tokens_votacion.status`, and `participacion_electoral.funcionario_id` in `V1__Initial_schema.sql`.
- Current code has one clear eligibility definition: `VoterEligibilityRepositoryAdapter` treats a funcionario as globally eligible only when `estado_laboral = ACTIVO` and `puede_votar = true`. That is the closest existing meaning for “votantes activos”, but it is not yet an explicit dashboard metric.
- There is no frontend app in this workspace. Existing OpenSpec artifacts already document this repo as backend-only, so rendering cards on the admin home screen requires a companion frontend change in the separate SPA repo.

### Affected Areas
- `src/main/java/co/com/votapp/ws/auth/infrastructure/adapter/in/web/FuncionarioController.java` — existing admin read surface; natural integration point if funcionario metrics stay under auth-facing admin APIs.
- `src/main/java/co/com/votapp/ws/auth/domain/port/out/FuncionarioRepositoryPort.java` — missing count-oriented read methods such as totals by labor status and globally eligible funcionarios.
- `src/main/java/co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioJpaRepository.java` — currently only search/lookup and eligibility-list queries; missing aggregate JPQL/native SQL.
- `src/main/java/co/com/votapp/ws/electoral/domain/port/out/ElectionRepositoryPort.java` — missing count methods by `ElectionStatus`.
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/EleccionJpaRepository.java` — currently supports search and scheduler lookups only; would need grouped counts or derived count queries.
- `src/main/java/co/com/votapp/ws/voting/domain/port/out/ParticipacionRepositoryPort.java` and `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/out/persistence/ParticipacionRepositoryAdapter.java` — no aggregate participation counts for dashboard use.
- `src/main/java/co/com/votapp/ws/votereligibility/infrastructure/adapter/out/persistence/VoterEligibilityRepositoryAdapter.java` — existing source of truth for “globally eligible” semantics that the proposal should either reuse or rename explicitly.
- `src/test/java/co/com/votapp/ws/auth/infrastructure/adapter/in/web/FuncionarioControllerWebMvcTest.java` and neighboring auth repository tests — existing TDD pattern for adding admin read endpoints.
- `src/test/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/ElectionControllerPaginationTest.java` and neighboring electoral repository tests — existing TDD pattern for election read contracts.

### Approaches
1. **Dedicated admin dashboard summary endpoint** — add one read-only endpoint returning all homepage cards in a single payload (for example: funcionario totals by status plus election totals by status).
   - Pros: one frontend call, explicit contract for the homepage, avoids abusing paginated list endpoints, easiest path to add future cards.
   - Cons: crosses auth/electoral/voting read concerns, so the proposal must define ownership carefully and clarify the exact meaning of “votantes activos”.
   - Effort: Medium

2. **Multiple per-context count endpoints** — expose separate summary/count endpoints from existing modules (`funcionarios`, `elections`, maybe `voting`) and let the frontend compose the dashboard.
   - Pros: keeps ownership closer to existing contexts, smaller backend slices per endpoint.
   - Cons: more frontend orchestration, more network calls, and “votantes activos” still needs a cross-context/product definition.
   - Effort: Medium

### Recommendation
Choose **Approach 1**. The request is specifically about the admin home panel, so a single dashboard-focused read contract is the cleanest fit. Keep it read-only and aggregate-only: do not change existing paginated endpoints just to serve cards. In implementation terms, the safest backend shape is a thin admin dashboard controller backed by count-oriented repository queries, reusing the existing global eligibility rule (`ACTIVO && puede_votar=true`) unless product clarifies a different meaning for “votantes activos”.

### Risks
- “Votantes activos” is ambiguous today: it could mean globally eligible funcionarios, funcionarios assigned to at least one active election, or funcionarios who still hold ISSUED tokens for active elections.
- Cross-context read ownership is the main design risk because the requested cards combine auth data (funcionarios), electoral data (election statuses), and potentially voting data (participation/tokens).
- Frontend coordination is required: this backend repo can expose the metrics, but the homepage cards themselves live in the separate frontend repo.
- If the team tries to reuse paginated list endpoints for totals, the UI will either become inefficient or incorrect because the current contracts are not summary-oriented.

### Ready for Proposal
Yes — but the orchestrator should tell the user that the proposal must lock one product definition for “votantes activos” and include a companion frontend PR, because this repository alone can provide the API but not the admin home panel UI.
