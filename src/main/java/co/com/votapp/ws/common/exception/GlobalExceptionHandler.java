package co.com.votapp.ws.common.exception;

import co.com.votapp.ws.auth.domain.exception.AccountInactiveException;
import co.com.votapp.ws.auth.domain.exception.AccountLockedException;
import co.com.votapp.ws.auth.domain.exception.InvalidCredentialsException;
import co.com.votapp.ws.electoral.domain.exception.ElectionNotModifiableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler that maps domain exceptions to HTTP responses.
 *
 * <p>Keeps error-to-status mapping out of controllers, centralising it here
 * so every module benefits automatically.
 *
 * <ul>
 *   <li>{@link NotFoundException} → 404 Not Found</li>
 *   <li>{@link ValidationException} → 400 Bad Request</li>
 *   <li>{@link DomainException} → 409 Conflict</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── Bean-validation and request-binding failures → 400 ─────────────────

    /**
     * Handles {@link MethodArgumentNotValidException} thrown by {@code @Valid} on
     * {@code @RequestBody} when bean-validation constraints fail (e.g. {@code @NotNull}).
     *
     * <p>Collects all field-level violation messages and joins them with a comma so the
     * response is deterministic and framework-wording-agnostic.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message.isBlank() ? "Invalid request" : message));
    }

    /**
     * Handles {@link HttpMessageNotReadableException} thrown when Jackson cannot deserialize
     * the request body — e.g. a malformed UUID string into a {@link java.util.UUID} field.
     *
     * <p>Returns a stable 400 response with the {@link ErrorResponse} shape so callers
     * never see the raw Spring/Jackson error format.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Malformed or unreadable request body"));
    }

    // ─── Portal auth exceptions (must come before generic DomainException) ───

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity
                .status(HttpStatus.LOCKED)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountInactive(AccountInactiveException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // ─── Electoral domain exceptions ──────────────────────────────────────────

    @ExceptionHandler(ElectionNotModifiableException.class)
    public ResponseEntity<ErrorResponse> handleElectionNotModifiable(ElectionNotModifiableException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // ─── Generic domain and application exceptions ────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    /** DTO for error responses (Record — mandatory per architecture). */
    public record ErrorResponse(String message) {}
}
