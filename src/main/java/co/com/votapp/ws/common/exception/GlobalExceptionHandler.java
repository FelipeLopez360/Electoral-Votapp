package co.com.votapp.ws.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that maps domain exceptions to HTTP responses.
 *
 * <p>Keeps error-to-status mapping out of controllers, centralising it here
 * so every module benefits automatically.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    /** DTO for error responses (Record — mandatory per architecture). */
    public record ErrorResponse(String message) {}
}
