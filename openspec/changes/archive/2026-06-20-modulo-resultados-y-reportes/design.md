# Design: Results and Reporting Module (`modulo-resultados-y-reportes`)

## Technical Approach

Add two read-only capabilities (`election-results`, `election-reports`) inside the existing `electoral` bounded context using **live aggregation** from canonical tables (`votos`, `participacion_electoral`, `censo_electoral`, `candidatos`). No result snapshot table in MVP. Two new input ports drive two new use cases; one new read-only output port supplies aggregated counts via a single grouped SQL query. A REST adapter exposes chart-ready JSON and streamed binary exports. Null votes become a synthetic candidate (`es_voto_nulo=true`), mirroring the existing blank-vote pattern. Strictly the newer `Election`/UUID path — no `Eleccion` legacy reuse.

## Architecture Decisions

| Decision | Choice | Alternatives rejected | Rationale |
|---|---|---|---|
| Compute strategy | Live aggregation on request | Snapshot on finalize (CQRS) | Smallest schema delta; admin read volume low; matches proposal |
| Null-vote storage | Synthetic candidate `es_voto_nulo=true` + V3 migration | New `votos` column; separate table | Reuses `votos.candidato_id` (NOT NULL); mirrors blank vote; zero cast-flow change |
| Aggregation query | One `GROUP BY candidato_id` count + separate participation/census counts | N+1 per-candidate counts | Single index-backed scan on `idx_votos_candidato`; O(candidates) result rows |
| Export generation | Stream to `OutputStream` via `ReportExporterPort` (PDF=OpenPDF, Excel=Apache POI SXSSF) | Buffer full byte[] in memory | Bounded memory on large elections; mitigates proposal risk |
| Port placement | `ResultsRepositoryPort` in `electoral/domain/port/out` | Extend write-only `VoteRepositoryPort` | Keeps voting context write-only; results is an electoral read concern |
| Finalized guard | Use case rejects non-`FINALIZADA` with domain exception | Allow live results | Proposal scopes results to finalized elections only |
| Export I/O boundary | Domain returns `ElectionResult`; exporter is output port, invoked by app service | Generate bytes in domain | Domain stays pure (no POI/OpenPDF imports) |

## Data Flow

    GET /results            ResultsController ─→ ResultsAppService ─→ GetElectionResultsUseCase
                                                                          │ (input port)
                                                                          ▼
                                          ElectionRepositoryPort (status=FINALIZADA guard)
                                          ResultsRepositoryPort.aggregate(eleccionId)
                                                                          │
                                                            ┌────────────┴─────────────┐
                                                       votos GROUP BY      participacion / censo counts
                                                                          ▼
                                                            ElectionResult (domain record)

    GET /report.pdf|xlsx    ResultsController ─→ ResultsAppService ─→ GetElectionResultsUseCase
                                                       │ ElectionResult
                                                       ▼
                                          ReportExporterPort.export(result, format, OutputStream)
                                                       │ streamed
                                                       ▼  StreamingResponseBody → HTTP download

## File Changes

