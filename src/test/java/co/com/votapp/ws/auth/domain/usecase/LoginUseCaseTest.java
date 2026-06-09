package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.exception.AccountInactiveException;
import co.com.votapp.ws.auth.domain.exception.InvalidCredentialsException;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthenticateFuncionarioUseCase}.
 *
 * <p>No Spring context is loaded — pure Mockito-driven unit test.
 */
@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private FuncionarioRepositoryPort repositoryPort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    private AuthenticateFuncionarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateFuncionarioUseCase(repositoryPort, passwordEncoderPort);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private Funcionario buildFuncionario(String estadoLaboral) {
        return new Funcionario(
                1,
                "EMP-001",
                "12345678",
                "Juan",
                "Pérez",
                "CC",
                "juan.perez@votapp.co",
                null,
                null,
                null,
                null,
                true,
                estadoLaboral,
                false
        );
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void authenticate_returnsFuncionario_whenActiveAndFound() {
        Funcionario active = buildFuncionario("ACTIVO");
        when(repositoryPort.findBloqueadoHasta("12345678")).thenReturn(Optional.empty());
        when(repositoryPort.findByDocumentoIdentidad("12345678")).thenReturn(Optional.of(active));
        when(repositoryPort.findPasswordHashByDocumentoIdentidad("12345678")).thenReturn(Optional.of("hash"));
        when(passwordEncoderPort.matches("any-password", "hash")).thenReturn(true);

        Funcionario result = useCase.authenticate("12345678", "any-password");

        assertThat(result).isSameAs(active);
        assertThat(result.isActivo()).isTrue();
        assertThat(result.getDocumentoIdentidad()).isEqualTo("12345678");
    }

    @Test
    void authenticate_throwsDomainException_whenNotFound() {
        when(repositoryPort.findBloqueadoHasta("99999999")).thenReturn(Optional.empty());
        when(repositoryPort.findByDocumentoIdentidad("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.authenticate("99999999", "any-password"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void authenticate_throwsDomainException_whenInactive() {
        Funcionario inactive = buildFuncionario("INACTIVO");
        when(repositoryPort.findBloqueadoHasta("12345678")).thenReturn(Optional.empty());
        when(repositoryPort.findByDocumentoIdentidad("12345678")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> useCase.authenticate("12345678", "any-password"))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessage("Funcionario inactivo");
    }
}
