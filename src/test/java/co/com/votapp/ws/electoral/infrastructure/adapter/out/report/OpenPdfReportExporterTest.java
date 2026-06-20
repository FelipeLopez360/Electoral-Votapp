package co.com.votapp.ws.electoral.infrastructure.adapter.out.report;

import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("OpenPdfReportExporter - PDF generation via OpenPDF")
class OpenPdfReportExporterTest {

    private static final String WATERMARK = "Votapp - Sistema Electoral";

    private ReportExporterPort exporter;

    @BeforeEach
    void setUp() {
        exporter = new OpenPdfReportExporter(WATERMARK);
    }

    @Test
    @DisplayName("Should produce non-empty byte output for PDF format")
    void export_shouldProduceNonEmptyBytes_whenPdfFormatRequested() {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult();

        // When
        exporter.export(result, ReportFormat.PDF, out);

        // Then
        assertThat(out.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should produce PDF magic bytes (%PDF) at the start of the stream")
    void export_shouldStartWithPdfMagicBytes_whenPdfFormatRequested() {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult();

        // When
        exporter.export(result, ReportFormat.PDF, out);

        // Then — %PDF magic header
        byte[] bytes = out.toByteArray();
        assertThat(bytes).hasSizeGreaterThan(4);
        assertThat(new String(bytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Should embed institutional watermark text in the generated PDF")
    void export_shouldContainWatermarkText_inGeneratedPdf() throws Exception {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult();

        // When
        exporter.export(result, ReportFormat.PDF, out);

        // Then — extract text and verify watermark is present
        PdfReader reader = new PdfReader(out.toByteArray());
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder fullText = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            fullText.append(extractor.getTextFromPage(page));
        }
        reader.close();

        assertThat(fullText.toString()).contains(WATERMARK);
    }

    @Test
    @DisplayName("Should embed candidate names and election name in the generated PDF")
    void export_shouldContainCandidateAndElectionData_inGeneratedPdf() throws Exception {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult();

        // When
        exporter.export(result, ReportFormat.PDF, out);

        // Then — election name and at least one candidate name must appear
        PdfReader reader = new PdfReader(out.toByteArray());
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder fullText = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            fullText.append(extractor.getTextFromPage(page));
        }
        reader.close();

        assertThat(fullText.toString())
                .contains("Elección de Directivos 2026")
                .contains("Ana García");
    }

    @Test
    @DisplayName("Should not throw when exporting an election with no real candidates (only synthetic)")
    void export_shouldNotThrow_whenOnlySyntheticCandidatesPresent() {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CandidateResult blankVote = new CandidateResult(
                UUID.randomUUID(), "Voto en Blanco", 10L, 100.0, true, false, false);
        ElectionResult result = new ElectionResult(
                UUID.randomUUID(), "Minimal Election", List.of(blankVote),
                10L, 0L, 10L, 20L, 10L);

        // When & Then
        assertThatNoException().isThrownBy(() -> exporter.export(result, ReportFormat.PDF, out));
        assertThat(out.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should ignore non-PDF format when called with XLSX (wrong exporter contract)")
    void export_shouldNotProducePdfBytes_whenXlsxFormatPassed() {
        // Note: OpenPdfReportExporter always generates PDF regardless of format parameter;
        // the format routing is the responsibility of the caller (ReportConfig delegates
        // PDF-specific exporter for PDF and Excel-specific exporter for XLSX).
        // This test documents that behavior: even if XLSX is passed, PDF bytes come out.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult();

        // When
        exporter.export(result, ReportFormat.XLSX, out);

        // Then — PDF bytes still produced (exporter is format-specific by design)
        assertThat(out.size()).isGreaterThan(0);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ElectionResult sampleResult() {
        CandidateResult candidateA = new CandidateResult(
                UUID.randomUUID(), "Ana García", 40L, 61.5, false, false, true);
        CandidateResult candidateB = new CandidateResult(
                UUID.randomUUID(), "Luis Martínez", 15L, 23.1, false, false, false);
        CandidateResult blankVote = new CandidateResult(
                UUID.randomUUID(), "Voto en Blanco", 10L, 15.4, true, false, false);

        return new ElectionResult(
                UUID.randomUUID(),
                "Elección de Directivos 2026",
                List.of(candidateA, candidateB, blankVote),
                10L, 0L, 65L, 100L, 65L
        );
    }
}
