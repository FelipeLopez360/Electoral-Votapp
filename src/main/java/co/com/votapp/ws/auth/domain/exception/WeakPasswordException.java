package co.com.votapp.ws.auth.domain.exception;

import co.com.votapp.ws.common.exception.DomainException;

/**
 * Thrown when a password does not meet the strength policy.
 *
 * <p>Strength rules (MVP): minimum 8 characters, at least one uppercase letter,
 * one lowercase letter, and one digit.
 *
 * <p>Mapped to 409 Conflict by {@link co.com.votapp.ws.common.exception.GlobalExceptionHandler}
 * via the generic {@link DomainException} handler.
 */
public class WeakPasswordException extends DomainException {

    public WeakPasswordException() {
        super("La contraseña no cumple los requisitos: mínimo 8 caracteres, 1 mayúscula, 1 minúscula y 1 número");
    }
}
