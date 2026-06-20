package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.common.config.SecurityConfig;
import co.com.votapp.ws.common.exception.GlobalExceptionHandler;
import co.com.votapp.ws.common.exception.NotFoundException;
import co.com.votapp.ws.electoral.application.dto.ElectionResultsResponse;
import co.com.votapp.ws.electoral.application.service.ResultsAppService;
import co.com.votapp.ws.electoral.domain.exception.ElectionNotFinalizedException;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC slice tests for {@link ResultsController}.
 *
 * <p>Verifies HTTP status codes, JSON shape, and exception-to-status mapping
 * via the real {@link GlobalExceptionHandler}.
 *
 * <p>Named *WebMvcTest.java (not *IT.java) — runs under surefire, no Testcontainers needed.
 */
@DisplayName("ResultsController - GET /api/v1/elections/{id}/results (WebMvcTest)")
@WebMvcTest(ResultsController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ResultsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResultsAppService resultsAppService;

    // Required by SecurityConfig.portalAuthFilter bean
    @MockitoBean
    private PortalSessionPort portalSessionPort;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 with chart-ready JSON for a finalized election")
    void getResults_shouldReturn200WithJson_whenElectionIsFinalized() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();
        ElectionResultsResponse response = buildSampleResponse(electionId);
        when(resultsAppService.getResults(any(UUID.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/elections/{id}/results", electionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.electionId").value(electionId.toString()))
                .andExpect(jsonPath("$.electionName").value("Test Election"))
                .andExpect(jsonPath("$.totalVotes").value(100))
                .andExpect(jsonPath("$.blankVotes").value(10))
                .andExpect(jsonPath("$.nullVotes").value(5))
                .andExpect(jsonPath("$.eligibleCount").value(200))
                .andExpect(jsonPath("$.participationCount").value(100))
                .andExpect(jsonPath("$.abstentions").value(100))
                .andExpect(jsonPath("$.participationRate").value(50.0))
                .andExpect(jsonPath("$.candidates").isArray())
                .andExpect(jsonPath("$.candidates[0].nombre").value("Candidate A"))
                .andExpect(jsonPath("$.candidates[0].votes").value(85))
                .andExpect(jsonPath("$.candidates[0].isWinner").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 409 when election is not FINALIZADA")
    void getResults_shouldReturn409_whenElectionIsNotFinalized() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();
        when(resultsAppService.getResults(any(UUID.class)))
                .thenThrow(new ElectionNotFinalizedException(electionId));

        // When & Then — GlobalExceptionHandler must map → 409
        mockMvc.perform(get("/api/v1/elections/{id}/results", electionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when election does not exist")
    void getResults_shouldReturn404_whenElectionNotFound() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();
        when(resultsAppService.getResults(any(UUID.class)))
                .thenThrow(new NotFoundException("Election not found: " + electionId));

        // When & Then
        mockMvc.perform(get("/api/v1/elections/{id}/results", electionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return 401 when no authentication provided")
    void getResults_shouldReturn401_whenNotAuthenticated() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();

        // When & Then — no @WithMockUser
        mockMvc.perform(get("/api/v1/elections/{id}/results", electionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ─── Binary report endpoint tests ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 with PDF content-type and disposition header for PDF report")
    void getReportPdf_shouldReturn200WithPdfContentType_whenElectionIsFinalized() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();
        StreamingResponseBody body = out -> out.write(new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF
        when(resultsAppService.generateReport(any(UUID.class), eq(ReportFormat.PDF))).thenReturn(body);

        // When & Then
        mockMvc.perform(get("/api/v1/elections/{id}/results/report.pdf", electionId)
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString(".pdf")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 200 with XLSX content-type and disposition header for Excel report")
    void getReportXlsx_shouldReturn200WithXlsxContentType_whenElectionIsFinalized() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();
        StreamingResponseBody body = out -> out.write(new byte[]{0x50, 0x4B, 0x03, 0x04}); // XLSX ZIP magic
        when(resultsAppService.generateReport(any(UUID.class), eq(ReportFormat.XLSX))).thenReturn(body);

        // When & Then
        mockMvc.perform(get("/api/v1/elections/{id}/results/report.xlsx", electionId)
                        .accept("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString(".xlsx")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 409 when election is not FINALIZADA on PDF report request")
    void getReportPdf_shouldReturn409_whenElectionIsNotFinalized() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();
        when(resultsAppService.generateReport(any(UUID.class), eq(ReportFormat.PDF)))
                .thenThrow(new ElectionNotFinalizedException(electionId));

        // When & Then
        mockMvc.perform(get("/api/v1/elections/{id}/results/report.pdf", electionId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return 401 when no authentication provided for PDF report")
    void getReportPdf_shouldReturn401_whenNotAuthenticated() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();

        // When & Then — no @WithMockUser
        mockMvc.perform(get("/api/v1/elections/{id}/results/report.pdf", electionId))
                .andExpect(status().isUnauthorized());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static ElectionResultsResponse buildSampleResponse(UUID electionId) {
        UUID candidateId = UUID.randomUUID();
        ElectionResultsResponse.CandidateResultEntry candidate =
                new ElectionResultsResponse.CandidateResultEntry(
                        candidateId.toString(), "Candidate A", 85L, 85.0, false, false, true);

        return new ElectionResultsResponse(
                electionId.toString(),
                "Test Election",
                List.of(candidate),
                10L,   // blankVotes
                5L,    // nullVotes
                100L,  // totalVotes
                200L,  // eligibleCount
                100L,  // participationCount
                100L,  // abstentions
                50.0   // participationRate
        );
    }
}
