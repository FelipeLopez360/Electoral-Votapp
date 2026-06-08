package co.com.votapp.ws.auth.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.application.dto.CreateFuncionarioRequest;
import co.com.votapp.ws.auth.application.dto.FuncionarioResponse;
import co.com.votapp.ws.auth.application.dto.UpdateFuncionarioRequest;
import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.in.CreateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.in.UpdateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.common.exception.NotFoundException;
import co.com.votapp.ws.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FuncionarioController}.
 *
 * <p>No Spring context — collaborators are mocked via Mockito.
 * Covers HTTP mapping, command assembly, response projection, and exception propagation
 * for all four endpoints: GET list, GET detail, POST create, PUT update.
 */
@DisplayName("FuncionarioController - Funcionario CRUD REST adapter")
@ExtendWith(MockitoExtension.class)
class FuncionarioControllerTest {

    @Mock private FuncionarioRepositoryPort funcionarioRepository;
    @Mock private CreateFuncionarioUseCase createFuncionarioUseCase;
    @Mock private UpdateFuncionarioUseCase updateFuncionarioUseCase;

    private FuncionarioController controller;

    // ── Test fixtures ────────────────────────────────────────────────────────

    private static Funcionario sampleFuncionario(int id) {
        return new Funcionario(
                id,
                "EMP-00" + id,
                "1000000" + id,
                "Ana",
                "García",
                "CC",
                "ana@test.com",
                null,
                1,
                2,
                null,
                true,
                "ACTIVO",
                true
        );
    }

    private static CreateFuncionarioRequest validCreateRequest() {
        return new CreateFuncionarioRequest(
                "Ana",
                "García",
                "CC",
                "10000001",
                "ana@test.com",
                null,
                1,
                2,
                "ACTIVO",
                true
        );
    }

    private static UpdateFuncionarioRequest validUpdateRequest() {
        return new UpdateFuncionarioRequest(
                "Ana Actualizada",
                null,
                "nueva@test.com",
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @BeforeEach
    void setUp() {
        controller = new FuncionarioController(
                funcionarioRepository,
                createFuncionarioUseCase,
                updateFuncionarioUseCase
        );
    }

    // ── GET /api/v1/funcionarios ──────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with list of funcionarios when no search param")
    void list_shouldReturn200WithFuncionarioList_whenNoSearchParam() {
        // Given
        Funcionario f1 = sampleFuncionario(1);
        Funcionario f2 = sampleFuncionario(2);
        when(funcionarioRepository.findAll(null)).thenReturn(List.of(f1, f2));

        // When
        ResponseEntity<List<FuncionarioResponse>> response = controller.list(null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).id()).isEqualTo(1);
        assertThat(response.getBody().get(1).id()).isEqualTo(2);
        verify(funcionarioRepository).findAll(null);
    }

    @Test
    @DisplayName("Should return 200 with filtered list when search param is provided")
    void list_shouldReturn200WithFilteredList_whenSearchParamProvided() {
        // Given
        Funcionario f = sampleFuncionario(1);
        when(funcionarioRepository.findAll("Ana")).thenReturn(List.of(f));

        // When
        ResponseEntity<List<FuncionarioResponse>> response = controller.list("Ana");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).nombres()).isEqualTo("Ana");

