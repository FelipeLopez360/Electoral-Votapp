package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FinalizeElectionUseCase - Election finalization business logic")
@ExtendWith(MockitoExtension.class)
class FinalizeElectionUseCaseTest {

    @Mock
    private ElectionRepositoryPort electionRepository;

    private FinalizeElectionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FinalizeElectionUseCaseImpl(electionRepository);
    }

    @Test
    @DisplayName("Should transition election from ACTIVA to FINALIZADA")
    void finalize_shouldTransitionToFinalizada_whenElectionIsActiva() {
        // Given
        UUID electionId = UUID.randomUUID();
        var election = electionWith(electionId, ElectionStatus.ACTIVA);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(electionRepository.save(any(Election.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.finalize(electionId);

        // Then
        ArgumentCaptor<Election> captor = ArgumentCaptor.forClass(Election.class);
        verify(electionRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ElectionStatus.FINALIZADA);
    }

    @Test
    @DisplayName("Should throw DomainException when election is CANCELADA")
    void finalize_shouldThrowDomainException_whenElectionIsCancelada() {
        // Given
        UUID electionId = UUID.randomUUID();
        var election = electionWith(electionId, ElectionStatus.CANCELADA);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.finalize(electionId))
                .isInstanceOf(DomainException.class);

        verify(electionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election is already FINALIZADA")
    void finalize_shouldThrowDomainException_whenElectionIsAlreadyFinalizada() {
        // Given
        UUID electionId = UUID.randomUUID();
        var election = electionWith(electionId, ElectionStatus.FINALIZADA);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.finalize(electionId))
                .isInstanceOf(DomainException.class);

        verify(electionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election is PROGRAMADA")
    void finalize_shouldThrowDomainException_whenElectionIsProgramada() {
        // Given
        UUID electionId = UUID.randomUUID();
        var election = electionWith(electionId, ElectionStatus.PROGRAMADA);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.finalize(electionId))
                .isInstanceOf(DomainException.class);

        verify(electionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election does not exist")
    void finalize_shouldThrowDomainException_whenElectionNotFound() {
        // Given
        UUID electionId = UUID.randomUUID();
        when(electionRepository.findById(electionId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.finalize(electionId))
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
                LocalDateTime.now().plusDays(30),
                true,
                1
        );
    }
}
