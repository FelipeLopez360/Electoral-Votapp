# Delta for Auth

## MODIFIED Requirements

### Requirement: List Funcionarios

The system MUST allow an admin to list all funcionarios with optional search/filter by nombre, documento, departamento, and estado_laboral, returning a paginated response defaulting to 8 items per page.
(Previously: The system allowed an admin to list all funcionarios returning a simple list.)

#### Scenario: List all funcionarios

- GIVEN there are funcionarios in the system
- WHEN an admin requests `GET /api/v1/funcionarios`
- THEN the system MUST return a paginated response defaulting to `size=8`
- AND the response MUST include `content`, `page`, `size`, `totalElements`, and `totalPages`
- AND the items in `content` MUST include: `id`, `numeroEmpleado`, `documentoIdentidad`, `nombres`, `apellidos`, `email`, `departamento`, `cargo`, `estadoLaboral`, `puedeVotar`, `debeCambiarPassword`

#### Scenario: Filter by search term

- GIVEN funcionarios exist with various names
- WHEN an admin requests `GET /api/v1/funcionarios?search=Juan`
- THEN the system MUST return a paginated response of funcionarios matching by `nombres`, `apellidos`, or `documentoIdentidad`
- AND the pagination MUST default to `size=8` if not specified
- AND `totalElements` MUST reflect the count of matching items only
