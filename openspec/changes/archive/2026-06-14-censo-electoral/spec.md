# Specification: Censo Electoral

## New Domain: Censo Management

### Purpose
Admin-facing management of the election-specific voter census, including bulk operations based on demographic filters.

### Requirements

#### Requirement: Census Lifecycle
The system MUST restrict census modifications to elections in the `PROGRAMADA` state.

- **Scenario: Modify census when election is PROGRAMADA**
  - GIVEN an election in `PROGRAMADA` state
  - WHEN an admin attempts to add, remove, or clear the census
  - THEN the system MUST perform the operation successfully

- **Scenario: Modify census when election is ACTIVA**
  - GIVEN an election in `ACTIVA` state
  - WHEN an admin attempts to add, remove, or clear the census
  - THEN the system MUST reject the operation with an error

- **Scenario: Modify census when election is FINALIZADA or CANCELADA**
  - GIVEN an election in `FINALIZADA` or `CANCELADA` state
  - WHEN an admin attempts to modify the census
  - THEN the system MUST reject the operation with an error

#### Requirement: Bulk-Add by Filter
The system MUST allow admins to bulk-add funcionarios to an election's census using filters (`departamentoId`, `estadoLaboral`, `puedeVotar`).

- **Scenario: Bulk-add by departamento (happy path)**
  - GIVEN an election in `PROGRAMADA` state
  - WHEN an admin bulk-adds using a specific `departamentoId`
  - THEN the system MUST add all matching, globally eligible (`ACTIVO`, `puede_votar=true`) funcionarios to the census
  - AND return the count of added and skipped funcionarios

- **Scenario: Bulk-add with some already in census (idempotent)**
  - GIVEN an election in `PROGRAMADA` state
  - AND a census that already contains some funcionarios from a department
  - WHEN an admin bulk-adds using that same `departamentoId`
  - THEN the system MUST skip the duplicates
  - AND add only the missing matching funcionarios
  - AND return the correct added and skipped counts

#### Requirement: Individual Add/Remove
The system MUST allow admins to individually add or remove a specific funcionario from an election's census.

- **Scenario: Individual add of eligible funcionario**
  - GIVEN an election in `PROGRAMADA` state
  - WHEN an admin adds a specific funcionario who is `ACTIVO` and `puede_votar=true`
  - THEN the system MUST add the funcionario to the census successfully

- **Scenario: Individual add of INACTIVO funcionario**
  - GIVEN an election in `PROGRAMADA` state
  - WHEN an admin adds a specific funcionario who is `INACTIVO` or has `puede_votar=false`
  - THEN the system MUST reject the operation with an error

- **Scenario: Individual remove from census**
  - GIVEN an election in `PROGRAMADA` state
  - AND a funcionario currently in the census
  - WHEN an admin removes the funcionario
  - THEN the system MUST remove the funcionario from the census

#### Requirement: Clear Census
The system MUST allow admins to completely clear an election's census.

- **Scenario: Clear census**
  - GIVEN an election in `PROGRAMADA` state with an existing census
  - WHEN an admin clears the census
  - THEN the system MUST remove all entries for that election

#### Requirement: List Census
The system MUST provide a paginated list of the current census for an election, with search and filter capabilities.

- **Scenario: List census with pagination**
  - GIVEN an election with an existing census
  - WHEN an admin requests the census list with pagination parameters
  - THEN the system MUST return the paginated list of funcionarios in the census

#### Requirement: List Eligible Funcionarios (Not in Census)
The system MUST provide a read endpoint to list eligible funcionarios who are NOT yet in the election's census, filterable by department, status, and eligibility.

- **Scenario: List eligible for selection**
  - GIVEN an election with a partial census
  - WHEN an admin requests eligible funcionarios not in the census
  - THEN the system MUST return only those eligible who are absent from the census

#### Requirement: Audit Census Modifications
The system MUST audit all census modifications (bulk-add, individual add/remove, clear).

- **Scenario: Audit modification operations**
  - GIVEN any successful census modification
  - WHEN the operation completes
  - THEN the system MUST log an audit event containing who performed it, what action was taken, and when

---

## Delta for Token Issuance

## MODIFIED Requirements

### Requirement: Election-Scoped Token Issuance Eligibility

The system MUST verify that a requesting voter is explicitly included in the election's census, AND meets the global eligibility criteria (`ACTIVO` and `puede_votar=true`), before issuing a voting token. If the election's census is completely empty, the system MUST fallback to allowing all globally eligible voters to participate.
(Previously: The system only verified global eligibility without checking election-specific census.)

#### Scenario: Token issuance when funcionario is in census
- GIVEN an `ACTIVA` election with a populated census
- AND a requesting funcionario who is in the census, `ACTIVO`, and `puede_votar=true`
- WHEN the funcionario requests a voting token
- THEN the system MUST issue the token successfully

#### Scenario: Token issuance when funcionario is NOT in census
- GIVEN an `ACTIVA` election with a populated census
- AND a requesting funcionario who is `ACTIVO` and `puede_votar=true` but NOT in the census
- WHEN the funcionario requests a voting token
- THEN the system MUST reject the request with an eligibility error

#### Scenario: Token issuance when census is empty (backward compatibility)
- GIVEN an `ACTIVA` election with an EMPTY census
- AND a requesting funcionario who is `ACTIVO` and `puede_votar=true`
- WHEN the funcionario requests a voting token
- THEN the system MUST allow token issuance
- AND issue the token successfully

#### Scenario: Token issuance when funcionario is in census but globally ineligible
- GIVEN an `ACTIVA` election with a populated census
- AND a requesting funcionario who is in the census but has become `INACTIVO` or `puede_votar=false`
- WHEN the funcionario requests a voting token
- THEN the system MUST reject the request with an eligibility error
