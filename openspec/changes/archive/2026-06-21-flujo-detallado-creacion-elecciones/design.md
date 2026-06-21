# Design: Detailed Election Creation Flow

## Technical Approach

Extend the existing `electoral` aggregate and the portal `voting` cast flow to carry
ballot configuration (`permiteVotoBlanco`, `maxVotosPorElector`) and rich candidate
profiles (`fotoUrl`, `biografia`, `propuestas`, `afiliacionPolitica`). The admin
`CreateElectionPage` becomes a 4-step wizard (Basic Info → Candidates → Ballot Config →
Review) that holds ALL state locally and submits ONCE at Review via a single
**comprehensive payload** to a new `POST /api/v1/elections/full` endpoint — no orphan
elections, one transaction. The portal vote contract changes from a single path-param
candidate to a multi-selection request body, because `maxVotosPorElector > 1` requires N
`votos` rows per token. Atomicity stays on the existing Redis SETNX lock + token `USED`
gate; we drop the `votos.token_id` UNIQUE constraint so one token can own N rows.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| Final-submit contract | Single `POST /elections/full` comprehensive payload (election + candidates) in one `@Transactional` app service | `POST /elections` + N `POST /candidates` loop | Spec Review step says "send the comprehensive payload"; one call = atomic create, no partial/orphan election if a candidate fails |
| Keep existing endpoints | `POST /elections`, `POST /elections/{id}/candidates` remain (now config/profile-aware) | Delete them | Used by other flows (add candidate to PROGRAMADA/ACTIVA); only ADD the aggregate endpoint |
| Ballot config home | New fields on `Election` record + `elecciones` columns | Separate `ballot_config` table | One row per election; avoids a join on the hot cast path |
| Rich profile home | New columns on existing `candidatos` table via `CandidatoEntity` | New table | Candidate is 1:1 with profile; reuses the shared `candidatos` table |
| Multi-vote uniqueness | Drop `votos.token_id` UNIQUE; keep Redis lock + token `USED` gate | Composite UNIQUE `(token_id, candidato_id)` | `markUsed()` already serializes one token; composite blocks legitimate N-row multi-vote |
| Cast contract shape | `castVote(UUID tokenId, List<UUID> candidatoIds)` + body `{candidatoIds:[...]}` | Loop single call per candidate | One lock, one `markUsed`, one transaction → true atomicity |
| Voting UI read contract | Extend `GET /portal/ballot/{eleccionId}` with `maxVotosPorElector`, `permiteVotoBlanco`, and rich candidate fields | New endpoint | UI already calls this before voting; single round-trip drives multi-select + blank exclusivity |
| Duplicate candidate IDs | Cast use case de-duplicates the list, THEN validates count and blank exclusivity | Reject any duplicate as error | Idempotent UX (double-click safe); count check applies to distinct selections |
| Multi-select validation | Domain use case enforces `maxVotosPorElector` + blank/null exclusivity, revalidated vs persisted config | Controller-only checks | Invariants must live in domain, authoritative under concurrency |
| Wizard state | Local `useState`/`useReducer`, submit once at Review | Persist per step | Spec requires local-only state until publish |
| Synthetic candidates | `Voto en Blanco` created on activation ONLY when `permiteVotoBlanco`; `Voto Nulo` unchanged; both keep rich fields NULL | Manage blank manually | Preserves existing activation contract; gates only blank creation |

## Data Flow

### Admin create (wizard → single comprehensive submit)

    CreateElectionPage (local state: basic, candidates[], ballotConfig)
         │  Step 4 "Publish"  → one request
         ▼
    POST /api/v1/elections/full
      { codigo, nombre, fechaInicio, fechaFin,
        permiteVotoBlanco, maxVotosPorElector,
        candidatos: [ { nombre, numeroOrden, fotoUrl, biografia,
                        propuestas, afiliacionPolitica }, ... ] }
         ▼
    CreateElectionWithCandidatesAppService (@Transactional)
      → CreateElectionUseCase.create(...)  (PROGRAMADA)
      → for each candidato: AddCandidateUseCase.addCandidate(...)
         ▼  (returns ElectionResponse incl. ballot config)
    navigate('/elections')

### Portal multi-vote read + cast

    GET /api/v1/portal/ballot/{eleccionId}
      → PortalBallotResponse {
          eleccionId, nombre,
          maxVotosPorElector, permiteVotoBlanco,   // drives multi-select UI
          candidates: [ { id, nombre, esVotoEnBlanco, numeroOrden,
                          fotoUrl, biografia, propuestas, afiliacionPolitica } ] }
         ▼
    VotePage (selected: Set<candidatoId>, cap = maxVotosPorElector)
         ▼
    POST /api/v1/portal/votar/{eleccionId}   body { candidatoIds: [...] }
         ▼
    CastVoteAppService (@Transactional)
         ▼
    CastVoteByTokenIdUseCaseImpl: validate token ISSUED → lock(tokenId) →
      revalidate election ACTIVA → dedupe candidatoIds → validate
      (count ≤ max, blank/null exclusivity, each candidate ∈ election) →
      markUsed → insert N votos → markParticipation → audit → release lock

