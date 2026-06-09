package co.com.votapp.ws.auth.domain.exception;

import co.com.votapp.ws.common.exception.DomainException;

/**
 * Thrown when the provided credentials (documento + password) do not match.
 *
 * <p>Intentionally generic — never reveals whether the documento or password was wrong
 * (prevents username enumeration).
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}
