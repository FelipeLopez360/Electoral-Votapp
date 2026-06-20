package co.com.votapp.ws.electoral.infrastructure.adapter.out.report;

import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("PoiExcelReportExporter - Excel generation via Apache POI")
class PoiExcelReportExporterTest {

    private static final String WATERMARK = "Votapp - Sistema Electoral";

    private ReportExporterPort exporter;

    @BeforeEach
    void setUp() {
        exporter = new PoiExcelReportExporter(WATERMARK);
    }

    @Test
    @DisplayName("Should produce non-empty byte output for XLSX format")
    void export_shouldProduceNonEmptyBytes_whenXlsxFormatRequested() {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult();

        // When
        exporter.export(result, ReportFormat.XLSX, out);

        // Then
        assertThat(out.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should produce valid XLSX workbook with a results sheet")
    void export_shouldProduceValidWorkbook_whenXlsxFormatRequested() throws Exception {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult();

        // When
        exporter.export(result, ReportFormat.XLSX, out);

        // Then — parse the output as a real workbook
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertThat(workbook.getNumberOfSheets()).isGreaterThanOrEqualTo(1);
            Sheet resultsSheet = workbook.getSheetAt(0);
            assertThat(resultsSheet).isNotNull();
        }
    }

    @Test
    @DisplayName("Should embed institutional watermark text in the watermark row (row index 1, cell 0)")
    void export_shouldContainWatermarkText_inWatermarkRow() throws Exception {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult();

        // When
        exporter.export(result, ReportFormat.XLSX, out);

        // Then — watermark is written to row 1 (0-indexed), cell 0
        // Row layout: [0]=title, [1]=watermark, [2]=blank, [3..]=participation stats
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row watermarkRow = sheet.getRow(1);
            assertThat(watermarkRow).isNotNull();
            assertThat(watermarkRow.getCell(0).getStringCellValue()).isEqualTo(WATERMARK);
        }
    }

    @Test
    @DisplayName("Should include candidate data rows in the results sheet")
    void export_shouldIncludeCandidateRows_inResultsSheet() throws Exception {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult(); // 3 candidates

        // When
        exporter.export(result, ReportFormat.XLSX, out);

        // Then — verify rows exist (header + at least 3 data rows)
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            // At minimum: header row + N candidate rows
            assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(result.candidateResults().size());
        }
    }

    @Test
    @DisplayName("Should include participation statistics rows (eligible voters, participants) in the sheet")
    void export_shouldContainParticipationStats_inMetaRows() throws Exception {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ElectionResult result = sampleResult(); // eligibleCount=100, participationCount=65

        // When
        exporter.export(result, ReportFormat.XLSX, out);

        // Then — scan all rows for "Eligible voters" label
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean foundEligibleLabel = false;
            boolean foundParticipantsLabel = false;
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                if (row.getCell(0) == null) continue;
                String cellValue = row.getCell(0).getStringCellValue();
                if ("Eligible voters".equals(cellValue)) foundEligibleLabel = true;
                if ("Participants".equals(cellValue)) foundParticipantsLabel = true;
            }
            assertThat(foundEligibleLabel)
                    .as("Expected 'Eligible voters' label row in workbook")
                    .isTrue();
            assertThat(foundParticipantsLabel)
                    .as("Expected 'Participants' label row in workbook")
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Should not throw when exporting an election with only synthetic candidates")
    void export_shouldNotThrow_whenOnlySyntheticCandidatesPresent() {
        // Given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CandidateResult blankVote = new CandidateResult(
                UUID.randomUUID(), "Voto en Blanco", 10L, 100.0, true, false, false);
        ElectionResult result = new ElectionResult(
                UUID.randomUUID(), "Minimal Election", List.of(blankVote),
                10L, 0L, 10L, 20L, 10L);

        // When & Then
        assertThatNoException().isThrownBy(() -> exporter.export(result, ReportFormat.XLSX, out));
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
