# Voting & Participation Specification

## Purpose
Handles the generation of voting tokens, validation, anonymous vote casting, and participation tracking.

## Requirements

### Requirement: Anonymous Vote Casting
The system MUST register votes without linking the voter's identity to their specific choice.

#### Scenario: Cast Vote
- GIVEN a voter with a valid, unused token
- WHEN the voter submits their choices
- THEN the system MUST record the vote choices anonymously
- AND mark the token as used or record participation separately to prevent double voting.

### Requirement: Token Validation with Redis
The system MUST validate voting tokens using Redis to guarantee atomicity and prevent double voting.

#### Scenario: Prevent Double Voting
- GIVEN a voter who has already used their token
- WHEN they attempt to vote again with the same token
- THEN the system MUST use Redis SETNX to check token status
- AND reject the second vote attempt instantly.