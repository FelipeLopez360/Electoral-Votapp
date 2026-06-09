package co.com.votapp.ws.auth.domain.exception;

import co.com.votapp.ws.common.exception.DomainException;

/**
 * Thrown when a funcionario account has been temporarily locked after exceeding
 * the maximum number of failed login attempts.
 */
public class AccountLockedException extends DomainException {

    public AccountLockedException() {
        super("Cuenta bloqueada temporalmente. Intentá de nuevo más tarde.");
    }
}
