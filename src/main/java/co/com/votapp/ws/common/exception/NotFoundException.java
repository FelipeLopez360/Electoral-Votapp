package co.com.votapp.ws.common.exception;

/**
 * Thrown when a requested resource cannot be found.
 * <p>
 * Maps to HTTP 404 via {@link GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
