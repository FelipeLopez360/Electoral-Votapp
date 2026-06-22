# Admin Dashboard Metrics Specification

## Purpose

Provides a read-only aggregate endpoint to retrieve summary counts for the administrative dashboard. This prevents the abuse of paginated APIs for simple metrics and provides an efficient overview of system state, specifically regarding active voters, funcionario statuses, and election statuses.

## Requirements

### Requirement: Admin Dashboard Metrics Retrieval

The system MUST provide an endpoint to retrieve aggregated counts of funcionarios, eligible active voters, and elections by status.

#### Scenario: Successfully retrieving dashboard metrics
- GIVEN an authenticated administrative user
- WHEN the user requests the dashboard metrics
- THEN the system returns the total number of funcionarios grouped by labor status
- AND the system returns the total number of eligible active voters (funcionarios where estado_laboral = ACTIVO and puede_votar = true)
- AND the system returns the total number of elections grouped by status

#### Scenario: Retrieving metrics when no data exists
- GIVEN an authenticated administrative user and an empty database
- WHEN the user requests the dashboard metrics
- THEN the system returns counts of 0 for all categories

#### Scenario: Accurate voter eligibility counts
- GIVEN a database containing funcionarios with various combinations of `estado_laboral` and `puede_votar`
- WHEN the user requests the dashboard metrics
- THEN the `active_voters` count exactly matches the number of funcionarios where `estado_laboral = 'ACTIVO'` and `puede_votar = true`
