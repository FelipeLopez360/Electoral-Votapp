# Proposal: Detailed Election Creation Flow

## Intent

Provide administrators a rich, multi-step election creation flow that includes candidate management, ballot configuration, and a final review step before publishing, ensuring accuracy in election setups.

## Scope

### In Scope
- Add election ballot settings (`permiteVotoBlanco`, `maxVotosPorElector`).
- Add rich candidate profiles (`fotoUrl`, `biografia`, `propuestas`, `afiliacionPolitica`).
- Multi-step UI in `CreateElectionPage` (Admin portal): Basic Info, Candidates, Ballot Config, and a final **Review before publishing**.
- Update backend schema, domain, and API contracts to support new election and candidate fields.
- Update voting use cases strictly as required to support the new ballot configurations (multi-voting limit and blank vote mutual exclusivity).

### Out of Scope
- A review step in the *voter* portal flow (`VotePage`).
- Reporting on complex demographic breakdowns.
- Dynamic weighted voting.

## Capabilities

> This section is the CONTRACT between proposal and specs phases.
> The sdd-spec agent reads this to know exactly which spec files to create or update.

### New Capabilities
- `election-creation-flow`: Multi-step admin wizard for configuring elections and candidates with a final review step.

### Modified Capabilities
- `electoral`: Update API and schemas to handle election configurations (blank vote flag, max votes) and rich candidate profiles.
- `voting`: Support for atomic multi-voting per token and selection limit validation (downstream impact of new ballot config).

## Approach

**Backend:** Update database schemas (`EleccionEntity`, `CandidatoEntity`) to include the new fields. Drop the `token_id` uniqueness constraint in `VoteEntity` to allow multiple rows per token for multi-voting, relying strictly on Redis lock and token status to guarantee atomicity. Modify use cases to enforce the new validation rules.
**Frontend:** Transform `CreateElectionPage` into a multi-step wizard. Step 1: Basic Info. Step 2: Candidates (rich profiles). Step 3: Ballot config (max votes, blank vote). Step 4: Final Review step before the admin publishes or schedules the election. Apply minimal updates to `VotePage` simply to handle the multi-selection state if configured.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `backend: database schemas` | Modified | Add columns to elections and candidates. Drop `token_id` unique constraint on `votos`. |
| `backend: domain & usecases` | Modified | Update models, add validation for `permiteVotoBlanco` and `maxVotosPorElector`. |
| `frontend: pages` | Modified | Overhaul `CreateElectionPage` into a multi-step wizard with a final review screen. |
| `frontend: voting flow` | Modified | `VotePage` updated to allow multi-selection based on election config. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Concurrency on multi-voting | High | Rely on Redis lock (`tokenLockPort`) and `USED` token state to ensure a token can only be consumed once. |
| State loss in Admin wizard | Medium | Maintain wizard state locally in React until the final Review and Publish submission. |

## Rollback Plan

Revert database schema changes (restore `token_id` unique constraint on `votos` table), revert use case logic, and roll back frontend `CreateElectionPage` to the single-step form.

## Dependencies

- None external.

## Success Criteria

- [ ] Admin can navigate a multi-step election creation wizard.
- [ ] Admin can configure `permiteVotoBlanco` and `maxVotosPorElector`.
- [ ] Admin can add candidates with photo URL, bio, proposals, and affiliation.
- [ ] Admin sees a "Review" screen before finalizing the election publication.
- [ ] Voters can select up to `maxVotosPorElector` candidates during the vote.