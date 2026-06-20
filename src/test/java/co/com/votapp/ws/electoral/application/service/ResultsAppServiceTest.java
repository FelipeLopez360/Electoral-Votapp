package co.com.votapp.ws.electoral.application.service;

import co.com.votapp.ws.electoral.application.dto.ElectionResultsResponse;
import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.in.GenerateElectionReportUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetElectionResultsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ResultsAppService - Orchestration and DTO mapping")
@ExtendWith(MockitoExtension.class)
class ResultsAppServiceTest {

    @Mock
    private GetElectionResultsUseCase getElectionResultsUseCase;

    @Mock
    private GenerateElectionReportUseCase generateElectionReportUseCase;

    private ResultsAppService service;

    @BeforeEach
    void setUp() {
        service = new ResultsAppService(getElectionResultsUseCase, generateElectionReportUseCase);
    }

    @Test
    @DisplayName("Should delegate to use case and return mapped response")
    void getResults_shouldDelegateToUseCaseAndReturnResponse() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        CandidateResult candidateResult = new CandidateResult(
                candidateId, "Candidate A", 70L, 70.0, false, false, true
        );
        ElectionResult domainResult = new ElectionResult(
                electionId, "Eleccion Test", List.of(candidateResult),
                20L, 10L, 100L, 200L, 150L
        );
        when(getElectionResultsUseCase.getResults(electionId)).thenReturn(domainResult);

        // When
        ElectionResultsResponse response = service.getResults(electionId);

        // Then
        verify(getElectionResultsUseCase).getResults(electionId);
        assertThat(response.electionId()).isEqualTo(electionId.toString());
        assertThat(response.electionName()).isEqualTo("Eleccion Test");
        assertThat(response.totalVotes()).isEqualTo(100L);
        assertThat(response.blankVotes()).isEqualTo(20L);
        assertThat(response.nullVotes()).isEqualTo(10L);
        assertThat(response.eligibleCount()).isEqualTo(200L);
        assertThat(response.participationCount()).isEqualTo(150L);
        assertThat(response.abstentions()).isEqualTo(50L);
        assertThat(response.participationRate()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Should map candidate results including winner flag and percentages")
    void getResults_shouldMapCandidateResultsCorrectly() {
        // Given
        UUID electionId  = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();

        CandidateResult candidateResult = new CandidateResult(
                candidateId, "Candidate A", 70L, 70.0, false, false, true
        );
        ElectionResult domainResult = new ElectionResult(
                electionId, "Test", List.of(candidateResult),
                0L, 0L, 70L, 100L, 70L
        );
        when(getElectionResultsUseCase.getResults(electionId)).thenReturn(domainResult);

        // When
        ElectionResultsResponse response = service.getResults(electionId);

        // Then
        assertThat(response.candidates()).hasSize(1);
        ElectionResultsResponse.CandidateResultEntry entry = response.candidates().getFirst();
        assertThat(entry.candidateId()).isEqualTo(candidateId.toString());
        assertThat(entry.nombre()).isEqualTo("Candidate A");
        assertThat(entry.votes()).isEqualTo(70L);
        assertThat(entry.percentage()).isEqualTo(70.0);
        assertThat(entry.esVotoEnBlanco()).isFalse();
        assertThat(entry.esVotoNulo()).isFalse();
        assertThat(entry.isWinner()).isTrue();
    }

    @Test
    @DisplayName("Should delegate to GenerateElectionReportUseCase and return StreamingResponseBody for PDF")
    void generateReport_shouldDelegateToUseCaseAndReturnStreamingBody_whenPdfRequested() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();
        doNothing().when(generateElectionReportUseCase)
                .generateReport(eq(electionId), eq(ReportFormat.PDF), any(OutputStream.class));

        // When
        StreamingResponseBody body = service.generateReport(electionId, ReportFormat.PDF);

        // Then — streaming body is not null and is callable
        assertThat(body).isNotNull();
        // Execute the stream to verify delegation
        body.writeTo(new ByteArrayOutputStream());
        verify(generateElectionReportUseCase).generateReport(eq(electionId), eq(ReportFormat.PDF), any(OutputStream.class));
    }

    @Test
    @DisplayName("Should delegate to GenerateElectionReportUseCase and return StreamingResponseBody for XLSX")
    void generateReport_shouldDelegateToUseCaseAndReturnStreamingBody_whenXlsxRequested() throws Exception {
        // Given
        UUID electionId = UUID.randomUUID();
        doNothing().when(generateElectionReportUseCase)
                .generateReport(eq(electionId), eq(ReportFormat.XLSX), any(OutputStream.class));

        // When
        StreamingResponseBody body = service.generateReport(electionId, ReportFormat.XLSX);

        // Then
        assertThat(body).isNotNull();
        body.writeTo(new ByteArrayOutputStream());
        verify(generateElectionReportUseCase).generateReport(eq(electionId), eq(ReportFormat.XLSX), any(OutputStream.class));
    }
}
