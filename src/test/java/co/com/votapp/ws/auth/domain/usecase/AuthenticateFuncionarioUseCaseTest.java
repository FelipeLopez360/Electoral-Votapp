package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.exception.AccountInactiveException;
import co.com.votapp.ws.auth.domain.exception.AccountLockedException;
import co.com.votapp.ws.auth.domain.exception.InvalidCredentialsException;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for updated {@link AuthenticateFuncionarioUseCase} (Task 1.4).
 *
 * <p>RED phase: tests will fail until use case is rewritten with BCrypt + lockout logic.
 */
@DisplayName("AuthenticateFuncionarioUseCase - BCrypt verification + lockout flow")
@ExtendWith(MockitoExtension.class)
class AuthenticateFuncionarioUseCaseTest {

    private static final String DOCUMENTO = "12345678";
    private static final String RAW_PASSWORD = "TestPass123!";
    private static final String HASHED_PASSWORD = "$2a$12$someHashedValue";

    @Mock
    private FuncionarioRepositoryPort repositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    private AuthenticateFuncionarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateFuncionarioUseCase(repositoryPort, passwordEncoderPort);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return Funcionario and reset failed attempts on successful login")
    void authenticate_shouldReturnFuncionario_whenCredentialsAreValid() {
        // Given
        Funcionario active = activeFuncionario();
        when(repositoryPort.findBloqueadoHasta(DOCUMENTO)).thenReturn(Optional.empty());
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(active));
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(HASHED_PASSWORD));
        when(passwordEncoderPort.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        // When
        Funcionario result = useCase.authenticate(DOCUMENTO, RAW_PASSWORD);

        // Then
        assertThat(result).isSameAs(active);
        verify(repositoryPort).resetFailedAttempts(DOCUMENTO);
        verify(repositoryPort).updateUltimoAcceso(anyString(), any(LocalDateTime.class));
    }

    // ─── Lockout checks ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw AccountLockedException when bloqueadoHasta is in the future")
    void authenticate_shouldThrowAccountLockedException_whenAccountIsLocked() {
        // Given
        LocalDateTime future = LocalDateTime.now().plusMinutes(10);
        when(repositoryPort.findBloqueadoHasta(DOCUMENTO)).thenReturn(Optional.of(future));

        // When & Then
        assertThatThrownBy(() -> useCase.authenticate(DOCUMENTO, RAW_PASSWORD))
                .isInstanceOf(AccountLockedException.class);

        // No further repo calls expected
        verify(repositoryPort, never()).findByDocumentoIdentidad(anyString());
    }

    @Test
    @DisplayName("Should NOT throw AccountLockedException when bloqueadoHasta is in the past (expired)")
    void authenticate_shouldNotThrowLockedException_whenLockHasExpired() {
        // Given
        LocalDateTime past = LocalDateTime.now().minusMinutes(5);
        when(repositoryPort.findBloqueadoHasta(DOCUMENTO)).thenReturn(Optional.of(past));
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(activeFuncionario()));
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(HASHED_PASSWORD));
        when(passwordEncoderPort.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        // When — should NOT throw
        Funcionario result = useCase.authenticate(DOCUMENTO, RAW_PASSWORD);

        // Then
        assertThat(result).isNotNull();
    }

    // ─── Invalid credentials ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw InvalidCredentialsException when funcionario not found")
    void authenticate_shouldThrowInvalidCredentials_whenFuncionarioNotFound() {
        // Given
        when(repositoryPort.findBloqueadoHasta(DOCUMENTO)).thenReturn(Optional.empty());
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.authenticate(DOCUMENTO, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password does not match")
    void authenticate_shouldThrowInvalidCredentials_whenPasswordDoesNotMatch() {
        // Given
        Funcionario active = activeFuncionario();
        when(repositoryPort.findBloqueadoHasta(DOCUMENTO)).thenReturn(Optional.empty());
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(active));
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(HASHED_PASSWORD));
        when(passwordEncoderPort.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(repositoryPort.findFailedAttempts(DOCUMENTO)).thenReturn(0);

        // When & Then
        assertThatThrownBy(() -> useCase.authenticate(DOCUMENTO, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(repositoryPort).incrementFailedAttempts(DOCUMENTO);
    }

    // ─── Account inactive ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw AccountInactiveException when funcionario is inactive")
    void authenticate_shouldThrowAccountInactiveException_whenFuncionarioIsInactive() {
        // Given
        Funcionario inactive = inactiveFuncionario();
        when(repositoryPort.findBloqueadoHasta(DOCUMENTO)).thenReturn(Optional.empty());
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(inactive));

        // When & Then
        assertThatThrownBy(() -> useCase.authenticate(DOCUMENTO, RAW_PASSWORD))
                .isInstanceOf(AccountInactiveException.class);

        verify(repositoryPort, never()).incrementFailedAttempts(anyString());
    }

    // ─── Lockout after 3 failed attempts ─────────────────────────────────────

    @Test
    @DisplayName("Should lock account for 15 minutes when failed attempts reach 3")
    void authenticate_shouldLockAccount_whenFailedAttemptsReachThreshold() {
        // Given
        Funcionario active = activeFuncionario();
        when(repositoryPort.findBloqueadoHasta(DOCUMENTO)).thenReturn(Optional.empty());
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(active));
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(HASHED_PASSWORD));
        when(passwordEncoderPort.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        // After increment, we'll be at 3 (the threshold)
        when(repositoryPort.findFailedAttempts(DOCUMENTO)).thenReturn(3);

        // When & Then
        assertThatThrownBy(() -> useCase.authenticate(DOCUMENTO, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(repositoryPort).incrementFailedAttempts(DOCUMENTO);
        verify(repositoryPort).lockAccount(anyString(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should NOT lock account when failed attempts are below threshold (2)")
    void authenticate_shouldNotLockAccount_whenFailedAttemptsAreBelowThreshold() {
        // Given
        Funcionario active = activeFuncionario();
        when(repositoryPort.findBloqueadoHasta(DOCUMENTO)).thenReturn(Optional.empty());
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(active));
        when(repositoryPort.findPasswordHashByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(HASHED_PASSWORD));
        when(passwordEncoderPort.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(repositoryPort.findFailedAttempts(DOCUMENTO)).thenReturn(2); // below 3

        // When & Then
        assertThatThrownBy(() -> useCase.authenticate(DOCUMENTO, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(repositoryPort).incrementFailedAttempts(DOCUMENTO);
        verify(repositoryPort, never()).lockAccount(anyString(), any(LocalDateTime.class));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Funcionario activeFuncionario() {
        return new Funcionario(1, "EMP001", DOCUMENTO, "Juan", "Pérez",
                "CC", "juan@test.co", null, null, null, null, true, "ACTIVO", false);
    }

    private Funcionario inactiveFuncionario() {
        return new Funcionario(2, "EMP002", DOCUMENTO, "María", "García",
                "CC", "maria@test.co", null, null, null, null, false, "INACTIVO", false);
    }
}
