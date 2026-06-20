package co.com.votapp.ws.electoral.infrastructure.adapter.out.report;

import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;
import co.com.votapp.ws.electoral.domain.port.out.ReportExporterPort;

import java.io.OutputStream;

/**
 * Composite adapter that dispatches report generation to the format-specific
 * {@link ReportExporterPort} implementation.
 *
 * <p>The domain use case holds a single {@link ReportExporterPort} reference; this
 * dispatcher implements that port and routes to the correct format-specific adapter
 * ({@link OpenPdfReportExporter} for PDF, {@link PoiExcelReportExporter} for XLSX).
 *
 * <p>Wired in {@code ReportConfig}. Keeps the domain use case clean — it doesn't
 * know about format-specific library choices.
 */
public class ReportExporterDispatcher implements ReportExporterPort {

    private final ReportExporterPort pdfExporter;
    private final ReportExporterPort xlsxExporter;

    public ReportExporterDispatcher(ReportExporterPort pdfExporter, ReportExporterPort xlsxExporter) {
        this.pdfExporter = pdfExporter;
        this.xlsxExporter = xlsxExporter;
    }

    @Override
    public void export(ElectionResult result, ReportFormat format, OutputStream out) {
        switch (format) {
            case PDF -> pdfExporter.export(result, format, out);
            case XLSX -> xlsxExporter.export(result, format, out);
            default -> throw new IllegalArgumentException("Unsupported report format: " + format);
        }
    }
}
