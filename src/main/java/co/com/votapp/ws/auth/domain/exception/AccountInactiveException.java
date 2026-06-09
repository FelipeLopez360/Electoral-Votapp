package co.com.votapp.ws.auth.domain.exception;

import co.com.votapp.ws.common.exception.DomainException;

/**
 * Thrown when a funcionario attempts to authenticate but their account
 * is marked as inactive ({@code estado_laboral != 'ACTIVO'}).
 */
public class AccountInactiveException extends DomainException {

    public AccountInactiveException() {
        super("Funcionario inactivo");
    }
}
