package co.com.votapp.ws.auth.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.CreateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CreateFuncionarioUseCase - Funcionario creation business logic")
@ExtendWith(MockitoExtension.class)
class CreateFuncionarioUseCaseTest {

    @Mock
    private FuncionarioRepositoryPort funcionarioRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private CreateFuncionarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateFuncionarioUseCaseImpl(funcionarioRepository, passwordEncoder);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Funcionario buildSavedFuncionario(String documentoIdentidad) {
        return new Funcionario(
                1,
                "EMP-001",
                documentoIdentidad,
                "Juan",
                "Pérez",
                "CC",
                "juan@test.com",
                null,
                null,
                null,
                null,
                true,
                "ACTIVO",
                true
        );
    }

    private CreateFuncionarioUseCase.Command validCommand() {
        return new CreateFuncionarioUseCase.Command(
                "Juan Carlos",
                "Pérez González",
                "CC",
                "10000001",
                "juan@test.com",
                null,
                null,
                null,
                "ACTIVO",
                true
        );
    }

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create funcionario when all fields are valid")
    void create_shouldReturnResult_whenAllFieldsValid() {
        // Given
        var command = validCommand();
        var savedFuncionario = buildSavedFuncionario("10000001");

        when(funcionarioRepository.existsByDocumentoIdentidad("10000001")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
        when(funcionarioRepository.saveWithHash(any(Funcionario.class), anyString()))
                .thenReturn(savedFuncionario);

        // When
        CreateFuncionarioUseCase.Result result = useCase.create(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.funcionario()).isNotNull();
        assertThat(result.temporaryPassword()).isNotBlank();
        assertThat(result.temporaryPassword()).hasSize(12);
    }

    @Test
    @DisplayName("Should generate a hashed password and persist it — raw password is never stored")
    void create_shouldHashPassword_andNeverPersistRaw() {
        // Given
        var command = validCommand();
        var savedFuncionario = buildSavedFuncionario("10000001");

        when(funcionarioRepository.existsByDocumentoIdentidad("10000001")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
        when(funcionarioRepository.saveWithHash(any(Funcionario.class), anyString()))
                .thenReturn(savedFuncionario);

        // When
        CreateFuncionarioUseCase.Result result = useCase.create(command);

        // Then — verify the hash persisted is NOT the raw password
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(funcionarioRepository).saveWithHash(any(), hashCaptor.capture());

        String persistedHash = hashCaptor.getValue();
        assertThat(persistedHash).isEqualTo("$2a$hashed");
        assertThat(persistedHash).isNotEqualTo(result.temporaryPassword());
    }

    @Test
    @DisplayName("Should throw ValidationException when nombres is blank")
    void create_shouldThrowValidationException_whenNombreIsBlank() {
        // Given
        var command = new CreateFuncionarioUseCase.Command(
                "",            // blank nombres
                "Pérez",
                "CC",
                "10000001",
                null,
                null,
                null,
                null,
                "ACTIVO",
                true
        );

        // When & Then
        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("nombres");

        verify(funcionarioRepository, never()).saveWithHash(any(), anyString());
    }

    @Test
    @DisplayName("Should throw ValidationException when documentoIdentidad is blank")
    void create_shouldThrowValidationException_whenDocumentoIsBlank() {
        // Given
        var command = new CreateFuncionarioUseCase.Command(
                "Juan",
                "Pérez",
                "CC",
                "",            // blank documento
                null,
                null,
                null,
                null,
                "ACTIVO",
                true
        );

        // When & Then
        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("documentoIdentidad");

        verify(funcionarioRepository, never()).saveWithHash(any(), anyString());
    }

    @Test
    @DisplayName("Should throw DomainException when documentoIdentidad already exists")
    void create_shouldThrowDomainException_whenDocumentoIdentidadDuplicate() {
        // Given
        var command = validCommand();
        when(funcionarioRepository.existsByDocumentoIdentidad("10000001")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("10000001");

        verify(funcionarioRepository, never()).saveWithHash(any(), anyString());
    }
}
