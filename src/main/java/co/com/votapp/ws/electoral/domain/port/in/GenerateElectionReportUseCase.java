package co.com.votapp.ws.electoral.domain.port.in;

import co.com.votapp.ws.electoral.domain.exception.ElectionNotFinalizedException;
import co.com.votapp.ws.electoral.domain.model.ReportFormat;

import java.io.OutputStream;
import java.util.UUID;

/**
 * Input port: generate and stream a binary report for a finalized election.
 *
 * <p>Delegates to {@link GetElectionResultsUseCase} for aggregation and to
 * {@code ReportExporterPort} for format-specific generation. Keeps domain pure —
 * no library-specific types appear in this contract.
 *
 * <p>Pure Java — ZERO framework imports.
 */
public interface GenerateElectionReportUseCase {

    /**
     * Generate a report for the given election and stream it to the output.
     *
     * @param eleccionId the election's UUID
     * @param format     the desired output format (PDF or XLSX)
     * @param out        the output stream to write the report bytes to;
     *                   the caller (application service) is responsible for closing
     * @throws co.com.votapp.ws.common.exception.NotFoundException if the election does not exist
     * @throws ElectionNotFinalizedException                        if the election is not FINALIZADA
     */
    void generateReport(UUID eleccionId, ReportFormat format, OutputStream out);
}
