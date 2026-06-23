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
 * Unit tests for {@link AddCandidateUseCaseImpl} after V5 changes:
 * uniqueness enforced by {@code funcionarioId} (not {@code numeroOrden}).
 *
 * <p>RED cycle: written first. Will fail until AddCandidateUseCaseImpl
 * and AddCandidateCommand are updated.
 */
@DisplayName("AddCandidateUseCase - funcionarioId uniqueness (V5)")
@ExtendWith(MockitoExtension.class)
class AddCandidateByFuncionarioIdUseCaseTest {

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
    @DisplayName("Should add candidate when funcionarioId is not already in election")
    void addCandidate_shouldSucceed_whenFuncionarioIdIsUnique() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var election = electionWith(eleccionId, ElectionStatus.PROGRAMADA);
        var command = new AddCandidateCommand(
                eleccionId, "Juan Pérez", "desc",
                7, // funcionarioId
                "https://example.com/photo.jpg", null, null);

        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.existsByEleccionIdAndFuncionarioId(eleccionId, 7)).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Candidate result = useCase.addCandidate(command);

        // Then
        assertThat(result.nombre()).isEqualTo("Juan Pérez");
        assertThat(result.funcionarioId()).isEqualTo(7);
        assertThat(result.eleccionId()).isEqualTo(eleccionId);
    }

    @Test
    @DisplayName("Should throw DomainException with 409 message when funcionarioId already exists in election")
    void addCandidate_shouldThrow409_whenFuncionarioIdIsDuplicate() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var election = electionWith(eleccionId, ElectionStatus.PROGRAMADA);
        var command = new AddCandidateCommand(
                eleccionId, "María García", "desc",
                3, // funcionarioId — already in the election
                null, null, null);

        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.existsByEleccionIdAndFuncionarioId(eleccionId, 3)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.addCandidate(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("funcionario");

        verify(candidateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should save candidate with null funcionarioId when not provided")
    void addCandidate_shouldSaveWithNullFuncionarioId_whenNotProvided() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var election = electionWith(eleccionId, ElectionStatus.PROGRAMADA);
        var command = new AddCandidateCommand(
                eleccionId, "Candidato Sin Funcionario", "desc",
                null, // no funcionarioId
                null, null, null);

        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Candidate result = useCase.addCandidate(command);

        // Then
        assertThat(result.funcionarioId()).isNull();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Election electionWith(UUID id, ElectionStatus status) {
        return new Election(
                id,
                "ELEC-TEST-" + id.toString().substring(0, 8),
                "Eleccion Test",
                status,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30),
                true,
                1
        );
    }
}
