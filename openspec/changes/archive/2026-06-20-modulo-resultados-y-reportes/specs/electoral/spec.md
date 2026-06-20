# Results and Reporting Specification

## Purpose
Define the backend requirements for calculating, aggregating, and exporting results for finalized elections. This relies on live aggregation of existing data and introduces a synthetic Null Vote concept.

*Note: Frontend implementation (admin charts, tabs, download UX) is an external dependency.*

## New Capabilities

### Domain: Election Results

#### Requirement: Finalized Election Gating
The system MUST ONLY provide results and statistics for elections in the `FINALIZADA` state.

- GIVEN an election that is `PROGRAMADA` or `ACTIVA`
- WHEN an admin requests the election results
- THEN the system MUST reject the request with a state violation error

#### Requirement: Null Vote Representation
The system MUST support null votes using a synthetic candidate approach.

- GIVEN an election
- WHEN it is activated
- THEN the system MUST ensure a synthetic candidate for null votes exists alongside the blank vote candidate

#### Requirement: Chart-Ready Aggregation
The system MUST provide an aggregated, read-only payload containing candidate vote counts, blank votes, and null votes.

- GIVEN a `FINALIZADA` election
- WHEN an admin requests the results
- THEN the system MUST calculate live vote counts from the `votos` table
- AND return a structured JSON response mapping each candidate (including blank and null) to their total votes

#### Requirement: Participation Statistics
The system MUST provide total participation statistics based on the census.

- GIVEN a `FINALIZADA` election
- WHEN an admin requests the results
- THEN the system MUST count total eligible voters from `censo_electoral`
- AND count total participants from `participacion_electoral`
- AND return the participation count and rate as a percentage

#### Requirement: Winner Visibility
The system MUST identify the winner(s) of the election in the results payload.

- GIVEN a `FINALIZADA` election with no ties
- WHEN the results are aggregated
- THEN the system MUST flag the candidate with the highest valid votes as the winner
- AND MUST flag multiple candidates as winners if there is an exact tie

### Domain: Election Reports

#### Requirement: Report Export Contracts
The system MUST provide endpoints to generate and download election reports in both PDF and Excel formats.

- GIVEN a `FINALIZADA` election
- WHEN an admin requests a PDF or Excel export
- THEN the system MUST generate a binary file containing the aggregated results and participation stats
- AND the file MUST include a secure institutional watermark
- AND return the binary stream with the correct MIME type

#### Requirement: Export Stream Handling
The system SHOULD stream large report generation to avoid excessive memory consumption.

- GIVEN a request for an election report
- WHEN the system builds the PDF or Excel file
- THEN it SHOULD stream the binary output directly to the HTTP response