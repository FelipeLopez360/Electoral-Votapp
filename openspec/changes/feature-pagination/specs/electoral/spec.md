# Delta for Electoral

## ADDED Requirements

### Requirement: List Elections

The system MUST provide a paginated list of all elections, defaulting to 8 items per page, and MUST support searching by election name or code.

#### Scenario: List elections with default pagination

- GIVEN there are multiple elections in the system
- WHEN an admin requests `GET /api/v1/elections` without pagination parameters
- THEN the system MUST return a paginated response defaulting to `page=0` and `size=8`
- AND the response MUST include `content`, `page`, `size`, `totalElements`, and `totalPages`
- AND the items in `content` MUST be ordered by creation date descending (`createdAt DESC`)

#### Scenario: Search elections by name or code

- GIVEN elections exist with various names and codes
- WHEN an admin requests `GET /api/v1/elections?search=Presidencial`
- THEN the system MUST return a paginated response containing only elections where the `nombre` or `codigo` matches "Presidencial" (case-insensitive)

#### Scenario: Requesting a specific page

- GIVEN more than 8 elections exist
- WHEN an admin requests `GET /api/v1/elections?page=1&size=8`
- THEN the system MUST return the second page of results (items 9-16)
- AND `totalPages` MUST reflect the correct total number of pages
