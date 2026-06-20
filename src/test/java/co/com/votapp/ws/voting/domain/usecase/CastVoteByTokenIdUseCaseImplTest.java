package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;
import co.com.votapp.ws.audit.domain.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.in.CastVoteByTokenIdPort;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.TokenLockPort;
import co.com.votapp.ws.voting.domain.port.out.VoteRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CastVoteByTokenIdUseCaseImpl} (Task 1.5).
 *
 * <p>RED phase: tests will fail until the use case is created.
 * Validates that the same atomic flow works when the token is resolved by UUID
 * instead of rawToken hash.
 */
@DisplayName("CastVoteByTokenIdUseCaseImpl - Vote by token ID without rawToken")
@ExtendWith(MockitoExtension.class)
class CastVoteByTokenIdUseCaseImplTest {

    @Mock private VotingTokenRepository votingTokenRepository;
    @Mock private TokenLockPort tokenLockPort;
    @Mock private ElectionRepositoryPort electionRepository;
    @Mock private CandidateRepositoryPort candidateRepository;
    @Mock private VoteRepositoryPort voteRepository;
    @Mock private ParticipacionRepositoryPort participacionRepository;
    @Mock private RegisterAuditEventPort auditPort;

    private CastVoteByTokenIdPort useCase;

    @BeforeEach
    void setUp() {
        useCase = new CastVoteByTokenIdUseCaseImpl(
                votingTokenRepository,
                tokenLockPort,
                electionRepository,
                candidateRepository,
                voteRepository,
                participacionRepository,
                auditPort
        );
    }

    @Test
    @DisplayName("Should cast vote successfully when token resolved by ID and all conditions are valid")
    void castVote_shouldExecuteFullAtomicFlow_whenTokenIdIsValid() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        VotingToken token = issuedToken(tokenId, eleccionId, 42L);
        Election election = activaElection(eleccionId);

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(candidatoId, eleccionId))
                .thenReturn(Optional.of(candidate(candidatoId, eleccionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.castVote(tokenId, candidatoId);

        // Then
        verify(voteRepository).save(any());
        verify(participacionRepository).markParticipation(any(), any(), any());
        verify(auditPort).register(any(AuditoriaEvento.class));
        verify(tokenLockPort).release(tokenId);
    }

    @Test
    @DisplayName("Should acquire lock before DB operations and release after in correct order")
    void castVote_shouldAcquireLockBeforeDbAndReleaseLockAfter() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        VotingToken token = issuedToken(tokenId, eleccionId, 42L);
        Election election = activaElection(eleccionId);

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(candidatoId, eleccionId))
                .thenReturn(Optional.of(candidate(candidatoId, eleccionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.castVote(tokenId, candidatoId);

        // Then — lock acquired before DB work, released after
        InOrder order = inOrder(tokenLockPort, electionRepository, voteRepository);
        order.verify(tokenLockPort).acquire(tokenId);
        order.verify(electionRepository).findById(eleccionId);
        order.verify(voteRepository).save(any());
        order.verify(tokenLockPort).release(tokenId);
    }

    @Test
    @DisplayName("Should throw DomainException when token is not found by ID")
    void castVote_shouldThrowDomainException_whenTokenNotFoundById() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.castVote(tokenId, candidatoId))
                .isInstanceOf(DomainException.class);

        verify(tokenLockPort, never()).acquire(any());
    }

    @Test
    @DisplayName("Should throw DomainException when token is not ISSUED (already USED or INVALIDATED)")
    void castVote_shouldThrowDomainException_whenTokenIsNotIssued() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        VotingToken usedToken = new VotingToken(tokenId, eleccionId, 42L, "hash", TokenStatus.USED, Instant.now());
        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(usedToken));

        // When & Then
        assertThatThrownBy(() -> useCase.castVote(tokenId, candidatoId))
                .isInstanceOf(DomainException.class);

        verify(tokenLockPort, never()).acquire(any());
    }

    @Test
    @DisplayName("Should throw DomainException and release lock when election is not ACTIVA")
    void castVote_shouldThrowDomainExceptionAndReleaseLock_whenElectionIsNotActiva() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        VotingToken token = issuedToken(tokenId, eleccionId, 42L);
        Election finalizedElection = new Election(
                eleccionId, "ELEC-TEST", "Test",
                ElectionStatus.FINALIZADA,
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(1)
        );

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(finalizedElection));

        // When & Then
        assertThatThrownBy(() -> useCase.castVote(tokenId, candidatoId))
                .isInstanceOf(DomainException.class);

        verify(tokenLockPort).release(tokenId);
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when lock cannot be acquired")
    void castVote_shouldThrowDomainException_whenLockNotAcquired() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        VotingToken token = issuedToken(tokenId, eleccionId, 42L);
        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> useCase.castVote(tokenId, candidatoId))
                .isInstanceOf(DomainException.class);

        verify(electionRepository, never()).findById(any());
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should register VOTE_ACCEPTED audit event with null funcionarioId (anonymity preserved)")
    void castVote_shouldRegisterVoteAcceptedAuditWithNullFuncionarioId_onSuccess() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        VotingToken token = issuedToken(tokenId, eleccionId, 42L);
        Election election = activaElection(eleccionId);

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(candidatoId, eleccionId))
                .thenReturn(Optional.of(candidate(candidatoId, eleccionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.castVote(tokenId, candidatoId);

        // Then
        ArgumentCaptor<AuditoriaEvento> auditCaptor = ArgumentCaptor.forClass(AuditoriaEvento.class);
        verify(auditPort).register(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getTipo()).isEqualTo("VOTE_ACCEPTED");
        assertThat(auditCaptor.getValue().getEleccionId()).isEqualTo(eleccionId);
        // Anonymity: funcionarioId must be null in the audit event
        assertThat(auditCaptor.getValue().getFuncionarioId()).isNull();
    }

    @Test
    @DisplayName("Should lock is ALWAYS released even when voteRepository throws")
    void castVote_shouldReleaseLock_evenWhenExceptionOccursDuringVoteSave() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();

        VotingToken token = issuedToken(tokenId, eleccionId, 42L);
        Election election = activaElection(eleccionId);

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(candidatoId, eleccionId))
                .thenReturn(Optional.of(candidate(candidatoId, eleccionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);
        doThrow(new RuntimeException("DB error")).when(voteRepository).save(any());

        // When & Then
        assertThatThrownBy(() -> useCase.castVote(tokenId, candidatoId))
                .isInstanceOf(RuntimeException.class);

        // Lock MUST be released even on failure
        verify(tokenLockPort).release(tokenId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private VotingToken issuedToken(UUID tokenId, UUID eleccionId, Long funcionarioId) {
        return new VotingToken(tokenId, eleccionId, funcionarioId, "hash", TokenStatus.ISSUED, Instant.now());
    }

    private Election activaElection(UUID id) {
        return new Election(id, "ELEC-" + id.toString().substring(0, 8), "Eleccion Test",
                ElectionStatus.ACTIVA, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
    }

    private co.com.votapp.ws.electoral.domain.Candidate candidate(UUID id, UUID eleccionId) {
        return new co.com.votapp.ws.electoral.domain.Candidate(id, eleccionId, "Candidato Test", false, false, 1);
    }
}