        ArgumentCaptor<String> searchCaptor = ArgumentCaptor.forClass(String.class);
        verify(funcionarioRepository).findAll(searchCaptor.capture());
        assertThat(searchCaptor.getValue()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("Should return 200 with empty list when no funcionarios match")
    void list_shouldReturn200WithEmptyList_whenNoneFound() {
        // Given
        when(funcionarioRepository.findAll(anyString())).thenReturn(List.of());

        // When
        ResponseEntity<List<FuncionarioResponse>> response = controller.list("ZZZZZ");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ── GET /api/v1/funcionarios/{id} ─────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with funcionario detail when id exists")
    void get_shouldReturn200WithDetail_whenFuncionarioExists() {
        // Given
        Funcionario f = sampleFuncionario(1);
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(f));

        // When
        ResponseEntity<FuncionarioResponse> response = controller.get(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1);
        assertThat(response.getBody().nombres()).isEqualTo("Ana");
        assertThat(response.getBody().estadoLaboral()).isEqualTo("ACTIVO");
        assertThat(response.getBody().puedeVotar()).isTrue();
        assertThat(response.getBody().debeCambiarPassword()).isTrue();
        // No password in detail response
        assertThat(response.getBody().temporaryPassword()).isNull();
    }

    @Test
    @DisplayName("Should throw NotFoundException when funcionario id does not exist")
    void get_shouldThrowNotFoundException_whenFuncionarioDoesNotExist() {
        // Given
        when(funcionarioRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> controller.get(999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    // ── POST /api/v1/funcionarios ─────────────────────────────────────────────

    @Test
    @DisplayName("Should return 201 with funcionario and temporaryPassword when create succeeds")
    void create_shouldReturn201WithTemporaryPassword_whenCreateSucceeds() {
        // Given
        Funcionario saved = sampleFuncionario(10);
        CreateFuncionarioUseCase.Result result = new CreateFuncionarioUseCase.Result(saved, "Temp@12345!");
        when(createFuncionarioUseCase.create(any())).thenReturn(result);

        // When
        ResponseEntity<FuncionarioResponse> response = controller.create(validCreateRequest());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(10);
        assertThat(response.getBody().debeCambiarPassword()).isTrue();
        assertThat(response.getBody().temporaryPassword()).isEqualTo("Temp@12345!");
    }

    @Test
    @DisplayName("Should build CreateFuncionarioUseCase.Command with correct fields from request")
    void create_shouldBuildCommandWithCorrectFields_fromRequest() {
        // Given
        Funcionario saved = sampleFuncionario(1);
        CreateFuncionarioUseCase.Result result = new CreateFuncionarioUseCase.Result(saved, "TempPwd");
        when(createFuncionarioUseCase.create(any())).thenReturn(result);

        CreateFuncionarioRequest request = new CreateFuncionarioRequest(
                "María", "López", "CC", "99999999", "maria@test.com",
                "+57 300 111 0000", 3, 5, "ACTIVO", false
        );

        // When
        controller.create(request);

        // Then
        ArgumentCaptor<CreateFuncionarioUseCase.Command> captor =
                ArgumentCaptor.forClass(CreateFuncionarioUseCase.Command.class);
        verify(createFuncionarioUseCase).create(captor.capture());
        CreateFuncionarioUseCase.Command cmd = captor.getValue();
        assertThat(cmd.nombres()).isEqualTo("María");
        assertThat(cmd.apellidos()).isEqualTo("López");
        assertThat(cmd.documentoIdentidad()).isEqualTo("99999999");
        assertThat(cmd.email()).isEqualTo("maria@test.com");
        assertThat(cmd.departamentoId()).isEqualTo(3);
        assertThat(cmd.cargoId()).isEqualTo(5);
        assertThat(cmd.estadoLaboral()).isEqualTo("ACTIVO");
        assertThat(cmd.puedeVotar()).isFalse();
    }

    @Test
    @DisplayName("Should propagate ValidationException (400) when use case throws validation error")
    void create_shouldPropagateValidationException_whenUseCaseThrows400() {
        // Given
        when(createFuncionarioUseCase.create(any()))
                .thenThrow(new ValidationException("nombres es obligatorio"));

        // When & Then
        assertThatThrownBy(() -> controller.create(validCreateRequest()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("nombres");
    }

    @Test
    @DisplayName("Should propagate DomainException (409) when documentoIdentidad is duplicate")
    void create_shouldPropagateDomainException_whenDocumentoIsDuplicate() {
        // Given
        when(createFuncionarioUseCase.create(any()))
                .thenThrow(new DomainException("10000001 ya existe"));

        // When & Then
        assertThatThrownBy(() -> controller.create(validCreateRequest()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("10000001");
    }

    // ── PUT /api/v1/funcionarios/{id} ─────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with updated funcionario when update succeeds")
    void update_shouldReturn200WithUpdatedFuncionario_whenUpdateSucceeds() {
        // Given
        Funcionario updated = new Funcionario(
                1, "EMP-001", "10000001",
                "Ana Actualizada", "García", "CC", "nueva@test.com",
                null, 1, 2, null, true, "ACTIVO", true
        );
        when(updateFuncionarioUseCase.update(any())).thenReturn(updated);

        // When
        ResponseEntity<FuncionarioResponse> response = controller.update(1, validUpdateRequest());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().nombres()).isEqualTo("Ana Actualizada");
        assertThat(response.getBody().email()).isEqualTo("nueva@test.com");
    }

    @Test
    @DisplayName("Should return 200 with puedeVotar=true when toggle enables voting")
    void update_shouldReturn200WithPuedeVotarTrue_whenToggledToTrue() {
        // Given
        Funcionario withVoting = new Funcionario(
                5, "EMP-005", "50000005",
                "Carlos", "Ruiz", "CC", "carlos@test.com",
                null, 1, 2, null, true, "ACTIVO", false
        );
        when(updateFuncionarioUseCase.update(any())).thenReturn(withVoting);

        UpdateFuncionarioRequest toggleRequest = new UpdateFuncionarioRequest(
                null, null, null, null, null, null, null, true, null
        );

        // When
        ResponseEntity<FuncionarioResponse> response = controller.update(5, toggleRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().puedeVotar()).isTrue();
    }

    @Test
    @DisplayName("Should set debeCambiarPassword=false when admin clears flag via PUT")
    void update_shouldSetDebeCambiarPasswordFalse_whenAdminClearsFlag() {
        // Given
        Funcionario passwordChanged = new Funcionario(
                3, "EMP-003", "30000003",
                "Luis", "Torres", "CC", "luis@test.com",
                null, 1, 2, null, true, "ACTIVO", false  // debeCambiarPassword=false
        );
        when(updateFuncionarioUseCase.update(any())).thenReturn(passwordChanged);

        UpdateFuncionarioRequest clearPasswordRequest = new UpdateFuncionarioRequest(
                null, null, null, null, null, null, null, null, false
        );

        // When
        ResponseEntity<FuncionarioResponse> response = controller.update(3, clearPasswordRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().debeCambiarPassword()).isFalse();

        // Verify the command carried debeCambiarPassword=false
        ArgumentCaptor<UpdateFuncionarioUseCase.Command> captor =
                ArgumentCaptor.forClass(UpdateFuncionarioUseCase.Command.class);
        verify(updateFuncionarioUseCase).update(captor.capture());
        assertThat(captor.getValue().debeCambiarPassword()).isFalse();
    }

    @Test
    @DisplayName("Should build UpdateFuncionarioUseCase.Command with id from path variable")
    void update_shouldBuildCommandWithIdFromPathVariable() {
        // Given
        Funcionario updated = sampleFuncionario(7);
        when(updateFuncionarioUseCase.update(any())).thenReturn(updated);

        UpdateFuncionarioRequest request = new UpdateFuncionarioRequest(
                "Nuevo", null, null, null, null, null, "SUSPENDIDO", null, null
        );

        // When
        controller.update(7, request);

        // Then
        ArgumentCaptor<UpdateFuncionarioUseCase.Command> captor =
                ArgumentCaptor.forClass(UpdateFuncionarioUseCase.Command.class);
        verify(updateFuncionarioUseCase).update(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(7);
        assertThat(captor.getValue().nombres()).isEqualTo("Nuevo");
        assertThat(captor.getValue().estadoLaboral()).isEqualTo("SUSPENDIDO");
    }

    @Test
    @DisplayName("Should propagate NotFoundException (404) when funcionario does not exist")
    void update_shouldThrowNotFoundException_whenFuncionarioDoesNotExist() {
        // Given
        when(updateFuncionarioUseCase.update(any()))
                .thenThrow(new NotFoundException("Funcionario 999 no encontrado"));

        // When & Then
        assertThatThrownBy(() -> controller.update(999, validUpdateRequest()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }
}
