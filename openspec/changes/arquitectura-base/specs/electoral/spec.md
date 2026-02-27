# Electoral Management Specification

## Purpose
Configures and manages elections, electoral periods, and election types.

## Requirements

### Requirement: Election Configuration
The system MUST allow admins to define elections, associating them with types and periods.

#### Scenario: Create Electoral Period
- GIVEN a user with administrative privileges
- WHEN the user defines start and end dates for a new electoral period
- THEN the system MUST validate that the end date is after the start date
- AND save the electoral period.

### Requirement: Active Election Validation
The system MUST determine if an election is currently active based on its scheduled period.

#### Scenario: Voting Outside Period
- GIVEN an election that has not yet started or has already ended
- WHEN a voter attempts to interact with the election
- THEN the system MUST deny the action because the election is inactive.