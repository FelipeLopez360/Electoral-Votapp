package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.electoral.application.dto.ElectionResultsResponse;
import co.com.votapp.ws.electoral.application.service.ResultsAppService;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.UUID;

/**
 * REST adapter for election results and binary report downloads.
 *
 * <p>Provides read-only endpoints for finalized election aggregation.
 * All requests require HTTP Basic auth (admin chain).
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /api/v1/elections/{id}/results} — chart-ready JSON</li>
 *   <li>{@code GET /api/v1/elections/{id}/results/report.pdf} — PDF binary stream</li>
 *   <li>{@code GET /api/v1/elections/{id}/results/report.xlsx} — Excel binary stream</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/elections")
@Tag(name = "Election Results", description = "Read-only aggregated results for finalized elections")
public class ResultsController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ResultsAppService resultsAppService;

    public ResultsController(ResultsAppService resultsAppService) {
        this.resultsAppService = resultsAppService;
    }

    /**
     * Get chart-ready aggregated results for a finalized election.
     *
     * <p>Returns per-candidate vote counts, percentages, winner flags,
     * and participation statistics for the specified election.
     * Only FINALIZADA elections are permitted — any other state returns HTTP 409.
     *
     * @param id the election UUID
     * @return 200 + JSON results, 404 if not found, 409 if not finalized, 401 if unauthenticated
     */
    @GetMapping(value = "/{id}/results", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get aggregated election results",
            description = "Returns chart-ready statistics for a finalized election. "
                    + "Requires FINALIZADA state. Requires admin credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aggregated results returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election not found"),
            @ApiResponse(responseCode = "409", description = "Election is not FINALIZADA")
    })
    public ResponseEntity<ElectionResultsResponse> getResults(@PathVariable UUID id) {
        ElectionResultsResponse response = resultsAppService.getResults(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Download a PDF report for a finalized election.
     *
     * <p>Streams the generated PDF binary directly to the HTTP response without buffering
     * in heap memory. Virtual threads keep blocking I/O cheap.
     *
     * @param id the election UUID
     * @return 200 + PDF stream, 409 if not finalized, 404 if not found, 401 if unauthenticated
     */
    @GetMapping(value = "/{id}/results/report.pdf", produces = "application/pdf")
    @Operation(
            summary = "Download PDF election report",
            description = "Generates and streams a PDF report for a finalized election. "
                    + "Requires FINALIZADA state. Requires admin credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF report streamed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election not found"),
            @ApiResponse(responseCode = "409", description = "Election is not FINALIZADA")
    })
    public ResponseEntity<StreamingResponseBody> getReportPdf(@PathVariable UUID id) {
        StreamingResponseBody body = resultsAppService.generateReport(id, ReportFormat.PDF);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("election-" + id + "-report.pdf")
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }

    /**
     * Download an Excel report for a finalized election.
     *
     * <p>Streams the generated XLSX binary directly to the HTTP response using
     * Apache POI SXSSF (Streaming API) to keep memory bounded.
     *
     * @param id the election UUID
     * @return 200 + XLSX stream, 409 if not finalized, 404 if not found, 401 if unauthenticated
     */
    @GetMapping(value = "/{id}/results/report.xlsx",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(
            summary = "Download Excel election report",
            description = "Generates and streams an Excel report for a finalized election. "
                    + "Requires FINALIZADA state. Requires admin credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Excel report streamed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Election not found"),
            @ApiResponse(responseCode = "409", description = "Election is not FINALIZADA")
    })
    public ResponseEntity<StreamingResponseBody> getReportXlsx(@PathVariable UUID id) {
        StreamingResponseBody body = resultsAppService.generateReport(id, ReportFormat.XLSX);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("election-" + id + "-report.xlsx")
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(XLSX_MEDIA_TYPE)
                .body(body);
    }
}
