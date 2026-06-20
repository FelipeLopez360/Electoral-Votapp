package co.com.votapp.ws.electoral.infrastructure.adapter.in.web;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.application.dto.BulkAddCensoRequest;
import co.com.votapp.ws.electoral.application.dto.BulkAddCensoResponse;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.exception.ElectionNotModifiableException;
import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import co.com.votapp.ws.common.domain.model.PageResult;
import co.com.votapp.ws.electoral.domain.port.in.ManageCensoUseCase;
import co.com.votapp.ws.organization.domain.Departamento;
import co.com.votapp.ws.organization.domain.port.out.DepartamentoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CensoController}.
 *
 * <p>No Spring context — all collaborators are mocked via Mockito.
 * Tests cover HTTP status mapping, argument delegation, and response projection.
 */
@DisplayName("CensoController - Census REST adapter")
@ExtendWith(MockitoExtension.class)
class CensoControllerTest {

    @Mock
    private ManageCensoUseCase manageCensoUseCase;

    @Mock
    private FuncionarioRepositoryPort funcionarioRepository;

    @Mock
    private DepartamentoRepositoryPort departamentoRepository;

    private CensoController controller;

    private static final UUID ELECTION_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final Integer FUNCIONARIO_ID = 42;
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        controller = new CensoController(manageCensoUseCase, funcionarioRepository, departamentoRepository);
    }

    // ── POST /bulk ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with bulk result when bulk add succeeds")
    void bulkAdd_shouldReturn200WithResult_whenElectionIsProgramada() {
        // Given
        var request = new BulkAddCensoRequest(1, "ACTIVO", true);
        when(manageCensoUseCase.addByFilters(ELECTION_ID, 1, "ACTIVO", true, null))
                .thenReturn(new ManageCensoUseCase.BulkAddResult(5, 2, 7));

        // When
        ResponseEntity<BulkAddCensoResponse> response = controller.bulkAdd(ELECTION_ID, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().added()).isEqualTo(5);
        assertThat(response.getBody().skipped()).isEqualTo(2);
        assertThat(response.getBody().total()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should propagate ElectionNotModifiableException when election is not PROGRAMADA")
    void bulkAdd_shouldPropagateException_whenElectionNotProgramada() {
        // Given
        var request = new BulkAddCensoRequest(1, "ACTIVO", true);
        doThrow(new ElectionNotModifiableException(ELECTION_ID, ElectionStatus.ACTIVA))
                .when(manageCensoUseCase).addByFilters(any(), anyInt(), any(), any(), any());

        // When & Then
        assertThatThrownBy(() -> controller.bulkAdd(ELECTION_ID, request))
                .isInstanceOf(ElectionNotModifiableException.class);
    }

    @Test
    @DisplayName("Should apply ACTIVO default when estadoLaboral is null in request")
    void bulkAdd_shouldDefaultToActivo_whenEstadoLaboralIsNull() {
        // Given
        var request = new BulkAddCensoRequest(3, null, null);
        when(manageCensoUseCase.addByFilters(ELECTION_ID, 3, "ACTIVO", Boolean.TRUE, null))
                .thenReturn(new ManageCensoUseCase.BulkAddResult(2, 0, 2));

        // When
        controller.bulkAdd(ELECTION_ID, request);

        // Then
        verify(manageCensoUseCase).addByFilters(
                eq(ELECTION_ID),
                eq(3),
                eq("ACTIVO"),
                eq(Boolean.TRUE),
                eq(null)
        );
    }

    // ── POST /{funcionarioId} ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 201 when individual add succeeds")
    void addIndividual_shouldReturn201_whenSucceeds() {
        // Given
        doNothing().when(manageCensoUseCase).addIndividual(ELECTION_ID, FUNCIONARIO_ID, null);

        // When
        ResponseEntity<Void> response = controller.addIndividual(ELECTION_ID, FUNCIONARIO_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(manageCensoUseCase).addIndividual(ELECTION_ID, FUNCIONARIO_ID, null);
    }

    @Test
    @DisplayName("Should propagate DomainException when funcionario is not eligible")
    void addIndividual_shouldPropagateException_whenFuncionarioNotEligible() {
        // Given
        doThrow(new DomainException("Funcionario 42 is not eligible"))
                .when(manageCensoUseCase).addIndividual(ELECTION_ID, FUNCIONARIO_ID, null);

        // When & Then
        assertThatThrownBy(() -> controller.addIndividual(ELECTION_ID, FUNCIONARIO_ID))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("not eligible");
    }

    // ── DELETE /{funcionarioId} ───────────────────────────────────────────────

    @Test
    @DisplayName("Should return 204 when individual remove succeeds")
    void removeIndividual_shouldReturn204_whenSucceeds() {
        // Given
        doNothing().when(manageCensoUseCase).removeIndividual(ELECTION_ID, FUNCIONARIO_ID);

        // When
        ResponseEntity<Void> response = controller.removeIndividual(ELECTION_ID, FUNCIONARIO_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(manageCensoUseCase).removeIndividual(ELECTION_ID, FUNCIONARIO_ID);
    }

    // ── DELETE / (clearCenso) ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 204 when census is cleared successfully")
    void clearCenso_shouldReturn204_whenSucceeds() {
        // Given
        doNothing().when(manageCensoUseCase).clearCenso(ELECTION_ID);

        // When
        ResponseEntity<Void> response = controller.clearCenso(ELECTION_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(manageCensoUseCase).clearCenso(ELECTION_ID);
    }

    @Test
    @DisplayName("Should propagate ElectionNotModifiableException when clearing non-PROGRAMADA election")
    void clearCenso_shouldPropagateException_whenElectionNotProgramada() {
        // Given
        doThrow(new ElectionNotModifiableException(ELECTION_ID, ElectionStatus.FINALIZADA))
                .when(manageCensoUseCase).clearCenso(ELECTION_ID);

        // When & Then
        assertThatThrownBy(() -> controller.clearCenso(ELECTION_ID))
                .isInstanceOf(ElectionNotModifiableException.class);
    }

    // ── GET / (listCenso) ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with enriched paginated census list")
    void listCenso_shouldReturn200WithEnrichedPage_whenCensusExists() {
        // Given
        CensoEntry entry = new CensoEntry(UUID.randomUUID(), ELECTION_ID, FUNCIONARIO_ID, null, NOW);
        var page = new PageResult<>(List.of(entry), 0, 20, 1L, 1);
        when(manageCensoUseCase.listCenso(ELECTION_ID, 0, 20)).thenReturn(page);

        var funcionario = new Funcionario(FUNCIONARIO_ID, "EMP042", "12345678",
                "María", "García", "CC", "maria@test.com", null,
                2, 4, null, true, "ACTIVO", false);
        when(funcionarioRepository.findById(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));

        var dept = new Departamento(2, "SISTEMAS", "Sistemas", true);
        when(departamentoRepository.findAllByActivoTrue()).thenReturn(List.of(dept));

        // When
        @SuppressWarnings("unchecked")
        ResponseEntity<PageResult<co.com.votapp.ws.electoral.application.dto.CensoEntryResponse>> response =
                (ResponseEntity<PageResult<co.com.votapp.ws.electoral.application.dto.CensoEntryResponse>>)
                        (ResponseEntity<?>) controller.listCenso(ELECTION_ID, 0, 20);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        var enriched = response.getBody().content().get(0);
        assertThat(enriched.nombres()).isEqualTo("María");
        assertThat(enriched.apellidos()).isEqualTo("García");
        assertThat(enriched.numeroEmpleado()).isEqualTo("EMP042");
        assertThat(enriched.departamentoNombre()).isEqualTo("Sistemas");
    }

    // ── GET /count ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with count when census exists")
    void countCenso_shouldReturn200WithCount_whenCensusExists() {
        // Given
        when(manageCensoUseCase.countCenso(ELECTION_ID)).thenReturn(17L);

        // When
        ResponseEntity<CensoController.CountResponse> response = controller.countCenso(ELECTION_ID);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().count()).isEqualTo(17L);
    }

    // ── Delegation verification ───────────────────────────────────────────────

    @Test
    @DisplayName("Should delegate addByFilters with correct arguments")
    void bulkAdd_shouldDelegateToUseCase_withCorrectArguments() {
        // Given
        var request = new BulkAddCensoRequest(5, "ACTIVO", true);
        when(manageCensoUseCase.addByFilters(ELECTION_ID, 5, "ACTIVO", true, null))
                .thenReturn(new ManageCensoUseCase.BulkAddResult(3, 1, 4));

        // When
        controller.bulkAdd(ELECTION_ID, request);

        // Then
        verify(manageCensoUseCase).addByFilters(
                eq(ELECTION_ID),
                eq(5),
                eq("ACTIVO"),
                eq(true),
                eq(null)
        );
    }
}
