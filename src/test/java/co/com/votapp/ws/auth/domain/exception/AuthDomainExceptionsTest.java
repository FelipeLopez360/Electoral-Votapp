package co.com.votapp.ws.auth.domain.exception;

import co.com.votapp.ws.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for auth domain exceptions (Tasks 1.3).
 *
 * <p>RED phase: these tests will fail until the exception classes are created.
 */
@DisplayName("Auth Domain Exceptions - Hierarchy and messages")
class AuthDomainExceptionsTest {

    @Test
    @DisplayName("Should extend DomainException and carry the expected message - InvalidCredentialsException")
    void invalidCredentialsException_shouldExtendDomainException_withExpectedMessage() {
        // When
        InvalidCredentialsException ex = new InvalidCredentialsException();

        // Then
        assertThat(ex).isInstanceOf(DomainException.class);
        assertThat(ex.getMessage()).isEqualTo("Credenciales inválidas");
    }

    @Test
    @DisplayName("Should extend DomainException and carry the expected message - AccountLockedException")
    void accountLockedException_shouldExtendDomainException_withExpectedMessage() {
        // When
        AccountLockedException ex = new AccountLockedException();

        // Then
        assertThat(ex).isInstanceOf(DomainException.class);
        assertThat(ex.getMessage()).isEqualTo("Cuenta bloqueada temporalmente. Intentá de nuevo más tarde.");
    }

    @Test
    @DisplayName("Should extend DomainException and carry the expected message - AccountInactiveException")
    void accountInactiveException_shouldExtendDomainException_withExpectedMessage() {
        // When
        AccountInactiveException ex = new AccountInactiveException();

        // Then
        assertThat(ex).isInstanceOf(DomainException.class);
        assertThat(ex.getMessage()).isEqualTo("Funcionario inactivo");
    }
}
