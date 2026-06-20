# Proposal: Censo Electoral

## Intent
Allow administrators to define a specific voter census for each election, replacing the current global-only eligibility. This enables bulk assignment by department and ensures only relevant demographic groups can vote per election.

## Scope

### In Scope
- Create `censo_electoral` table.
- Add domain models (`CensoElectoral`) and ports (`ManageCensoElectoralUseCase`, `CensoElectoralRepositoryPort`) in the `electoral` context.
- Add REST endpoints for bulk census management under `/api/v1/elections/{electionId}/censo`.
- Update `FuncionarioRepositoryPort` with filtered queries (departamento, estadoLaboral, puedeVotar).
- Update `VoterEligibilityRepositoryPort` to check election-scoped eligibility.
- Update `IssueVotingTokenUseCaseImpl` to enforce per-election eligibility.
- Restrict census modifications to `PROGRAMADA` election state.

### Out of Scope
- Vote casting flow and token model changes.
- Modifying `participacion_electoral` structure.
- Frontend implementation.

## Capabilities

### New Capabilities
- `censo-management`: Admin endpoints to add, remove, and list election-specific eligible voters, including bulk addition.

### Modified Capabilities
- `token-issuance`: Token issuance verification will require election-specific census membership AND global active/eligible status.

## Approach
Option A (materialized census table). A `V2` migration introduces `censo_electoral`. The `electoral` context manages bulk assignments via new endpoints. Token issuance in the `voting` context queries `votereligibility`, which now checks both the `censo_electoral` table and global `ACTIVO` constraints.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `db/migration/V2__Censo_electoral.sql` | New | Migration script for `censo_electoral` |
| `co.com.votapp.ws.electoral.domain.*` | New | `CensoElectoral` model, input/output ports |
| `co.com.votapp.ws.electoral.adapter.*` | Modified | Add `CensoController` endpoints |
| `co.com.votapp.ws.votereligibility.*` | Modified | `isEligibleForElection` port logic |
| `co.com.votapp.ws.voting.*` | Modified | `IssueVotingTokenUseCaseImpl` validation |
| `co.com.votapp.ws.auth.*` | Modified | Filtered query in `FuncionarioRepositoryPort` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Spec drift in Token Issuance | High | `IssueVotingTokenUseCaseImpl` lacks `ACTIVA` enforcement; fix alongside census logic. |
| V1 vs V2 convention | Medium | Explicitly bypass the 'V1 canonical' rule to support production-like history. |

## Rollback Plan
- Revert application code.
- Drop `censo_electoral` table (`DROP TABLE censo_electoral;`).
- Token issuance resumes falling back to global eligibility.

## Dependencies
- None

## Success Criteria
- [ ] Admins can bulk-add all eligible employees of a department to an election.
- [ ] Employees NOT in the election census are rejected during token issuance.
- [ ] Employees IN the census but globally INACTIVO are rejected during token issuance.
- [ ] Census operations fail if the election is not PROGRAMADA.
