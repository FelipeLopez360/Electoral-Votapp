# Tasks: modulo-resultados-y-reportes

## Review Workload Forecast

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

Estimated changed lines: 600-800
Suggested split:
- **PR 1**: DB Migration (V3 Null Vote), POM updates, Domain models, and Ports.
- **PR 2**: Core Aggregation (Persistence Adapter, Use Case, App Service, JSON REST endpoint) + Null Vote Activation flow.
- **PR 3**: Export capabilities (PDF/Excel Adapters, Report Use Case, Binary REST endpoint, ReportConfig).

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Infrastructure & Domain Foundation | PR 1 | Base branch: feature/modulo-resultados. Safe purely additive models and DB migration. |
| 2 | Live Aggregation & API | PR 2 | Base branch: PR 1 branch. Implements JSON results API and updates activation flow to spawn synthetic null vote. |
| 3 | Exporters & Binary Streams | PR 3 | Base branch: PR 2 branch. Adds OpenPDF and POI logic, completing report endpoints. |

## Phase 1: Foundation (Migration, Models & Ports)

- [x] 1.1 `pom.xml`: Add OpenPDF and Apache POI (`poi-ooxml`) dependencies.
- [x] 1.2 `db/migration/V3__Null_vote.sql`: Add `es_voto_nulo BOOLEAN NOT NULL DEFAULT false` to `candidatos` and update the constraint to allow null vote names.
- [x] 1.3 `electoral/domain/model/`: Create `CandidateResult.java` and `ElectionResult.java` records for stats and percentages. Also added `ResultAggregate.java`, `ReportFormat.java`, and extended `Candidate.java` with `esVotoNulo` field.
- [x] 1.4 `electoral/domain/port/out/`: Create `ResultsRepositoryPort.java` with `aggregate(UUID)` and `ReportExporterPort.java`.
- [x] 1.5 `electoral/domain/port/in/`: Create `GetElectionResultsUseCase.java` and `GenerateElectionReportUseCase.java`.
- [x] 1.6 `electoral/domain/exception/`: Create `ElectionNotFinalizedException.java`.

## Phase 2: Core Aggregation & JSON API

- [x] 2.1 `ResultsRepositoryAdapter.java`: Implement `ResultsRepositoryPort.aggregate` via native grouped JPA query.
- [x] 2.2 `GetElectionResultsUseCaseImpl.java`: Enforce `FINALIZADA` state, calculate percentages, and determine winners. Add unit tests.
- [x] 2.3 `config/DomainConfig.java`: Wire `GetElectionResultsUseCaseImpl` as a manual bean.
- [x] 2.4 `electoral/application/`: Create `ElectionResultsResponse.java` DTO and `ResultsAppService.java` orchestration logic.
- [x] 2.5 `ResultsController.java`: Add `GET /api/v1/elections/{id}/results` returning JSON. Add `@WebMvcTest`.

## Phase 3: Null Vote Activation Flow

- [x] 3.1 `ActivateElectionUseCaseImpl.java`: Update election activation to also create a synthetic null-vote candidate (`es_voto_nulo=true`) alongside the blank vote.
- [x] 3.2 Update `ActivateElectionUseCaseImplTest` to verify null-vote creation.

## Phase 4: Exporters & Binary Reports

- [x] 4.1 `GenerateElectionReportUseCaseImpl.java`: Implement use case delegating to results use case and exporter port. Add tests.
- [x] 4.2 `OpenPdfReportExporter.java`: Implement PDF generation using OpenPDF with watermark logic.
- [x] 4.3 `PoiExcelReportExporter.java`: Implement Excel generation using Apache POI SXSSF.
- [x] 4.4 `config/ReportConfig.java`: Wire exporters and `ReportProperties` (for watermark text).
- [x] 4.5 `ResultsAppService.java`: Add orchestration for report streaming.
- [x] 4.6 `ResultsController.java`: Add `GET /api/v1/elections/{id}/results/report.pdf` and `report.xlsx` returning `StreamingResponseBody`.
- [x] 4.7 Update `config/DomainConfig.java`: Wire `GenerateElectionReportUseCaseImpl`.

## Phase 5: Integration & Cleanup

- [x] 5.1 `ResultsRepositoryAdapterIT.java`: Add `@SpringBootTest` Testcontainers test verifying native aggregation SQL.