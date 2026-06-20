# Electoral Specification

## Purpose

Election lifecycle management including voter registration, census management, and ballot administration.

## Capabilities

### censo-management

Admin-facing management of the election-specific voter census, including bulk operations based on demographic filters.

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

---

## Domain: Election Results

### election-results

Read-only aggregation of election outcomes for finalized elections, including candidate vote counts, blank votes, null votes, and participation statistics.

#### Requirement: Finalized Election Gating

The system MUST ONLY provide results and statistics for elections in the `FINALIZADA` state.

- **Scenario: Request results for PROGRAMADA election**
  - GIVEN an election that is `PROGRAMADA`
  - WHEN an admin requests the election results
  - THEN the system MUST reject the request with a state violation error (HTTP 409)

- **Scenario: Request results for ACTIVA election**
  - GIVEN an election that is `ACTIVA`
  - WHEN an admin requests the election results
  - THEN the system MUST reject the request with a state violation error (HTTP 409)

- **Scenario: Request results for FINALIZADA election**
  - GIVEN an election that is `FINALIZADA`
  - WHEN an admin requests the election results
  - THEN the system MUST return aggregated statistics successfully

#### Requirement: Null Vote Representation

The system MUST support null votes using a synthetic candidate approach.

- **Scenario: Null vote creation on activation**
  - GIVEN an election in `PROGRAMADA` state
  - WHEN the election is activated
  - THEN the system MUST ensure a synthetic candidate for null votes exists alongside the blank vote candidate

#### Requirement: Chart-Ready Aggregation

The system MUST provide an aggregated, read-only payload containing candidate vote counts, blank votes, and null votes, suitable for chart rendering.

- **Scenario: Retrieve aggregated results**
  - GIVEN a `FINALIZADA` election
  - WHEN an admin requests the results via `GET /api/v1/elections/{id}/results`
  - THEN the system MUST calculate live vote counts from the `votos` table
  - AND return a structured JSON response mapping each candidate (including blank and null) to their total votes
  - AND include vote percentages and abstention data

#### Requirement: Participation Statistics

The system MUST provide total participation statistics based on the census.

- **Scenario: Retrieve participation data**
  - GIVEN a `FINALIZADA` election
  - WHEN an admin requests the results
  - THEN the system MUST count total eligible voters from `censo_electoral`
  - AND count total participants from `participacion_electoral`
  - AND return the participation count and rate as a percentage

#### Requirement: Winner Visibility

The system MUST identify the winner(s) of the election in the results payload.

- **Scenario: Single winner with no ties**
  - GIVEN a `FINALIZADA` election with one candidate having the highest valid votes
  - WHEN the results are aggregated
  - THEN the system MUST flag that candidate as the winner

- **Scenario: Multiple winners (tie)**
  - GIVEN a `FINALIZADA` election with multiple candidates tied for highest valid votes
  - WHEN the results are aggregated
  - THEN the system MUST flag all tied candidates as winners

---

## Domain: Election Reports

### election-reports

Generation and download of secure, institutional election reports in PDF and Excel formats with watermarks and aggregated statistics.

#### Requirement: Report Export Contracts

The system MUST provide endpoints to generate and download election reports in both PDF and Excel formats.

- **Scenario: Download PDF report**
  - GIVEN a `FINALIZADA` election
  - WHEN an admin requests a PDF export via `GET /api/v1/elections/{id}/results/report.pdf`
  - THEN the system MUST generate a binary PDF file containing the aggregated results and participation stats
  - AND the file MUST include a secure institutional watermark
  - AND return the binary stream with `Content-Type: application/pdf`

- **Scenario: Download Excel report**
  - GIVEN a `FINALIZADA` election
  - WHEN an admin requests an Excel export via `GET /api/v1/elections/{id}/results/report.xlsx`
  - THEN the system MUST generate a binary Excel file containing the aggregated results and participation stats
  - AND the file MUST include a secure institutional watermark
  - AND return the binary stream with `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

#### Requirement: Export Stream Handling

The system SHOULD stream large report generation to avoid excessive memory consumption.

- **Scenario: Stream report generation**
  - GIVEN a request for an election report
  - WHEN the system builds the PDF or Excel file
  - THEN it SHOULD stream the binary output directly to the HTTP response via `StreamingResponseBody`
  - AND use memory-efficient libraries (OpenPDF for PDF, SXSSF for Excel) to handle large elections

---

## Source of Truth

Last updated: 2026-06-20 via SDD changes `censo-electoral`, `modulo-resultados-y-reportes`
