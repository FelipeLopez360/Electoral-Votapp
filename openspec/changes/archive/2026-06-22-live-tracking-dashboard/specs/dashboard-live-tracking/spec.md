# Dashboard Live Tracking Specification

## Purpose

Retrieve real-time participation progress for active elections via a dedicated polling endpoint, providing the admin dashboard with lightweight tracking data without burdening global aggregations.

## Requirements

### Requirement: Retrieve Active Elections Tracking Data

The system MUST expose a `GET /api/v1/dashboard/live-tracking` endpoint that returns a list of all elections currently in the `ACTIVA` status.

#### Scenario: No active elections exist
- GIVEN no elections have the status `ACTIVA`
- WHEN a client requests the live tracking endpoint
- THEN the system returns a 200 OK
- AND the response contains an empty list

#### Scenario: Multiple active elections exist
- GIVEN multiple elections have the status `ACTIVA`
- WHEN a client requests the live tracking endpoint
- THEN the system returns a 200 OK
- AND the response contains a list of all active elections with their ID and title

### Requirement: Count Cast Votes (Participation)

For each active election returned, the system MUST accurately count the total number of cast votes from the participation records.

#### Scenario: Election with cast votes
- GIVEN an active election has recorded participation entries
- WHEN a client requests the live tracking endpoint
- THEN the response for that election MUST include `totalCastVotes` equal to the number of participation entries

#### Scenario: Election with zero votes
- GIVEN an active election has no recorded participation entries
- WHEN a client requests the live tracking endpoint
- THEN the response for that election MUST include `totalCastVotes` equal to 0

### Requirement: Calculate Total Eligible Voters (Fallback Logic)

For each active election returned, the system MUST calculate the total eligible voters. The system MUST first check the custom census for the election. If the census count is greater than 0, it MUST be used. If the census count is 0, the system MUST fall back to counting global active voters (voters with `estado_laboral='ACTIVO'` and `puede_votar=true`).

#### Scenario: Election with custom census
- GIVEN an active election has a custom census configured (count > 0)
- WHEN a client requests the live tracking endpoint
- THEN the response for that election MUST include `totalEligibleVoters` equal to the custom census count
- AND the system MUST NOT query global active voters

#### Scenario: Election without custom census
- GIVEN an active election does NOT have a custom census (count == 0)
- WHEN a client requests the live tracking endpoint
- THEN the system MUST count global active voters
- AND the response for that election MUST include `totalEligibleVoters` equal to the global active voters count