| File | Action | Description |
|---|---|---|
| `db/migration/V3__Null_vote.sql` | Create | Add `es_voto_nulo BOOLEAN NOT NULL DEFAULT false` to `candidatos`; relax/extend blank-vote CHECK to permit null-vote name |
| `electoral/domain/model/ElectionResult.java` | Create | Record: election meta, `List<CandidateResult>`, blankVotes, nullVotes, totalVotes, eligibleCount, participationCount, abstentions |
| `electoral/domain/model/CandidateResult.java` | Create | Record: candidateId, nombre, votes, percentage |
| `electoral/domain/port/in/GetElectionResultsUseCase.java` | Create | Input port: `ElectionResult getResults(UUID eleccionId)` |
| `electoral/domain/port/in/GenerateElectionReportUseCase.java` | Create | Input port: streams report for a format |
| `electoral/domain/port/out/ResultsRepositoryPort.java` | Create | Read-only: `aggregate(UUID)` returns raw per-candidate counts + totals |
| `electoral/domain/port/out/ReportExporterPort.java` | Create | `export(ElectionResult, ReportFormat, OutputStream)` — pure signature, no infra types |
| `electoral/domain/usecase/GetElectionResultsUseCaseImpl.java` | Create | Loads election, guards FINALIZADA, aggregates, computes percentages/abstentions |
| `electoral/domain/usecase/GenerateElectionReportUseCaseImpl.java` | Create | Delegates to results use case + exporter port |
| `electoral/domain/exception/ElectionNotFinalizedException.java` | Create | Domain exception → HTTP 409 |
| `electoral/application/service/ResultsAppService.java` | Create | Orchestration; opens stream, sets watermark metadata |
| `electoral/application/dto/ElectionResultsResponse.java` | Create | Chart-ready JSON projection (records) |
| `electoral/infrastructure/adapter/in/web/ResultsController.java` | Create | `GET /api/v1/elections/{id}/results`, `/results/report.pdf`, `/results/report.xlsx` |
| `electoral/infrastructure/adapter/out/persistence/ResultsRepositoryAdapter.java` | Create | Native/grouped JPA query; maps rows → domain |
| `electoral/infrastructure/adapter/out/report/OpenPdfReportExporter.java` | Create | Implements `ReportExporterPort`; streamed PDF + watermark |
| `electoral/infrastructure/adapter/out/report/PoiExcelReportExporter.java` | Create | SXSSF streamed Excel + watermark sheet metadata |
| `config/DomainConfig.java` | Modify | Wire the two new use cases (manual beans) |
| `config/ReportConfig.java` | Create | Wire exporter beans + `ReportProperties` (watermark text) |
| `pom.xml` | Modify | Add OpenPDF + Apache POI (poi-ooxml) |
| `ElectionActivation` flow (Activate use case) | Modify | On activation, also create synthetic null-vote candidate (mirrors blank-vote creation) |

## Interfaces / Contracts

```java
// domain/port/out — pure, read-only
public interface ResultsRepositoryPort {
    ResultAggregate aggregate(UUID eleccionId); // candidate counts + totals, single query
}
public record ResultAggregate(
    List<CandidateCount> candidateCounts, long blankVotes, long nullVotes,
    long totalVotes, long eligibleCount, long participationCount) {}

// domain/port/out — streamed export, no infra types in signature
public interface ReportExporterPort {
    void export(ElectionResult result, ReportFormat format, OutputStream out);
}
public enum ReportFormat { PDF, XLSX }
```

Backend/frontend contract boundary: backend owns aggregation + percentages + binary generation; frontend (separate repo, out of scope) consumes `ElectionResultsResponse` JSON for charts and triggers binary downloads via the two report endpoints. Percentages are computed server-side so chart and report agree.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit (domain) | `GetElectionResultsUseCaseImpl`: FINALIZADA guard, percentage math, abstention = eligible − participation, null/blank classification | `@ExtendWith(MockitoExtension)`, mock `ElectionRepositoryPort` + `ResultsRepositoryPort` only |
| Unit (domain) | `GenerateElectionReportUseCaseImpl` delegates and passes stream through | Mock `ReportExporterPort`, verify interaction |
| Unit (adapter) | Exporters write non-empty stream, correct content-type bytes, watermark present | Plain JUnit, ByteArrayOutputStream |
| Slice | `ResultsController` JSON shape + 409 on non-finalized + content-disposition headers | `@WebMvcTest`, mock app service |
| Integration (`*IT`) | Real aggregation SQL on seeded finalized election (valid+blank+null votes) returns correct counts | `@SpringBootTest` + Testcontainers (Postgres) |

## Performance / Consistency

- Finalized elections are immutable (no new votes), so live aggregation is **consistent without locking** — read-after-finalize is stable. No snapshot needed.
- Aggregation is a single `GROUP BY candidato_id` over `votos` using `idx_votos_candidato`; participation/census are indexed counts. Cost O(votes) scan, acceptable for low admin read volume.
- Exports stream via `OutputStream`/SXSSF so memory stays bounded regardless of election size (mitigates proposal "memory overhead on large exports" risk).
- Virtual threads (already enabled) keep blocking export I/O cheap; no reactive needed.

## Migration / Rollout

V3 migration is additive (`es_voto_nulo` defaults false) — backward compatible, no backfill. Existing finalized elections without a null-vote candidate simply report nullVotes=0. Read-only endpoints; clean revert disables them without touching canonical data.

## Open Questions

- [ ] Should null-vote synthetic candidates be backfilled for already-FINALIZADA elections, or only forward (new activations)? Forward-only assumed.
- [ ] Watermark exact text/branding source — config property assumed; confirm value with stakeholders.
- [ ] Is a per-candidate tie/winner flag required in the JSON, or does the frontend derive it? Server-side `isWinner` assumed safer for report/chart consistency.