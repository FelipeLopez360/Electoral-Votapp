## Exploration: censo-electoral

### Current State
- `V1__Initial_schema.sql` models global eligibility only. `funcionarios` carries `departamento_id -> departamentos.id`, `cargo_id -> cargos.id`, `estado_laboral`, and `puede_votar`; `elecciones` is independent; `tokens_votacion` and `participacion_electoral` link `eleccion_id` with `funcionario_id`, but there is no per-election census table.
- `VoterEligibilityRepositoryPort` exposes `boolean isEligible(Long funcionarioId)` only, and `VoterEligibilityRepositoryAdapter` implements it by reading `funcionarios` and checking `estado_laboral = 'ACTIVO' && puede_votar = true`.
- `IssueVotingTokenUseCaseImpl` calls that global eligibility check before issuing, then checks existing ISSUED token and prior participation. It does **not** verify per-election membership, and despite comments in `IssueVotingTokenCommand` / `TokenController`, it also does **not** verify that the election is `ACTIVA`.
- Admin election management exists in `ElectionController` (`/api/v1/elections`) for create/list/detail/update, candidate management, activate/finalize, and ballot retrieval. There is no census management endpoint.
- `Election` / `Eleccion` domain models only carry identity, code, name, status, and dates. `Funcionario` carries `departamentoId`, `cargoId`, `estadoLaboral`, and `puedeVotar`, but no election-scoped eligibility.
- `FuncionarioRepositoryPort` only supports `findAll(String search)`, `findById`, and `findByDocumentoIdentidad`. Its JPA query searches by nombres/apellidos/documento only; there are no repository queries for departamento / estado_laboral / puede_votar filters needed for bulk census assignment.

### Affected Areas
- `src/main/resources/db/migration/V1__Initial_schema.sql` — current schema has no election census structure.
- `src/main/java/co/com/votapp/ws/votereligibility/domain/port/out/VoterEligibilityRepositoryPort.java` — current contract is global, not election-scoped.
- `src/main/java/co/com/votapp/ws/votereligibility/infrastructure/adapter/out/persistence/VoterEligibilityRepositoryAdapter.java` — current implementation reuses `funcionarios` only.
- `src/main/java/co/com/votapp/ws/voting/domain/usecase/IssueVotingTokenUseCaseImpl.java` — must change because token issuance currently gates on global eligibility only.
- `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/in/web/TokenController.java` — token issuance API will need updated validation semantics and likely better error mapping.
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/ElectionController.java` — natural place for admin census endpoints under an election.
- `src/main/java/co/com/votapp/ws/auth/domain/port/out/FuncionarioRepositoryPort.java` — missing filter-oriented read contract for bulk selection.
- `src/main/java/co/com/votapp/ws/auth/infrastructure/adapter/out/persistence/FuncionarioJpaRepository.java` — missing filtered queries by departamento / estado_laboral / puede_votar.
- `src/main/java/co/com/votapp/ws/auth/infrastructure/adapter/in/web/FuncionarioController.java` — current GET only supports `search`, not structured filters.
- `src/main/java/co/com/votapp/ws/organization/infrastructure/adapter/in/web/OrganizationController.java` — already exposes active departments, useful for census filter UI.

### Approaches
1. **Option A: materialized `censo_electoral` table** — store explicit election-to-funcionario assignments and manage them through admin bulk actions.
   - Pros: simple read model for issuance (`exists eleccion_id + funcionario_id`), strongest auditability, easy manual add/remove, maps directly to the user story.
   - Cons: bulk actions need dedicated application logic; snapshot can become stale if a funcionario later changes department/status, so issuance should still apply the global `ACTIVO + puede_votar` guard.
   - Migration impact: add `censo_electoral(eleccion_id, funcionario_id, created_at, source)` with unique `(eleccion_id, funcionario_id)` and indexes; optional audit metadata if needed.
   - Domain purity: good — electoral owns census configuration; votereligibility consumes a narrow port.
   - Audit trail: good — each row is explicit and reviewable.
   - Effort: Medium.

2. **Option B: per-election eligibility rules** — persist filters such as departamentos / estado / puede_votar and evaluate eligibility dynamically.
   - Pros: most flexible, compact storage, easy to express “all SISTEMAS activos con puede_votar=true”.
   - Cons: hard to explain/audit the exact electorate at a given moment, results shift when funcionario data changes, manual exceptions are awkward, query logic becomes infrastructure-heavy.
   - Migration impact: add one or more rule tables (or JSON rules) plus a rule evaluator; likely more than one adapter/query change.
   - Domain purity: weaker — dynamic query semantics leak into eligibility logic.
   - Audit trail: weak to medium unless rules are versioned and evaluated into snapshots.
   - Effort: High.

3. **Option C: hybrid rules + materialized census** — save rules, materialize them into `censo_electoral`, and allow manual overrides.
   - Pros: best long-term flexibility, keeps an auditable snapshot, supports both bulk assignment and individual exceptions.
   - Cons: highest complexity; requires refresh/rebuild semantics, conflict rules, and source-of-truth decisions.
   - Migration impact: `censo_electoral` plus rule tables and source metadata.
   - Domain purity: medium — still manageable, but only with clear ownership boundaries.
   - Audit trail: best, if membership source and override history are tracked.
   - Effort: High.

### Recommendation
Choose **Option A** for this MVP change. The requirement is not “build a generic rule engine”; it is “let admins define who can vote in this election, including bulk add by filters.” A dedicated `censo_electoral` table plus bulk-assignment use cases gives explicit, auditable membership with much lower complexity. Keep the existing global check as a second guard (`in censo` **and** `ACTIVO` **and** `puede_votar=true`) so HR changes still invalidate ineligible people without rebuilding the census.

### Risks
- **Documentation drift already exists**: `IssueVotingTokenCommand` and `TokenController` say issuance is only for `ACTIVA` elections, but `IssueVotingTokenUseCaseImpl` does not enforce that today.
- **Spec drift already exists**: auth spec says funcionario listing supports department and labor-status filters, but `FuncionarioJpaRepository.search(...)` only implements free-text search.
- The codebase has both `Election` and legacy `Eleccion` models; adding census data to the wrong abstraction will increase duplication.
- Need a product decision on whether census edits are allowed after activation. Restricting edits to `PROGRAMADA` is safer, but it is not defined yet.
- Repo guidance says “V1 is canonical, no V2 until production data exists”; the requested “new V2 migration” conflicts with that convention unless the team explicitly decides production-like migration history now matters more.

### Ready for Proposal
Yes — recommend a proposal centered on Option A, with: (1) election-scoped census table, (2) electoral admin endpoints/use cases for bulk add/remove/list, (3) an election-aware voter eligibility port used by token issuance, and (4) filtered funcionario queries to support department/status/eligibility bulk selection.
