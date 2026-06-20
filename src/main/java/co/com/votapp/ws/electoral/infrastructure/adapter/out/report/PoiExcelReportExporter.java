package co.com.votapp.ws.electoral.infrastructure.adapter.out.report;

import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.OutputStream;
import java.util.List;

/**
 * Output adapter: generates a streamed Excel election report using Apache POI SXSSF.
 *
 * <p>Implements {@link ReportExporterPort} for the XLSX format. Uses SXSSF (Streaming API)
 * so memory usage stays bounded for large elections regardless of vote count.
 *
 * <p>This adapter is registered as a Spring bean in {@code ReportConfig}.
 * The watermark text is injected via constructor (sourced from {@code ReportProperties}).
 *
 * <p>This class is format-specific: it always generates XLSX regardless of the
 * {@link ReportFormat} parameter — format routing is the responsibility of the caller.
 */
public class PoiExcelReportExporter implements ReportExporterPort {

    private static final int SXSSF_ROW_ACCESS_WINDOW = 100;

    private final String watermarkText;

    public PoiExcelReportExporter(String watermarkText) {
        this.watermarkText = watermarkText;
    }

    @Override
    public void export(ElectionResult result, ReportFormat format, OutputStream out) {
        // SXSSFWorkbook streams data — avoids buffering entire file in heap
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(SXSSF_ROW_ACCESS_WINDOW)) {

            // ─── Sheet 1: Candidate Results ───────────────────────────────────
            Sheet resultsSheet = workbook.createSheet("Results");

            // Styles
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle metaLabelStyle = buildMetaLabelStyle(workbook);
            CellStyle dataStyle = buildDataStyle(workbook);

            int rowIndex = 0;

            // Title row
            Row titleRow = resultsSheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Election Results Report — " + result.electionName());
            titleCell.setCellStyle(metaLabelStyle);

            // Watermark row
            Row watermarkRow = resultsSheet.createRow(rowIndex++);
            Cell watermarkCell = watermarkRow.createCell(0);
            watermarkCell.setCellValue(watermarkText);

            // Blank row
            resultsSheet.createRow(rowIndex++);

            // Participation stats
            rowIndex = writeMetaRow(resultsSheet, rowIndex, metaLabelStyle, dataStyle,
                    "Eligible voters", String.valueOf(result.eligibleCount()));
            rowIndex = writeMetaRow(resultsSheet, rowIndex, metaLabelStyle, dataStyle,
                    "Participants", String.valueOf(result.participationCount()));
            rowIndex = writeMetaRow(resultsSheet, rowIndex, metaLabelStyle, dataStyle,
                    "Abstentions", String.valueOf(result.abstentions()));
            rowIndex = writeMetaRow(resultsSheet, rowIndex, metaLabelStyle, dataStyle,
                    "Participation rate", String.format("%.2f%%", result.participationRate()));
            rowIndex = writeMetaRow(resultsSheet, rowIndex, metaLabelStyle, dataStyle,
                    "Total votes", String.valueOf(result.totalVotes()));
            rowIndex = writeMetaRow(resultsSheet, rowIndex, metaLabelStyle, dataStyle,
                    "Blank votes", String.valueOf(result.blankVotes()));
            rowIndex = writeMetaRow(resultsSheet, rowIndex, metaLabelStyle, dataStyle,
                    "Null votes", String.valueOf(result.nullVotes()));

            // Blank row before candidate table
            resultsSheet.createRow(rowIndex++);

            // Candidate results header
            String[] headers = {"Candidate", "Votes", "Percentage", "Blank?", "Null?", "Winner?"};
            Row headerRow = resultsSheet.createRow(rowIndex++);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            // Candidate data rows
            for (CandidateResult candidate : result.candidateResults()) {
                Row dataRow = resultsSheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(candidate.nombre());
                dataRow.createCell(1).setCellValue(candidate.votes());
                dataRow.createCell(2).setCellValue(String.format("%.2f%%", candidate.percentage()));
                dataRow.createCell(3).setCellValue(candidate.esVotoEnBlanco() ? "Yes" : "No");
                dataRow.createCell(4).setCellValue(candidate.esVotoNulo() ? "Yes" : "No");
                dataRow.createCell(5).setCellValue(candidate.isWinner() ? "Yes" : "");

                // Apply data style to all cells
                for (int col = 0; col < 6; col++) {
                    dataRow.getCell(col).setCellStyle(dataStyle);
                }
                // Override text cells that need string style
                dataRow.getCell(0).setCellValue(candidate.nombre());
            }

            workbook.write(out);

        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    // ─── Style builders ───────────────────────────────────────────────────────

    private CellStyle buildHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle buildMetaLabelStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle buildDataStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private int writeMetaRow(Sheet sheet, int rowIndex,
                              CellStyle labelStyle, CellStyle valueStyle,
                              String label, String value) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
        return rowIndex + 1;
    }
}
