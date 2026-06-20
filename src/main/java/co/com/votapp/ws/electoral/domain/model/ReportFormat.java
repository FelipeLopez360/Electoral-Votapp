package co.com.votapp.ws.electoral.domain.model;

/**
 * Supported election report export formats.
 *
 * <p>Used by {@code ReportExporterPort} and {@code GenerateElectionReportUseCase}
 * to select the appropriate generation strategy.
 *
 * <p>Pure Java — ZERO framework imports.
 */
public enum ReportFormat {
    PDF,
    XLSX
}
