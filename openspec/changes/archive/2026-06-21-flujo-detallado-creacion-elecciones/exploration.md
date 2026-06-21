## Exploration: Flujo Detallado de Creación de Elecciones

### Current State
Currently, the election creation flow in the MVP is minimal. An election is created with only `codigo`, `nombre`, `fechaInicio`, and `fechaFin`. The "Voto en Blanco" candidate is automatically created during activation. Candidates are added with basic fields: `nombre`, `descripcion`, and `numeroOrden`. Voting allows a single selection, and the `votos` table enforces a unique `token_id` constraint, ensuring a token casts only one vote. The portal voting flow has a single ballot step followed immediately by submission.

### Affected Areas

**Backend (`Electoral-Votapp`):**
- `co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/EleccionEntity.java` — Needs new schema columns (`permiteVotoBlanco`, `maxVotosPorElector`).
- `co/com/votapp/ws/candidates/infrastructure/adapter/out/persistence/CandidatoEntity.java` — Needs new schema columns (`fotoUrl`, `biografia`, `propuestas`, `afiliacionPolitica`).
- `co/com/votapp/ws/electoral/domain/Election.java` & `co/com/votapp/ws/candidates/domain/Candidato.java` — Domain models must reflect new properties.
- `co/com/votapp/ws/electoral/infrastructure/adapter/in/web/ElectionController.java` — Update requests and responses to handle the new fields.
- `co/com/votapp/ws/electoral/domain/usecase/ActivateElectionUseCaseImpl.java` — Must check `permiteVotoBlanco` before creating the synthetic blank vote.
- `co/com/votapp/ws/voting/domain/usecase/CastVoteByTokenIdUseCaseImpl.java` — Must be updated to accept a list of candidate IDs, validate against `maxVotosPorElector`, and handle multiple inserts.
- `co/com/votapp/ws/voting/infrastructure/adapter/out/persistence/VoteEntity.java` — The `unique = true` constraint on `token_id` must be removed to allow multiple rows per token for multi-voting.

**Frontend (`Electoral-Votapp-Frontend`):**
- `src/pages/CreateElectionPage.tsx` & `src/pages/ElectionDetailPage.tsx` — Add toggles and inputs for election configuration (blank vote, max votes) and rich candidate fields (photo, bio, proposals, affiliation).
- `src/api/client.ts` & `src/api/portalClient.ts` — Update interfaces for payloads and responses.
- `src/pages/portal/VotePage.tsx` — Implement rich UI for candidates, change to multi-selection state, enforce validation (max votes, blank vote mutual exclusivity), and add a "Review" step before confirming the vote.

### Approaches

1. **Rich Candidate Fields & Multi-voting (Recommended)**
   - **Pros:** Directly fulfills business requests; keeps multi-voting normalized by storing one row per candidate vote in the `votos` table.
   - **Cons:** Requires a database schema migration. Removing the `token_id` unique constraint shifts the atomic guarantee entirely to the Redis lock and `VotingToken` status, which is fine but requires careful testing.
   - **Effort:** Medium/High

2. **Store multi-votes as JSON array**
   - **Pros:** Keeps `token_id` unique in the `votos` table; no change to uniqueness constraint.
   - **Cons:** Anti-pattern for SQL reporting; makes querying election results more complex.
   - **Effort:** Medium

### Recommendation
**Approach 1** is recommended. The MVP is built on Postgres, and keeping votes normalized (one row per token-candidate pair) allows for straightforward aggregation for results. The `token_id` unique constraint in `votos` should be dropped, relying on the `tokenLockPort` (Redis lock) and the token's `USED` status to prevent double-voting. The frontend `VotePage` should introduce a `step` state to accommodate the new Review screen.

### Risks
- **Concurrency & Integrity:** With the `token_id` uniqueness constraint removed from `votos`, we strictly rely on the Redis lock and `VotingToken` state update to prevent a token from voting twice. 
- **Bounded Context Overlap:** The candidate model exists in both the `candidates` and `electoral` packages. Both must be updated synchronously to avoid inconsistencies.
- **Mutual Exclusivity Logic:** The frontend and backend must perfectly align on the validation logic (e.g., if "Voto en Blanco" is selected, no other candidate can be selected; max votes cannot exceed the configured limit).

### Ready for Proposal
Yes — The orchestrator should proceed to the proposal phase. The architecture boundaries, impacted files, and database constraints have been fully mapped.