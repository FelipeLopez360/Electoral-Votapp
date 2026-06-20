package co.com.votapp.ws.electoral.domain.port.out;

import co.com.votapp.ws.electoral.domain.model.ElectionResult;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;

import java.io.OutputStream;

/**
 * Output port for streaming election report generation.
 *
 * <p>Implemented by format-specific adapters (OpenPDF for PDF, Apache POI SXSSF for Excel).
 * The domain contract uses only standard JDK types ({@link OutputStream}) so the domain
 * layer remains completely free of library-specific imports.
 *
 * <p>Implementations MUST stream output directly to the provided {@link OutputStream}
 * rather than buffering the full document in memory, to keep memory usage bounded
 * for large elections.
 *
 * <p>Pure Java — ZERO framework imports.
 */
public interface ReportExporterPort {

    /**
     * Generate an election report in the specified format and stream it to the output.
     *
     * @param result the aggregated election results to include in the report
     * @param format the desired output format ({@link ReportFormat#PDF} or {@link ReportFormat#XLSX})
     * @param out    the output stream to write the report bytes to; the caller is responsible
     *               for closing this stream
     */
    void export(ElectionResult result, ReportFormat format, OutputStream out);
}
