package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.NotFoundException;
import co.com.votapp.ws.electoral.domain.exception.ElectionNotFinalizedException;
import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.in.GenerateElectionReportUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetElectionResultsUseCase;
import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GenerateElectionReportUseCaseImpl - Report generation business logic")
@ExtendWith(MockitoExtension.class)
class GenerateElectionReportUseCaseImplTest {

    @Mock
    private GetElectionResultsUseCase getElectionResultsUseCase;

    @Mock
    private ReportExporterPort reportExporterPort;

    private GenerateElectionReportUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GenerateElectionReportUseCaseImpl(getElectionResultsUseCase, reportExporterPort);
    }

    @Test
    @DisplayName("Should delegate to results use case and exporter port for PDF format")
    void generateReport_shouldDelegateToResultsAndExporter_whenPdfRequested() {
        // Given
        UUID electionId = UUID.randomUUID();
        ElectionResult result = sampleResult(electionId);
        OutputStream out = new ByteArrayOutputStream();
        when(getElectionResultsUseCase.getResults(electionId)).thenReturn(result);

        // When
        useCase.generateReport(electionId, ReportFormat.PDF, out);

        // Then
        verify(getElectionResultsUseCase).getResults(electionId);
        verify(reportExporterPort).export(result, ReportFormat.PDF, out);
    }

    @Test
    @DisplayName("Should delegate to results use case and exporter port for XLSX format")
    void generateReport_shouldDelegateToResultsAndExporter_whenXlsxRequested() {
        // Given
        UUID electionId = UUID.randomUUID();
        ElectionResult result = sampleResult(electionId);
        OutputStream out = new ByteArrayOutputStream();
        when(getElectionResultsUseCase.getResults(electionId)).thenReturn(result);

        // When
        useCase.generateReport(electionId, ReportFormat.XLSX, out);

        // Then
        verify(getElectionResultsUseCase).getResults(electionId);
        verify(reportExporterPort).export(result, ReportFormat.XLSX, out);
    }

    @Test
    @DisplayName("Should pass exact ElectionResult instance from results use case to exporter")
    void generateReport_shouldPassExactResultInstance_toExporter() {
        // Given
        UUID electionId = UUID.randomUUID();
        ElectionResult result = sampleResult(electionId);
        OutputStream out = new ByteArrayOutputStream();
        when(getElectionResultsUseCase.getResults(electionId)).thenReturn(result);

        // When
        useCase.generateReport(electionId, ReportFormat.PDF, out);

        // Then
        ArgumentCaptor<ElectionResult> resultCaptor = ArgumentCaptor.forClass(ElectionResult.class);
        verify(reportExporterPort).export(resultCaptor.capture(), eq(ReportFormat.PDF), eq(out));
        assertThat(resultCaptor.getValue()).isSameAs(result);
    }

    @Test
    @DisplayName("Should propagate NotFoundException when election does not exist")
    void generateReport_shouldPropagateNotFoundException_whenElectionNotFound() {
        // Given
        UUID electionId = UUID.randomUUID();
        OutputStream out = new ByteArrayOutputStream();
        when(getElectionResultsUseCase.getResults(electionId))
                .thenThrow(new NotFoundException("Election not found: " + electionId));

        // When & Then
        assertThatThrownBy(() -> useCase.generateReport(electionId, ReportFormat.PDF, out))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(electionId.toString());
    }

    @Test
    @DisplayName("Should propagate ElectionNotFinalizedException when election is not FINALIZADA")
    void generateReport_shouldPropagateElectionNotFinalizedException_whenNotFinalized() {
        // Given
        UUID electionId = UUID.randomUUID();
        OutputStream out = new ByteArrayOutputStream();
        when(getElectionResultsUseCase.getResults(electionId))
                .thenThrow(new ElectionNotFinalizedException(electionId));

        // When & Then
        assertThatThrownBy(() -> useCase.generateReport(electionId, ReportFormat.XLSX, out))
                .isInstanceOf(ElectionNotFinalizedException.class);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ElectionResult sampleResult(UUID electionId) {
        CandidateResult candidate = new CandidateResult(
                UUID.randomUUID(), "Candidate A", 50L, 50.0, false, false, true);
        return new ElectionResult(electionId, "Test Election",
                List.of(candidate), 10L, 5L, 65L, 100L, 65L);
    }
}
