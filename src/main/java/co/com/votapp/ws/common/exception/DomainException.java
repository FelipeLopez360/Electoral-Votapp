package co.com.votapp.ws.common.exception;

/**
 * Base exception for all domain-related errors.
 * <p>
 * This exception should be thrown when a business rule or invariant is violated.
 * It is a {@link RuntimeException} because domain exceptions typically represent
 * irrecoverable logic errors from the client's perspective (e.g. invalid state transitions).
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