## File Changes

### Backend (`/Users/felipe-tresesenta/Documents/Electoral-Votapp`)

| File | Action | Description |
|------|--------|-------------|
| `src/main/resources/db/migration/V4__Ballot_config_and_candidate_profiles.sql` | Create | Add `elecciones.permite_voto_blanco BOOLEAN NOT NULL DEFAULT true`, `elecciones.max_votos_por_elector INTEGER NOT NULL DEFAULT 1 CHECK (max_votos_por_elector >= 1)`; add `candidatos.foto_url`, `biografia`, `propuestas`, `afiliacion_politica` (all NULLable TEXT); `ALTER TABLE votos DROP CONSTRAINT votos_token_id_key`; add `CREATE INDEX idx_votos_token ON votos(token_id)`. |
| `electoral/domain/Election.java` | Modify | Add `boolean permiteVotoBlanco`, `int maxVotosPorElector`; compact-ctor validates `maxVotosPorElector >= 1`. |
| `electoral/domain/Candidate.java` | Modify | Add nullable `fotoUrl`, `biografia`, `propuestas`, `afiliacionPolitica`; synthetic candidates pass NULLs. |
| `electoral/application/command/CreateElectionCommand.java` | Modify | Add `permiteVotoBlanco`, `maxVotosPorElector`. |
| `electoral/application/command/AddCandidateCommand.java` | Modify | Add the four rich-profile fields (nullable). |
| `electoral/domain/usecase/CreateElectionUseCaseImpl.java` | Modify | Build `Election` with ballot config. |
| `electoral/domain/usecase/AddCandidateUseCaseImpl.java` | Modify | Pass rich fields into the `Candidate`. |
| `electoral/domain/usecase/ActivateElectionUseCaseImpl.java` | Modify | Create `Voto en Blanco` ONLY when `election.permiteVotoBlanco()`; `Voto Nulo` unchanged. |
| `electoral/application/service/CreateElectionWithCandidatesAppService.java` | Create | `@Service @Transactional` orchestrating `CreateElectionUseCase` + N `AddCandidateUseCase` for the comprehensive payload. |
| `electoral/infrastructure/adapter/in/web/ElectionController.java` | Modify | Add `POST /elections/full` (`CreateElectionFullRequest` with nested `candidatos`); extend `CreateElectionRequest`, `AddCandidateRequest`, `ElectionResponse`, `CandidateResponse` with new fields. |
| `electoral/infrastructure/adapter/out/persistence/EleccionEntity.java` | Modify | Map `permite_voto_blanco`, `max_votos_por_elector`. |
| `electoral/infrastructure/adapter/out/persistence/ElectionRepositoryAdapter.java` + `EleccionRepositoryAdapter.java` | Modify | Map new columns in `toDomain`/`toEntity`. |
| `candidates/infrastructure/adapter/out/persistence/CandidatoEntity.java` | Modify | Map `foto_url`, `biografia`, `propuestas`, `afiliacion_politica`. |
| `electoral/infrastructure/adapter/out/persistence/CandidateRepositoryAdapter.java` | Modify | Map rich fields in `toEntity`/`toDomain`. |
| `voting/domain/port/in/CastVoteByTokenIdPort.java` | Modify | `void castVote(UUID tokenId, List<UUID> candidatoIds)`. |
| `voting/domain/usecase/CastVoteByTokenIdUseCaseImpl.java` | Modify | Dedupe list; validate count ≤ `maxVotosPorElector`, blank/null exclusivity, membership; one `markUsed`; insert one `Vote` per candidate inside the locked transaction. |
| `voting/application/service/CastVoteAppService.java` | Modify | `castVoteByTokenId(UUID tokenId, List<UUID> candidatoIds)`. |
| `voting/infrastructure/adapter/out/persistence/VoteEntity.java` | Modify | Remove `unique = true` from `token_id` `@Column`. |
| `voting/infrastructure/adapter/in/web/portal/PortalVotingController.java` | Modify | Replace `POST /votar/{eleccionId}/{candidatoId}` with `POST /votar/{eleccionId}` + `{candidatoIds}` body; extend `PortalBallotResponse` with `maxVotosPorElector`, `permiteVotoBlanco`; extend `PortalCandidateItem` with rich fields. |

### Frontend (`/Users/felipe-tresesenta/Documents/Electoral-Votapp-Frontend`)

| File | Action | Description |
|------|--------|-------------|
| `src/pages/CreateElectionPage.tsx` | Modify | 4-step wizard (Basic / Candidates / Ballot Config / Review); single submit calling `elections.createFull(...)`. |
| `src/pages/portal/VotePage.tsx` | Modify | Multi-select up to `maxVotosPorElector`; blank/null exclusivity (auto-clear others); submit `candidatoIds[]`. |
| `src/api/client.ts` | Modify | Add `elections.createFull(payload)`; extend `ElectionResponse` + `CandidateResponse` types and `candidates.add` payload with new fields. |
| `src/api/portalClient.ts` | Modify | `votar(eleccionId, candidatoIds: string[])` sends body; extend `PortalBallotResponse` + `PortalCandidateItem` types. |

