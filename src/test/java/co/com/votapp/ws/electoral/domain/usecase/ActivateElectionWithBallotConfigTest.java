package co.com.votapp.ws.electoral.domain.usecase;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ActivateElectionUseCaseImpl} ballot config behavior.
 *
 * <p>RED phase: validates that the blank vote candidate is conditionally created
 * based on the election's permiteVotoBlanco flag (task 1.5).
 * Null vote creation remains unchanged.
 */
@DisplayName("ActivateElectionUseCase - Ballot config controlled blank vote creation")
@ExtendWith(MockitoExtension.class)
class ActivateElectionWithBallotConfigTest {

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
    @DisplayName("Should create blank vote candidate when permiteVotoBlanco=true")
    void activate_shouldCreateBlankVote_whenPermiteVotoBlancoIsTrue() {
        // Given
        UUID electionId = UUID.randomUUID();
        Election election = electionWith(electionId, true, 1);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(electionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(candidateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.activate(electionId);

        // Then — blank vote + null vote = 2 saves
        ArgumentCaptor<Candidate> captor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository, times(2)).save(captor.capture());
        List<Candidate> saved = captor.getAllValues();
        assertThat(saved).anyMatch(Candidate::esVotoEnBlanco);
        assertThat(saved).anyMatch(Candidate::esVotoNulo);
    }

    @Test
    @DisplayName("Should NOT create blank vote candidate when permiteVotoBlanco=false")
    void activate_shouldNotCreateBlankVote_whenPermiteVotoBlancoIsFalse() {
        // Given
        UUID electionId = UUID.randomUUID();
        Election election = electionWith(electionId, false, 1);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(electionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(candidateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.activate(electionId);

        // Then — only null vote is created (1 save)
        ArgumentCaptor<Candidate> captor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository, times(1)).save(captor.capture());
        Candidate saved = captor.getValue();
        assertThat(saved.esVotoEnBlanco()).isFalse();
        assertThat(saved.esVotoNulo()).isTrue();
    }

    @Test
    @DisplayName("Should always create null vote candidate regardless of permiteVotoBlanco")
    void activate_shouldAlwaysCreateNullVote_regardlessOfPermiteVotoBlanco() {
        // Given
        UUID electionId = UUID.randomUUID();
        Election election = electionWith(electionId, false, 1);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(electionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(candidateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.activate(electionId);

        // Then — null vote is always saved
        ArgumentCaptor<Candidate> captor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(c -> c.nombre().equals("Voto Nulo"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Election electionWith(UUID id, boolean permiteVotoBlanco, int maxVotos) {
        return new Election(
                id,
                "ELEC-" + id.toString().substring(0, 8),
                "Eleccion Test",
                ElectionStatus.PROGRAMADA,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30),
                permiteVotoBlanco,
                maxVotos
        );
    }
}
