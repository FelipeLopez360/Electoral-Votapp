# Delta for Electoral

## ADDED Requirements

### Requirement: Candidate Registration by Funcionario

The system MUST allow registration of candidates for an election using `funcionarioId` to logically link the candidate to an actual official within the organization.

#### Scenario: Registering a valid candidate

- GIVEN an election in `PROGRAMADA` state
- WHEN an admin adds a candidate providing a valid `funcionarioId` and other details (name, photoUrl, bio, proposals) via `POST /api/v1/elections/{id}/candidates` or `POST /api/v1/elections/full`
- THEN the system MUST save the candidate
- AND link the candidate to the provided `funcionarioId`

#### Scenario: Registering a duplicate candidate in the same election

- GIVEN an election in `PROGRAMADA` state that already has a candidate linked to `funcionarioId` X
- WHEN an admin attempts to add another candidate with the same `funcionarioId` X to the same election
- THEN the system MUST reject the request with a validation error (HTTP 409 Conflict)

### Requirement: Alphabetical Ballot Ordering

The system MUST return candidates ordered alphabetically by their `nombre` when retrieving the ballot or candidate lists, completely replacing manual numeric ordering.

#### Scenario: Retrieve ballot or candidate list

- GIVEN an election with multiple candidates
- WHEN a voter or admin retrieves the ballot or list of candidates
- THEN the system MUST return the candidates sorted alphabetically by `nombre` in ascending order

## REMOVED Requirements

### Requirement: Manual Candidate Ordering

(Reason: The `numeroOrden` field is obsolete; the system now automatically enforces alphabetical sorting by name to simplify candidate registration.)
(Migration: Drop the `numero_orden` column from the database and remove `numeroOrden` from all API DTOs and domain models.)

### Requirement: Candidate Political Affiliation

(Reason: The `afiliacionPolitica` field is obsolete as the election is internal to the organization and political affiliations are not applicable.)
(Migration: Drop the `afiliacion_politica` column from the database and remove `afiliacionPolitica` from all API DTOs and domain models.)