## Interfaces / Contracts

```java
// Input port — multi-selection
public interface CastVoteByTokenIdPort {
    void castVote(UUID tokenId, List<UUID> candidatoIds);
}
```

```jsonc
// POST /api/v1/elections/full  (comprehensive final-submit payload — Review step)
{ "codigo": "ELEC-2026-01", "nombre": "...",
  "fechaInicio": "2026-01-01T08:00:00", "fechaFin": "2026-01-02T18:00:00",
  "permiteVotoBlanco": true, "maxVotosPorElector": 1,
  "candidatos": [
    { "nombre": "...", "numeroOrden": 1, "fotoUrl": "...", "biografia": "...",
      "propuestas": "...", "afiliacionPolitica": "..." } ] }

// GET /api/v1/portal/ballot/{eleccionId}  (read contract for the voting UI)
{ "eleccionId": "...", "nombre": "...",
  "maxVotosPorElector": 3, "permiteVotoBlanco": true,
  "candidates": [
    { "id": "...", "nombre": "...", "esVotoEnBlanco": false, "numeroOrden": 1,
      "fotoUrl": "...", "biografia": "...", "propuestas": "...",
      "afiliacionPolitica": "..." } ] }

// POST /api/v1/portal/votar/{eleccionId}  (replaces path candidatoId)
{ "candidatoIds": ["uuid1", "uuid2"] }
```

```typescript
interface BallotConfig { permiteVotoBlanco: boolean; maxVotosPorElector: number }
interface CandidateProfile {
  nombre: string; numeroOrden: number;
  fotoUrl?: string; biografia?: string; propuestas?: string; afiliacionPolitica?: string;
}
```

## Read Contracts (authoritative source per consumer)

| Field(s) | Exposed by | Consumer |
|----------|-----------|----------|
| `maxVotosPorElector`, `permiteVotoBlanco` | `GET /api/v1/portal/ballot/{eleccionId}` | `VotePage` multi-select + blank exclusivity |
| `maxVotosPorElector`, `permiteVotoBlanco` | `GET /api/v1/elections/{id}` (admin) | Admin review/detail screens |
| `fotoUrl`, `biografia`, `propuestas`, `afiliacionPolitica` | `GET /api/v1/portal/ballot/{eleccionId}` | Voter-facing candidate cards |
| `fotoUrl`, `biografia`, `propuestas`, `afiliacionPolitica` | `GET /api/v1/elections/{id}/candidates` (admin) | Admin candidate list / wizard edit |

## Validation Ownership

| Rule | Frontend (UX) | Backend (authoritative) |
|------|---------------|-------------------------|
| `maxVotosPorElector >= 1` | Block step progression | `Election` ctor + DB CHECK |
| Selection count ≤ max | Disable extra checkboxes | Cast use case (revalidated vs persisted config) |
| Blank/null vote exclusivity | Auto-clear others when blank/null picked | Cast use case rejects mixed ballot |
| Duplicate candidate IDs | Set-based selection prevents dupes | Cast use case de-dupes list before count check |
| Required basic fields | Block step progression | Command/record validation |

Frontend validation is convenience only; the cast use case re-reads election config from
the DB and is the single source of truth under concurrency.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Domain unit | `Election`/`Candidate` new-field validation; cast count + blank/null exclusivity + dedupe | `@ExtendWith(MockitoExtension)`, mock ports |
| Domain unit | Activate creates blank ONLY when `permiteVotoBlanco`; null vote unaffected | mock `CandidateRepositoryPort`, verify save args |
| App | `CreateElectionWithCandidatesAppService` orchestration + tx; `CastVoteAppService` passes list | Mockito `InOrder` |
| Adapter IT | Multi-row insert per token after dropping UNIQUE; rich-field persistence | Testcontainers PostgreSQL, `*IT.java` |
| Controller slice | `POST /elections/full` body; `/votar` body parsing; ballot exposes config | `@WebMvcTest`, `*IT.java` |
| Frontend | Wizard step state, Review summary, single submit, multi-select cap, blank exclusivity | Vitest + RTL |

## Migration / Rollout

Additive forward migration `V4` (new columns default-safe; existing PROGRAMADA elections
get `permiteVotoBlanco=true`, `maxVotosPorElector=1` → behavior unchanged). The only
destructive step is dropping the `votos.token_id` UNIQUE constraint (Postgres auto-name
`votos_token_id_key`) plus removing `unique=true` from `VoteEntity` — safe forward;
rollback requires no duplicate `token_id` rows. No data backfill.

## Open Questions

- [ ] `propuestas` stored as TEXT (single field) for MVP — assumed, not a structured list.
- [ ] Blank/null vote does NOT count toward `maxVotosPorElector` (it is mutually exclusive, so the limit is moot when blank/null is selected).
