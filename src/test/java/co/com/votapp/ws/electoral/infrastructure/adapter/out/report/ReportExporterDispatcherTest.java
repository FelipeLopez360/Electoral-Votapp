package co.com.votapp.ws.electoral.infrastructure.adapter.out.report;

import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("ReportExporterDispatcher - Format-based routing")
@ExtendWith(MockitoExtension.class)
class ReportExporterDispatcherTest {

    @Mock
    private ReportExporterPort pdfExporter;

    @Mock
    private ReportExporterPort xlsxExporter;

    private ReportExporterDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new ReportExporterDispatcher(pdfExporter, xlsxExporter);
    }

    @Test
    @DisplayName("Should delegate to PDF exporter when format is PDF")
    void export_shouldDelegateToPdfExporter_whenFormatIsPdf() {
        // Given
        ElectionResult result = sampleResult();
        OutputStream out = new ByteArrayOutputStream();

        // When
        dispatcher.export(result, ReportFormat.PDF, out);

        // Then
        verify(pdfExporter).export(result, ReportFormat.PDF, out);
        verifyNoInteractions(xlsxExporter);
    }

    @Test
    @DisplayName("Should delegate to XLSX exporter when format is XLSX")
    void export_shouldDelegateToXlsxExporter_whenFormatIsXlsx() {
        // Given
        ElectionResult result = sampleResult();
        OutputStream out = new ByteArrayOutputStream();

        // When
        dispatcher.export(result, ReportFormat.XLSX, out);

        // Then
        verify(xlsxExporter).export(result, ReportFormat.XLSX, out);
        verifyNoInteractions(pdfExporter);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ElectionResult sampleResult() {
        CandidateResult candidate = new CandidateResult(
                UUID.randomUUID(), "Candidate A", 50L, 50.0, false, false, true);
        return new ElectionResult(UUID.randomUUID(), "Test Election",
                List.of(candidate), 10L, 5L, 65L, 100L, 65L);
    }
}
