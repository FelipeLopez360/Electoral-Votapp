# Archive Report: Flujo Detallado de Creación de Elecciones

## Business Goal Achieved
Provided administrators with a rich, multi-step election creation flow. This includes candidate management with rich profiles (photos, bios, proposals, affiliations), ballot configuration (blank vote toggles, maximum votes per voter limit), and a final review step before publishing. This ensures accuracy in election setups and allows voters to select multiple candidates based on the new ballot configurations.

## Technical Boundaries Altered
- **Database Schema**: Dropped the unique constraint on `token_id` in the `votos` table to allow multiple vote rows per token, enabling multi-voting. Added new ballot config columns (`permite_voto_blanco`, `max_votos_por_elector`) to the `elecciones` table and rich profile columns to the `candidatos` table.
- **Domain & API**: Added a new comprehensive `POST /api/v1/elections/full` endpoint to process the wizard's final submission atomically. Extended the portal's voting contract to accept an array of candidate IDs, relying on Redis lock and token state to guarantee atomicity.
- **Frontend Architecture**: Transitioned `CreateElectionPage` into a 4-step wizard with local state management (Basic Info -> Candidates -> Ballot Config -> Review). Modified `VotePage` to handle multi-selection limits and mutual exclusivity for blank votes.

## Lessons Learned & Compromises Made
- **Backward Compatibility**: Discovered a backward-compatibility regression where the legacy `CreateElectionRequest` was missing default ballot configurations when primitives (`boolean`/`int`) were used instead of wrappers. Wrapper types (`Boolean`/`Integer`) were applied to safely bind omitted JSON fields to `null`, correctly delegating to historical defaults.
- **Error Handling**: Found that domain invariant violations from compact constructors were returning HTTP 500s. Resolved by mapping `IllegalArgumentException` to HTTP 400 in the `GlobalExceptionHandler`.
- **Atomicity Compromise**: To allow multi-voting per token while ensuring security, the unique constraint on `votos.token_id` was dropped. Instead of a composite unique constraint that would block legitimate multi-voting, the system now relies entirely on the distributed Redis lock and the token's `USED` state validation.

## Final File Manifest

### Backend
- `src/main/resources/db/migration/V4__Ballot_config_and_candidate_profiles.sql`
- `src/main/java/co/com/votapp/ws/electoral/domain/Election.java`
- `src/main/java/co/com/votapp/ws/electoral/domain/Candidate.java`
- `src/main/java/co/com/votapp/ws/electoral/application/command/CreateElectionCommand.java`
- `src/main/java/co/com/votapp/ws/electoral/application/command/AddCandidateCommand.java`
- `src/main/java/co/com/votapp/ws/electoral/domain/usecase/CreateElectionUseCaseImpl.java`
- `src/main/java/co/com/votapp/ws/electoral/domain/usecase/AddCandidateUseCaseImpl.java`
- `src/main/java/co/com/votapp/ws/electoral/domain/usecase/ActivateElectionUseCaseImpl.java`
- `src/main/java/co/com/votapp/ws/electoral/application/service/CreateElectionWithCandidatesAppService.java`
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/in/web/ElectionController.java`
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/EleccionEntity.java`
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/ElectionRepositoryAdapter.java`
- `src/main/java/co/com/votapp/ws/candidates/infrastructure/adapter/out/persistence/CandidatoEntity.java`
- `src/main/java/co/com/votapp/ws/electoral/infrastructure/adapter/out/persistence/CandidateRepositoryAdapter.java`
- `src/main/java/co/com/votapp/ws/voting/domain/port/in/CastVoteByTokenIdPort.java`
- `src/main/java/co/com/votapp/ws/voting/domain/usecase/CastVoteByTokenIdUseCaseImpl.java`
- `src/main/java/co/com/votapp/ws/voting/application/service/CastVoteAppService.java`
- `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/out/persistence/VoteEntity.java`
- `src/main/java/co/com/votapp/ws/voting/infrastructure/adapter/in/web/portal/PortalVotingController.java`

### Frontend
- `src/pages/CreateElectionPage.tsx`
- `src/pages/portal/VotePage.tsx`
- `src/api/client.ts`
- `src/api/portalClient.ts`
