# Voting Specification

## Purpose

Core voting logic, ballot casting, and multi-selection validation.

## Requirements

### Requirement: Multi-Candidate Voting Validation

The system MUST validate that a vote payload does not exceed the `maxVotosPorElector` configured for the active election.

#### Scenario: Cast vote within limit
- GIVEN an active election with `maxVotosPorElector=3`
- WHEN a voter submits a ballot selecting 2 candidates
- THEN the system MUST accept and process the vote

#### Scenario: Reject vote exceeding limit
- GIVEN an active election with `maxVotosPorElector=1`
- WHEN a voter submits a ballot selecting 2 candidates
- THEN the system MUST reject the vote with a validation error

### Requirement: Blank Vote Mutual Exclusivity

The system MUST ensure that if the "Voto en Blanco" candidate is selected, no other candidate can be selected in the same ballot.

#### Scenario: Cast blank vote only
- GIVEN an active election where blank vote is permitted
- WHEN a voter submits a ballot containing only the "Voto en Blanco" candidate ID
- THEN the system MUST accept and process the vote

#### Scenario: Reject mixed blank and normal vote
- GIVEN an active election
- WHEN a voter submits a ballot containing the "Voto en Blanco" candidate ID AND another candidate ID
- THEN the system MUST reject the vote with a validation error

### Requirement: Atomic Multi-Selection Cast

The system MUST guarantee atomicity when persisting multiple candidate selections for a single voting token, relying strictly on distributed locking and token state validation.

#### Scenario: Successful atomic multi-vote
- GIVEN a valid, unused voting token
- WHEN the voter submits a valid multi-selection ballot
- THEN the system MUST acquire the lock
- AND insert a row in the `votos` table for each selected candidate
- AND mark the token as `USED`
- AND release the lock

#### Scenario: Reject double voting attempt
- GIVEN a voting token that is currently being processed or is already `USED`
- WHEN a concurrent or subsequent vote is submitted
- THEN the system MUST reject the request, preventing duplicate inserts