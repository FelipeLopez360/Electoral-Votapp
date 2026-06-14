package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.auth.domain.Funcionario;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.exception.ElectionNotModifiableException;
import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import co.com.votapp.ws.electoral.domain.port.in.ManageCensoUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import co.com.votapp.ws.electoral.domain.model.PageResult;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD RED cycle for {@link ManageCensoUseCaseImpl}.
 *
 * <p>All tests written BEFORE the production implementation.
 * Mock all three output ports: ElectionRepositoryPort, FuncionarioRepositoryPort, CensoRepositoryPort.
 */
@DisplayName("ManageCensoUseCaseImpl - Census management business rules")
@ExtendWith(MockitoExtension.class)
class ManageCensoUseCaseImplTest {

    @Mock
    private ElectionRepositoryPort electionRepository;

    @Mock
    private FuncionarioRepositoryPort funcionarioRepository;

    @Mock
    private CensoRepositoryPort censoRepository;

    private ManageCensoUseCase useCase;

    private static final UUID ELECCION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Integer FUNCIONARIO_ID = 42;
    private static final Integer ADMIN_ID = 1;
    private static final Integer DEPARTAMENTO_ID = 10;

    @BeforeEach
    void setUp() {
        useCase = new ManageCensoUseCaseImpl(electionRepository, funcionarioRepository, censoRepository);
    }

    // ─── addByDepartamento — happy path ──────────────────────────────────────

    @Test
    @DisplayName("Should bulk-add eligible funcionarios from departamento when election is PROGRAMADA")
    void addByDepartamento_shouldAddAll_whenElectionIsProgramada() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        var funcionario1 = activoFuncionario(FUNCIONARIO_ID, DEPARTAMENTO_ID);
        var funcionario2 = activoFuncionario(FUNCIONARIO_ID + 1, DEPARTAMENTO_ID);
        when(funcionarioRepository.findEligibleByDepartamento(DEPARTAMENTO_ID))
                .thenReturn(List.of(funcionario1, funcionario2));

        when(censoRepository.existsByEleccionIdAndFuncionarioId(eq(ELECCION_ID), anyInt())).thenReturn(false);

        // When
        var result = useCase.addByDepartamento(ELECCION_ID, DEPARTAMENTO_ID, ADMIN_ID);

        // Then
        assertThat(result.added()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(0);
        assertThat(result.total()).isEqualTo(2);
        verify(censoRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip already-in-census funcionarios when bulk-adding by departamento")
    void addByDepartamento_shouldSkipDuplicates_whenAlreadyInCensus() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        var funcionario1 = activoFuncionario(FUNCIONARIO_ID, DEPARTAMENTO_ID);
        var funcionario2 = activoFuncionario(FUNCIONARIO_ID + 1, DEPARTAMENTO_ID);
        when(funcionarioRepository.findEligibleByDepartamento(DEPARTAMENTO_ID))
                .thenReturn(List.of(funcionario1, funcionario2));

        // funcionario1 is already in census, funcionario2 is not
        when(censoRepository.existsByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(true);
        when(censoRepository.existsByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID + 1)).thenReturn(false);

        // When
        var result = useCase.addByDepartamento(ELECCION_ID, DEPARTAMENTO_ID, ADMIN_ID);

        // Then
        assertThat(result.added()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(2);
    }

    // ─── addByDepartamento — election status guard ────────────────────────────

    @Test
    @DisplayName("Should reject addByDepartamento when election is ACTIVA")
    void addByDepartamento_shouldThrow_whenElectionIsActiva() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.ACTIVA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.addByDepartamento(ELECCION_ID, DEPARTAMENTO_ID, ADMIN_ID))
                .isInstanceOf(ElectionNotModifiableException.class);

        verify(censoRepository, never()).save(any());
        verify(censoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should reject addByDepartamento when election is FINALIZADA")
    void addByDepartamento_shouldThrow_whenElectionIsFinalizada() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.FINALIZADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.addByDepartamento(ELECCION_ID, DEPARTAMENTO_ID, ADMIN_ID))
                .isInstanceOf(ElectionNotModifiableException.class);
    }

