package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ActivateElectionUseCase - Election activation business logic")
@ExtendWith(MockitoExtension.class)
class ActivateElectionUseCaseTest {

    @Mock
    private ElectionRepositoryPort electionRepository;

    @Mock
    private CandidateRepositoryPort candidateRepository;

    private ActivateElectionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ActivateElectionUseCaseImpl(electionRepository, candidateRepository);
    }

    @Test
    @DisplayName("Should transition election from PROGRAMADA to ACTIVA")
    void activate_shouldTransitionToActiva_whenElectionIsProgramada() {
        // Given
        UUID electionId = UUID.randomUUID();
        var election = electionWith(electionId, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(electionRepository.save(any(Election.class))).thenAnswer(inv -> inv.getArgument(0));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.activate(electionId);

        // Then
        ArgumentCaptor<Election> captor = ArgumentCaptor.forClass(Election.class);
        verify(electionRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ElectionStatus.ACTIVA);
    }

    @Test
    @DisplayName("Should auto-create synthetic Voto en Blanco candidate with numero_orden=0")
    void activate_shouldCreateBlankVoteCandidate_whenElectionIsProgramada() {
        // Given
        UUID electionId = UUID.randomUUID();
        var election = electionWith(electionId, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(electionRepository.save(any(Election.class))).thenAnswer(inv -> inv.getArgument(0));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.activate(electionId);

        // Then — two synthetic candidates are saved (blank + null)
        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository, times(2)).save(candidateCaptor.capture());
        List<Candidate> savedCandidates = candidateCaptor.getAllValues();
        Candidate blankVote = savedCandidates.stream()
                .filter(Candidate::esVotoEnBlanco)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No blank vote candidate saved"));
        assertThat(blankVote.nombre()).isEqualTo("Voto en Blanco");
        assertThat(blankVote.numeroOrden()).isEqualTo(0);
        assertThat(blankVote.eleccionId()).isEqualTo(electionId);
    }

    @Test
    @DisplayName("Should auto-create synthetic Voto Nulo candidate with numero_orden=-1 alongside blank vote")
    void activate_shouldCreateNullVoteCandidate_whenElectionIsProgramada() {
        // Given
        UUID electionId = UUID.randomUUID();
        var election = electionWith(electionId, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(electionRepository.save(any(Election.class))).thenAnswer(inv -> inv.getArgument(0));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.activate(electionId);

        // Then
        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository, times(2)).save(candidateCaptor.capture());
        List<Candidate> savedCandidates = candidateCaptor.getAllValues();
        Candidate nullVote = savedCandidates.stream()
                .filter(Candidate::esVotoNulo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No null vote candidate saved"));
        assertThat(nullVote.nombre()).isEqualTo("Voto Nulo");
        assertThat(nullVote.esVotoNulo()).isTrue();
        assertThat(nullVote.esVotoEnBlanco()).isFalse();
        assertThat(nullVote.eleccionId()).isEqualTo(electionId);
    }

    @Test
    @DisplayName("Should throw DomainException when election is already ACTIVA")
    void activate_shouldThrowDomainException_whenElectionIsAlreadyActiva() {
        // Given
        UUID electionId = UUID.randomUUID();
        var election = electionWith(electionId, ElectionStatus.ACTIVA);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.activate(electionId))
                .isInstanceOf(DomainException.class);

        verify(electionRepository, never()).save(any());
        verify(candidateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election is FINALIZADA")
    void activate_shouldThrowDomainException_whenElectionIsFinalizada() {
        // Given
        UUID electionId = UUID.randomUUID();
        var election = electionWith(electionId, ElectionStatus.FINALIZADA);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.activate(electionId))
                .isInstanceOf(DomainException.class);

        verify(electionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election does not exist")
    void activate_shouldThrowDomainException_whenElectionNotFound() {
        // Given
        UUID electionId = UUID.randomUUID();
        when(electionRepository.findById(electionId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.activate(electionId))
                .isInstanceOf(DomainException.class);
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
}
