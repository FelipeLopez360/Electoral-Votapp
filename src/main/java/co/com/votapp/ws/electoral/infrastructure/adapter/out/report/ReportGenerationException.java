package co.com.votapp.ws.electoral.infrastructure.adapter.out.report;

/**
 * Runtime exception thrown when report generation fails in an exporter adapter.
 *
 * <p>Lives in the infrastructure layer — not a domain exception.
 * Caught and mapped to HTTP 500 by the global exception handler (catch-all).
 */
public class ReportGenerationException extends RuntimeException {

    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