    @Test
    @DisplayName("Should throw DomainException when election not found")
    void addByDepartamento_shouldThrow_whenElectionNotFound() {
        // Given
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.addByDepartamento(ELECCION_ID, DEPARTAMENTO_ID, ADMIN_ID))
                .isInstanceOf(DomainException.class);
    }

    // ─── addByFilters ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should bulk-add using flexible filters when election is PROGRAMADA")
    void addByFilters_shouldAddFiltered_whenElectionIsProgramada() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        var funcionario = activoFuncionario(FUNCIONARIO_ID, DEPARTAMENTO_ID);
        when(funcionarioRepository.findEligibleByFilters(DEPARTAMENTO_ID, "ACTIVO", true))
                .thenReturn(List.of(funcionario));

        when(censoRepository.existsByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(false);

        // When
        var result = useCase.addByFilters(ELECCION_ID, DEPARTAMENTO_ID, "ACTIVO", true, ADMIN_ID);

        // Then
        assertThat(result.added()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);
        verify(censoRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should reject addByFilters when election is ACTIVA")
    void addByFilters_shouldThrow_whenElectionIsActiva() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.ACTIVA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.addByFilters(ELECCION_ID, DEPARTAMENTO_ID, "ACTIVO", true, ADMIN_ID))
                .isInstanceOf(ElectionNotModifiableException.class);
    }

    // ─── addIndividual ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should add eligible funcionario individually when election is PROGRAMADA")
    void addIndividual_shouldSave_whenFuncionarioIsEligible() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        var funcionario = activoFuncionario(FUNCIONARIO_ID, DEPARTAMENTO_ID);
        when(funcionarioRepository.findById(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));

        // When
        useCase.addIndividual(ELECCION_ID, FUNCIONARIO_ID, ADMIN_ID);

        // Then
        ArgumentCaptor<CensoEntry> captor = ArgumentCaptor.forClass(CensoEntry.class);
        verify(censoRepository).save(captor.capture());
        assertThat(captor.getValue().eleccionId()).isEqualTo(ELECCION_ID);
        assertThat(captor.getValue().funcionarioId()).isEqualTo(FUNCIONARIO_ID);
        assertThat(captor.getValue().agregadoPor()).isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("Should reject individual add when funcionario is INACTIVO")
    void addIndividual_shouldThrow_whenFuncionarioIsInactivo() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        var inactivo = inactivoFuncionario(FUNCIONARIO_ID, DEPARTAMENTO_ID);
        when(funcionarioRepository.findById(FUNCIONARIO_ID)).thenReturn(Optional.of(inactivo));

        // When & Then
        assertThatThrownBy(() -> useCase.addIndividual(ELECCION_ID, FUNCIONARIO_ID, ADMIN_ID))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("eligible");

        verify(censoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject individual add when funcionario cannot vote")
    void addIndividual_shouldThrow_whenFuncionarioCannotVote() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        var noPuedeVotar = funcionarioNoPuedeVotar(FUNCIONARIO_ID, DEPARTAMENTO_ID);
        when(funcionarioRepository.findById(FUNCIONARIO_ID)).thenReturn(Optional.of(noPuedeVotar));

        // When & Then
        assertThatThrownBy(() -> useCase.addIndividual(ELECCION_ID, FUNCIONARIO_ID, ADMIN_ID))
                .isInstanceOf(DomainException.class);

        verify(censoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject individual add when funcionario not found")
    void addIndividual_shouldThrow_whenFuncionarioNotFound() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));
        when(funcionarioRepository.findById(FUNCIONARIO_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.addIndividual(ELECCION_ID, FUNCIONARIO_ID, ADMIN_ID))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Should reject individual add when election is ACTIVA")
    void addIndividual_shouldThrow_whenElectionIsActiva() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.ACTIVA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.addIndividual(ELECCION_ID, FUNCIONARIO_ID, ADMIN_ID))
                .isInstanceOf(ElectionNotModifiableException.class);
    }

    // ─── removeIndividual ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should remove funcionario from census when election is PROGRAMADA")
    void removeIndividual_shouldDelete_whenElectionIsProgramada() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        // When
        useCase.removeIndividual(ELECCION_ID, FUNCIONARIO_ID);

        // Then
        verify(censoRepository).deleteByEleccionIdAndFuncionarioId(ELECCION_ID, FUNCIONARIO_ID);
    }

    @Test
    @DisplayName("Should reject removal when election is ACTIVA")
    void removeIndividual_shouldThrow_whenElectionIsActiva() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.ACTIVA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.removeIndividual(ELECCION_ID, FUNCIONARIO_ID))
                .isInstanceOf(ElectionNotModifiableException.class);

        verify(censoRepository, never()).deleteByEleccionIdAndFuncionarioId(any(), any());
    }

    // ─── clearCenso ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should clear all census entries when election is PROGRAMADA")
    void clearCenso_shouldDeleteAll_whenElectionIsProgramada() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        // When
        useCase.clearCenso(ELECCION_ID);

        // Then
        verify(censoRepository).deleteAllByEleccionId(ELECCION_ID);
    }

    @Test
    @DisplayName("Should reject clearCenso when election is ACTIVA")
    void clearCenso_shouldThrow_whenElectionIsActiva() {
        // Given
        var election = electionWith(ELECCION_ID, ElectionStatus.ACTIVA);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.clearCenso(ELECCION_ID))
                .isInstanceOf(ElectionNotModifiableException.class);

        verify(censoRepository, never()).deleteAllByEleccionId(any());
    }

    // ─── listCenso ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return paginated census list")
    void listCenso_shouldDelegate_toCensoRepository() {
        // Given
        var entry = new CensoEntry(UUID.randomUUID(), ELECCION_ID, FUNCIONARIO_ID, ADMIN_ID, Instant.now());
        var page = new PageResult<>(List.of(entry), 0, 10, 1L, 1);
        when(censoRepository.findByEleccionId(ELECCION_ID, 0, 10)).thenReturn(page);

        // When
        var result = useCase.listCenso(ELECCION_ID, 0, 10);

        // Then
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).eleccionId()).isEqualTo(ELECCION_ID);
    }

    // ─── countCenso ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return count of census entries")
    void countCenso_shouldDelegate_toCensoRepository() {
        // Given
        when(censoRepository.countByEleccionId(ELECCION_ID)).thenReturn(7L);

        // When
        var result = useCase.countCenso(ELECCION_ID);

        // Then
        assertThat(result).isEqualTo(7L);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Election electionWith(UUID id, ElectionStatus status) {
        return new Election(
                id,
                "ELEC-" + id.toString().substring(0, 8),
                "Eleccion Test",
                status,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30)
        );
    }

    private Funcionario activoFuncionario(Integer id, Integer departamentoId) {
        return new Funcionario(
                id, "EMP%06d".formatted(id), "DOC" + id,
                "Nombre", "Apellido", "CC",
                "email" + id + "@test.com", "123456789",
                departamentoId, 1,
                java.time.LocalDate.now(), true, "ACTIVO", false
        );
    }

    private Funcionario inactivoFuncionario(Integer id, Integer departamentoId) {
        return new Funcionario(
                id, "EMP%06d".formatted(id), "DOC" + id,
                "Nombre", "Apellido", "CC",
                "email" + id + "@test.com", "123456789",
                departamentoId, 1,
                java.time.LocalDate.now(), true, "INACTIVO", false
        );
    }

    private Funcionario funcionarioNoPuedeVotar(Integer id, Integer departamentoId) {
        return new Funcionario(
                id, "EMP%06d".formatted(id), "DOC" + id,
                "Nombre", "Apellido", "CC",
                "email" + id + "@test.com", "123456789",
                departamentoId, 1,
                java.time.LocalDate.now(), false, "ACTIVO", false
        );
    }
}
