package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private AuthenticateFuncionarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateFuncionarioUseCase(repositoryPort);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private Funcionario buildFuncionario(String estadoLaboral) {
        return new Funcionario(
                1,
                "EMP-001",
                "12345678",
                "juan.perez@votapp.co",
                true,
                estadoLaboral
        );
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void authenticate_returnsFuncionario_whenActiveAndFound() {
        Funcionario active = buildFuncionario("ACTIVO");
        when(repositoryPort.findByDocumentoIdentidad("12345678"))
                .thenReturn(Optional.of(active));

        Funcionario result = useCase.authenticate("12345678", "any-password");

        assertThat(result).isSameAs(active);
        assertThat(result.isActivo()).isTrue();
        assertThat(result.getDocumentoIdentidad()).isEqualTo("12345678");
    }

    @Test
    void authenticate_throwsDomainException_whenNotFound() {
        when(repositoryPort.findByDocumentoIdentidad("99999999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.authenticate("99999999", "any-password"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Credenciales inválidas");
    }

    @Test
    void authenticate_throwsDomainException_whenInactive() {
        Funcionario inactive = buildFuncionario("INACTIVO");
        when(repositoryPort.findByDocumentoIdentidad("12345678"))
                .thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> useCase.authenticate("12345678", "any-password"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Funcionario inactivo");
    }
}
