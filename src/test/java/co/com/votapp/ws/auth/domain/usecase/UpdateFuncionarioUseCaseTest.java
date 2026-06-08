package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.UpdateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UpdateFuncionarioUseCase - Funcionario update business logic")
@ExtendWith(MockitoExtension.class)
class UpdateFuncionarioUseCaseTest {

    @Mock
    private FuncionarioRepositoryPort funcionarioRepository;

    private UpdateFuncionarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateFuncionarioUseCaseImpl(funcionarioRepository);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Funcionario existingFuncionario() {
        return new Funcionario(
                1,
                "EMP-001",
                "10000001",
                "Juan",
                "Pérez",
                "CC",
                "juan@test.com",
                null,
                1,
                2,
                null,
                true,
                "ACTIVO",
                true
        );
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update funcionario fields when valid command provided")
    void update_shouldUpdateFuncionario_whenValid() {
        // Given
        Funcionario existing = existingFuncionario();
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(existing));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

        var command = new UpdateFuncionarioUseCase.Command(
                1,
                "Juan Carlos",       // updated
                "Pérez González",    // updated
                "nuevo@test.com",    // updated
                "+57 300 000 0000",  // updated
                1,
                3,                   // cargoId updated
                "SUSPENDIDO",        // estadoLaboral updated
                false,               // puedeVotar updated
                null                 // debeCambiarPassword unchanged
        );

        // When
        Funcionario result = useCase.update(command);

        // Then
        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(funcionarioRepository).save(captor.capture());
        Funcionario saved = captor.getValue();

        assertThat(saved.getNombres()).isEqualTo("Juan Carlos");
        assertThat(saved.getApellidos()).isEqualTo("Pérez González");
        assertThat(saved.getEmail()).isEqualTo("nuevo@test.com");
        assertThat(saved.getEstadoLaboral()).isEqualTo("SUSPENDIDO");
        assertThat(saved.isPuedeVotar()).isFalse();
        assertThat(saved.getCargoId()).isEqualTo(3);

        // Immutable fields preserved
        assertThat(saved.getDocumentoIdentidad()).isEqualTo("10000001");
        assertThat(saved.getId()).isEqualTo(1);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should preserve existing values for null command fields (partial update)")
    void update_shouldPreserveExistingValues_whenCommandFieldsAreNull() {
        // Given
        Funcionario existing = existingFuncionario();
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(existing));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));

        // Only update email, leave everything else null
        var command = new UpdateFuncionarioUseCase.Command(
                1, null, null, "updated@test.com", null, null, null, null, null, null
        );

        // When
        useCase.update(command);

        // Then
        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(funcionarioRepository).save(captor.capture());
        Funcionario saved = captor.getValue();

        assertThat(saved.getNombres()).isEqualTo("Juan");           // preserved
        assertThat(saved.getApellidos()).isEqualTo("Pérez");        // preserved
        assertThat(saved.getEmail()).isEqualTo("updated@test.com"); // updated
        assertThat(saved.getEstadoLaboral()).isEqualTo("ACTIVO");   // preserved
        assertThat(saved.isPuedeVotar()).isTrue();                  // preserved
    }

    @Test
    @DisplayName("Should throw NotFoundException when funcionario does not exist")
    void update_shouldThrowNotFoundException_whenFuncionarioDoesNotExist() {
        // Given
        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        var command = new UpdateFuncionarioUseCase.Command(
                99, "Juan", null, null, null, null, null, null, null, null
        );

        // When & Then
        assertThatThrownBy(() -> useCase.update(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        verify(funcionarioRepository, never()).save(any());
    }
}
