package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.application.command.AddCandidateCommand;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AddCandidateUseCaseImpl}.
 *
 * <p>V5: Uses funcionarioId uniqueness instead of numeroOrden uniqueness.
 */
@DisplayName("AddCandidateUseCase - Candidate management business logic")
@ExtendWith(MockitoExtension.class)
class AddCandidateUseCaseTest {

    @Mock
    private ElectionRepositoryPort electionRepository;

    @Mock
    private CandidateRepositoryPort candidateRepository;

    private AddCandidateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AddCandidateUseCaseImpl(electionRepository, candidateRepository);
    }

    @Test
    @DisplayName("Should add candidate when election is PROGRAMADA and funcionarioId is unique")
    void addCandidate_shouldReturnCandidate_whenElectionIsProgramadaAndFuncionarioIsUnique() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var election = electionWith(eleccionId, ElectionStatus.PROGRAMADA);
        var command = new AddCandidateCommand(eleccionId, "Candidato Uno", "Descripcion", 1, null, null, null);

        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.existsByEleccionIdAndFuncionarioId(eleccionId, 1)).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Candidate result = useCase.addCandidate(command);

        // Then
        assertThat(result.eleccionId()).isEqualTo(eleccionId);
        assertThat(result.nombre()).isEqualTo("Candidato Uno");
        assertThat(result.funcionarioId()).isEqualTo(1);
        assertThat(result.esVotoEnBlanco()).isFalse();
    }

    @Test
    @DisplayName("Should add candidate when election is ACTIVA")
    void addCandidate_shouldReturnCandidate_whenElectionIsActiva() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var election = electionWith(eleccionId, ElectionStatus.ACTIVA);
        var command = new AddCandidateCommand(eleccionId, "Candidato Dos", "Descripcion", 2, null, null, null);

        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.existsByEleccionIdAndFuncionarioId(eleccionId, 2)).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Candidate result = useCase.addCandidate(command);

        // Then
        assertThat(result.nombre()).isEqualTo("Candidato Dos");
    }

    @Test
    @DisplayName("Should throw DomainException when election is FINALIZADA")
    void addCandidate_shouldThrowDomainException_whenElectionIsFinalizada() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var election = electionWith(eleccionId, ElectionStatus.FINALIZADA);
        var command = new AddCandidateCommand(eleccionId, "Candidato", "Desc", null, null, null, null);

        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.addCandidate(command))
                .isInstanceOf(DomainException.class);

        verify(candidateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election is CANCELADA")
    void addCandidate_shouldThrowDomainException_whenElectionIsCancelada() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var election = electionWith(eleccionId, ElectionStatus.CANCELADA);
        var command = new AddCandidateCommand(eleccionId, "Candidato", "Desc", null, null, null, null);

        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.addCandidate(command))
                .isInstanceOf(DomainException.class);

        verify(candidateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when funcionarioId is already a candidate in the election")
    void addCandidate_shouldThrowDomainException_whenFuncionarioIdAlreadyExists() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var election = electionWith(eleccionId, ElectionStatus.PROGRAMADA);
        var command = new AddCandidateCommand(eleccionId, "Candidato", "Desc", 5, null, null, null);

        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.existsByEleccionIdAndFuncionarioId(eleccionId, 5)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.addCandidate(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("funcionario");

        verify(candidateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election does not exist")
    void addCandidate_shouldThrowDomainException_whenElectionNotFound() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var command = new AddCandidateCommand(eleccionId, "Candidato", "Desc", null, null, null, null);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.addCandidate(command))
                .isInstanceOf(DomainException.class);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Election electionWith(UUID id, ElectionStatus status) {
        return new Election(
                id,
                "ELEC-TEST-" + id,
                "Eleccion Test",
                status,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30),
                true,
                1
        );
    }
}
