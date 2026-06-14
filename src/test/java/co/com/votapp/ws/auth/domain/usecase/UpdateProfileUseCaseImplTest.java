package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UpdateProfileUseCaseImpl}.
 *
 * <p>Strict TDD: tests written BEFORE the implementation class exists.
 */
@DisplayName("UpdateProfileUseCaseImpl - Profile update business rules")
@ExtendWith(MockitoExtension.class)
class UpdateProfileUseCaseImplTest {

    private static final String DOCUMENTO = "12345678";

    @Mock
    private FuncionarioRepositoryPort repositoryPort;

    private UpdateProfileUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateProfileUseCaseImpl(repositoryPort);
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update only email and telefono, preserving all other fields")
    void update_shouldUpdateEmailAndTelefono_preservingOtherFields() {
        // Given
        Funcionario existing = existingFuncionario();
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(existing));

        Funcionario updated = new Funcionario(
                existing.getId(), existing.getNumeroEmpleado(), existing.getDocumentoIdentidad(),
                existing.getNombres(), existing.getApellidos(), existing.getTipoDocumento(),
                "new@email.co", "3001112233",
                existing.getDepartamentoId(), existing.getCargoId(), existing.getFechaIngreso(),
                existing.isPuedeVotar(), existing.getEstadoLaboral(), existing.isDebeCambiarPassword()
        );
        when(repositoryPort.save(any(Funcionario.class))).thenReturn(updated);

        // When
        Funcionario result = useCase.update(DOCUMENTO, "new@email.co", "3001112233");

        // Then
        assertThat(result.getEmail()).isEqualTo("new@email.co");
        assertThat(result.getTelefono()).isEqualTo("3001112233");
        // Preserved fields must remain unchanged
        assertThat(result.getNombres()).isEqualTo("Juan");
        assertThat(result.getApellidos()).isEqualTo("Pérez");
        assertThat(result.getDocumentoIdentidad()).isEqualTo(DOCUMENTO);
        assertThat(result.getNumeroEmpleado()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName("Should pass funcionario with updated email and telefono to the repository save")
    void update_shouldSaveFuncionarioWithUpdatedContactFields() {
        // Given
        Funcionario existing = existingFuncionario();
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.of(existing));
        when(repositoryPort.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.update(DOCUMENTO, "updated@email.co", "3009998877");

        // Then — capture what was saved
        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(repositoryPort).save(captor.capture());
        Funcionario saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("updated@email.co");
        assertThat(saved.getTelefono()).isEqualTo("3009998877");
        // Immutable fields must be copied verbatim
        assertThat(saved.getId()).isEqualTo(existing.getId());
        assertThat(saved.getNombres()).isEqualTo(existing.getNombres());
        assertThat(saved.getApellidos()).isEqualTo(existing.getApellidos());
        assertThat(saved.getNumeroEmpleado()).isEqualTo(existing.getNumeroEmpleado());
        assertThat(saved.getDocumentoIdentidad()).isEqualTo(existing.getDocumentoIdentidad());
    }

    // ─── Not found ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw NotFoundException when funcionario does not exist for the given documento")
    void update_shouldThrowNotFoundException_whenFuncionarioNotFound() {
        // Given
        when(repositoryPort.findByDocumentoIdentidad(DOCUMENTO)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.update(DOCUMENTO, "x@y.co", "111"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(DOCUMENTO);

        verify(repositoryPort, never()).save(any());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Funcionario existingFuncionario() {
        return new Funcionario(
                1, "EMP001", DOCUMENTO, "Juan", "Pérez",
                "CC", "old@email.co", "3000001111",
                10, 2, LocalDate.of(2020, 1, 15),
                true, "ACTIVO", false
        );
    }
}
