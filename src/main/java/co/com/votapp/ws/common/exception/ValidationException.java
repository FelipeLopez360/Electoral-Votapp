package co.com.votapp.ws.common.exception;

/**
 * Thrown when input fails domain validation.
 * <p>
 * Maps to HTTP 400 via {@link GlobalExceptionHandler}.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
