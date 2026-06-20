package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.in.GenerateElectionReportUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetElectionResultsUseCase;
import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;

import java.io.OutputStream;
import java.util.UUID;

/**
 * Use case implementation: generate and stream a binary report for a finalized election.
 *
 * <p>Delegates result aggregation to {@link GetElectionResultsUseCase} (which handles the
 * FINALIZADA guard and percentage math) and then hands the domain result to
 * {@link ReportExporterPort} for format-specific generation.
 *
 * <p>This use case intentionally contains NO format-specific logic. The domain stays pure:
 * no OpenPDF or Apache POI imports here.
 *
 * <p>Pure Java — ZERO Spring or framework imports. Wired manually in ReportConfig.
 */
public class GenerateElectionReportUseCaseImpl implements GenerateElectionReportUseCase {

    private final GetElectionResultsUseCase getElectionResultsUseCase;
    private final ReportExporterPort reportExporterPort;

    public GenerateElectionReportUseCaseImpl(GetElectionResultsUseCase getElectionResultsUseCase,
                                              ReportExporterPort reportExporterPort) {
        this.getElectionResultsUseCase = getElectionResultsUseCase;
        this.reportExporterPort = reportExporterPort;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the aggregated results (delegates FINALIZADA guard to
     * {@link GetElectionResultsUseCase}) and streams the binary output to the provided
     * {@link OutputStream} via the format-specific {@link ReportExporterPort}.
     */
    @Override
    public void generateReport(UUID eleccionId, ReportFormat format, OutputStream out) {
        ElectionResult result = getElectionResultsUseCase.getResults(eleccionId);
        reportExporterPort.export(result, format, out);
    }
}
