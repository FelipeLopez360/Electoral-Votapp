package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.electoral.application.dto.ElectionResultsResponse;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.in.GenerateElectionReportUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetElectionResultsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.UUID;

/**
 * Application service for election results and report generation.
 *
 * <p>Thin orchestration layer between the REST adapter and the domain use cases.
 * Handles DTO mapping, provides a Spring-managed boundary ({@code @Service}),
 * and returns {@link StreamingResponseBody} for binary exports so the controller
 * remains free of I/O concerns.
 *
 * <p>Does NOT contain business logic — all rules live in the use case layer.
 */
@Service
public class ResultsAppService {

    private final GetElectionResultsUseCase getElectionResultsUseCase;
    private final GenerateElectionReportUseCase generateElectionReportUseCase;

    public ResultsAppService(GetElectionResultsUseCase getElectionResultsUseCase,
                             GenerateElectionReportUseCase generateElectionReportUseCase) {
        this.getElectionResultsUseCase = getElectionResultsUseCase;
        this.generateElectionReportUseCase = generateElectionReportUseCase;
    }

    /**
     * Retrieve chart-ready aggregated results for a finalized election.
     *
     * @param electionId the election's UUID
     * @return a {@link ElectionResultsResponse} ready for JSON serialization
     */
    public ElectionResultsResponse getResults(UUID electionId) {
        ElectionResult result = getElectionResultsUseCase.getResults(electionId);
        return ElectionResultsResponse.from(result);
    }

    /**
     * Generate a binary report for a finalized election.
     *
     * <p>Returns a {@link StreamingResponseBody} that the controller can pass directly
     * to Spring MVC, which streams the output without buffering in memory.
     *
     * @param electionId the election's UUID
     * @param format     the desired report format ({@link ReportFormat#PDF} or {@link ReportFormat#XLSX})
     * @return a lazy streaming body ready for HTTP response writing
     */
    public StreamingResponseBody generateReport(UUID electionId, ReportFormat format) {
        return outputStream -> generateElectionReportUseCase.generateReport(electionId, format, outputStream);
    }
}
