# Proposal: modulo-resultados-y-reportes

## Intent
Provide a comprehensive results and reporting module for finalized elections. This allows administrators to select a finalized election, view aggregated chart-ready statistics (participation, candidate performance), and download secure institutional PDF/Excel reports.

## Scope

### In Scope
- Read-model aggregation of votes, participation, and census for finalized elections.
- REST endpoints providing chart-ready JSON payloads for frontend consumption.
- PDF and Excel export generation services with institutional watermarking.
- Explicit definition of Null Vote semantics at the domain level.
- Core logic contained purely within Hexagonal boundaries (`domain/`, `application/`, `adapter/in/web/`, `adapter/out/persistence/`).

### Out of Scope
- Frontend implementation (Admin charts, result tabs, and export UX). *Note: This proposal relies on a corresponding frontend change.*
- Results calculation for non-finalized (`ACTIVA`, `PROGRAMADA`) elections.
- Legacy `Eleccion` path integration (all new features strictly use the new `Election` contexts).

## Capabilities

### New Capabilities
- `election-results`: Read-only aggregation of election outcomes (candidate vote counts, blank votes, null votes, total participation).
- `election-reports`: Generation and download of secure PDF/Excel documents with watermarks.

### Modified Capabilities
- None

## Approach

**Decision Point 1: Result Computation Strategy**
We propose **Live Aggregation** (on-demand compute from `votos`, `participacion_electoral`, and `censo_electoral`).
- *Why*: Smallest schema change, uses existing canonical data sources, and fits MVP needs perfectly before scaling up to a CQRS snapshot model.
- *Tradeoff*: Computes stats on every admin request, but admin read volume is low.

**Decision Point 2: Null-Vote Semantics**
We propose treating Null Votes as a synthetic candidate (e.g., `es_voto_nulo=true`), mirroring the existing Blank Vote pattern.
- *Why*: Reuses the existing `votos` table structure without requiring a schema migration or complex type hierarchies.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `adapter/in/web/` | New | Endpoints for chart JSON and binary export downloads. |
| `domain/port/in/` | New | `GetElectionResultsUseCase`, `GenerateElectionReportUseCase`. |
| `domain/model/` | Modified | Add Null Vote concept to Domain. |
| `adapter/out/persistence/` | Modified | Add read-only queries for aggregation without mutating data. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Memory overhead on large exports | Medium | Stream report generation instead of buffering full files in memory. |
| Export libraries bloating JAR | Low | Select lightweight, standard libraries (e.g., Apache POI, OpenPDF). |

## Rollback Plan
Since this is purely a read/reporting extension, reverting the PR/deployment cleanly disables the endpoints without corrupting canonical election or vote data.

## Dependencies
- Frontend implementation for Admin charts and download UX.
- PDF generation library (e.g., OpenPDF/iText).
- Excel generation library (e.g., Apache POI).

## Success Criteria
- [ ] Admin can retrieve aggregated chart-ready statistics for a finalized election.
- [ ] Admin can download a watermarked PDF and Excel report.
- [ ] System properly categorizes valid, blank, and null votes.
- [ ] All code adheres to Spring Boot Hexagonal strict boundaries (no framework code in `domain/`).