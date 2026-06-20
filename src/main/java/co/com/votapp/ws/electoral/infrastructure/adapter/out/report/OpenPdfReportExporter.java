package co.com.votapp.ws.electoral.infrastructure.adapter.out.report;

import co.com.votapp.ws.electoral.domain.model.CandidateResult;
import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStream;

/**
 * Output adapter: generates a streamed PDF election report using OpenPDF.
 *
 * <p>Implements {@link ReportExporterPort} for the PDF format. Writes directly to the
 * provided {@link OutputStream} without buffering the full document in memory.
 *
 * <p>This adapter is registered as a Spring bean in {@code ReportConfig}.
 * The watermark text is injected via constructor (sourced from {@code ReportProperties}).
 *
 * <p>This class is format-specific: it always generates PDF regardless of the
 * {@link ReportFormat} parameter — format routing is the responsibility of the caller.
 */
public class OpenPdfReportExporter implements ReportExporterPort {

    private final String watermarkText;

    public OpenPdfReportExporter(String watermarkText) {
        this.watermarkText = watermarkText;
    }

    @Override
    public void export(ElectionResult result, ReportFormat format, OutputStream out) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // ─── Title ────────────────────────────────────────────────────────
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
            Paragraph title = new Paragraph("Election Results Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            // ─── Watermark / institution line ─────────────────────────────────
            Font watermarkFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY);
            Paragraph watermark = new Paragraph(watermarkText, watermarkFont);
            watermark.setAlignment(Element.ALIGN_CENTER);
            document.add(watermark);

            document.add(new Paragraph(" "));

            // ─── Election metadata ────────────────────────────────────────────
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

            document.add(new Paragraph("Election: " + result.electionName(), labelFont));
            document.add(new Paragraph(" "));

            // ─── Participation statistics ─────────────────────────────────────
            document.add(new Paragraph("Participation Statistics", labelFont));
            document.add(new Paragraph("Eligible voters: " + result.eligibleCount(), valueFont));
            document.add(new Paragraph("Participants: " + result.participationCount(), valueFont));
            document.add(new Paragraph("Abstentions: " + result.abstentions(), valueFont));
            document.add(new Paragraph(
                    String.format("Participation rate: %.2f%%", result.participationRate()), valueFont));
            document.add(new Paragraph("Total votes: " + result.totalVotes(), valueFont));
            document.add(new Paragraph("Blank votes: " + result.blankVotes(), valueFont));
            document.add(new Paragraph("Null votes: " + result.nullVotes(), valueFont));

            document.add(new Paragraph(" "));

            // ─── Candidate results table ──────────────────────────────────────
            document.add(new Paragraph("Candidate Results", labelFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1f, 1.5f, 1f});

            // Table header
            addHeaderCell(table, "Candidate");
            addHeaderCell(table, "Votes");
            addHeaderCell(table, "Percentage");
            addHeaderCell(table, "Winner");

            // Table rows
            for (CandidateResult candidate : result.candidateResults()) {
                addDataCell(table, candidate.nombre(), valueFont);
                addDataCell(table, String.valueOf(candidate.votes()), valueFont);
                addDataCell(table, String.format("%.2f%%", candidate.percentage()), valueFont);
                addDataCell(table, candidate.isWinner() ? "Yes" : "", valueFont);
            }

            document.add(table);

        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate PDF report: " + e.getMessage(), e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(40, 80, 160));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }
}
