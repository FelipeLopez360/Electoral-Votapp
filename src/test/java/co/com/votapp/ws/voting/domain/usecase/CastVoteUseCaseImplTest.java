package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;
import co.com.votapp.ws.audit.domain.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.application.command.CastVoteCommand;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.in.CastVoteUseCase;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CastVoteUseCaseImpl - Atomic vote casting business logic")
@ExtendWith(MockitoExtension.class)
class CastVoteUseCaseImplTest {

    @Mock
    private VotingTokenRepository votingTokenRepository;

    @Mock
    private TokenLockPort tokenLockPort;

    @Mock
    private ElectionRepositoryPort electionRepository;

    @Mock
    private CandidateRepositoryPort candidateRepository;

    @Mock
    private VoteRepositoryPort voteRepository;

    @Mock
    private ParticipacionRepositoryPort participacionRepository;

    @Mock
    private RegisterAuditEventPort auditPort;

    private CastVoteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CastVoteUseCaseImpl(
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
    @DisplayName("Should cast vote successfully in the correct sequence: hash → find token → lock → revalidate → mark → vote → participation → audit → release")
    void cast_shouldExecuteFullAtomicFlow_whenAllConditionsAreValid() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        String rawToken = "valid-raw-token";

        var token = issuedToken(tokenId, electionId, 42L);
        var election = activaElection(electionId);
        var command = new CastVoteCommand(rawToken, candidatoId);

        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(candidatoId, electionId))
                .thenReturn(Optional.of(candidate(candidatoId, electionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.cast(command);

        // Then — verify all side effects happened
        verify(voteRepository).save(any());
        verify(participacionRepository).markParticipation(any(), any(), any());
        verify(auditPort).register(any(AuditoriaEvento.class));
        verify(tokenLockPort).release(tokenId);
    }

    @Test
    @DisplayName("Should acquire lock before any DB operations and release after")
    void cast_shouldAcquireLockBeforeDbAndReleaseLockAfter() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        var token = issuedToken(tokenId, electionId, 42L);
        var election = activaElection(electionId);
        var command = new CastVoteCommand("rawToken", candidatoId);

        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(candidatoId, electionId))
                .thenReturn(Optional.of(candidate(candidatoId, electionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.cast(command);

        // Then — lock acquired before DB work, released after
        InOrder order = inOrder(tokenLockPort, electionRepository, voteRepository);
        order.verify(tokenLockPort).acquire(tokenId);
        order.verify(electionRepository).findById(electionId);
        order.verify(voteRepository).save(any());
        order.verify(tokenLockPort).release(tokenId);
    }

    @Test
    @DisplayName("Should throw DomainException (409) when token is already USED (markUsed returns false)")
    void cast_shouldThrowDomainException_whenTokenAlreadyUsed() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        var token = issuedToken(tokenId, electionId, 42L);
        var election = activaElection(electionId);
        var command = new CastVoteCommand("rawToken", candidatoId);

        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(candidatoId, electionId))
                .thenReturn(Optional.of(candidate(candidatoId, electionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(false); // already used

        // When & Then
        assertThatThrownBy(() -> useCase.cast(command))
                .isInstanceOf(DomainException.class);

        // Lock MUST be released even on failure
        verify(tokenLockPort).release(tokenId);
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when token lock cannot be acquired")
    void cast_shouldThrowDomainException_whenLockNotAcquired() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        var token = issuedToken(tokenId, electionId, 42L);
        var command = new CastVoteCommand("rawToken", candidatoId);

        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> useCase.cast(command))
                .isInstanceOf(DomainException.class);

        // No DB operations should happen if lock was not acquired
        verify(electionRepository, never()).findById(any());
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election is not ACTIVA (revalidation)")
    void cast_shouldThrowDomainExceptionAndReleaseLock_whenElectionIsNotActiva() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        var token = issuedToken(tokenId, electionId, 42L);
        var finalizedElection = new Election(
                electionId, "ELEC-TEST", "Test",
                ElectionStatus.FINALIZADA,
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(1)
        );
        var command = new CastVoteCommand("rawToken", candidatoId);

        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(finalizedElection));

        // When & Then
        assertThatThrownBy(() -> useCase.cast(command))
                .isInstanceOf(DomainException.class);

        // Lock MUST be released even on revalidation failure
        verify(tokenLockPort).release(tokenId);
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DomainException when token is not found (invalid or already used)")
    void cast_shouldThrowDomainException_whenTokenNotFound() {
        // Given
        UUID candidatoId = UUID.randomUUID();
        var command = new CastVoteCommand("invalid-token", candidatoId);

        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.cast(command))
                .isInstanceOf(DomainException.class);

        verify(tokenLockPort, never()).acquire(any());
    }

    @Test
    @DisplayName("Should register VOTE_ACCEPTED audit event on successful cast")
    void cast_shouldRegisterVoteAcceptedAudit_whenCastSucceeds() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        var token = issuedToken(tokenId, electionId, 42L);
        var election = activaElection(electionId);
        var command = new CastVoteCommand("rawToken", candidatoId);

        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.of(token));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(candidatoId, electionId))
                .thenReturn(Optional.of(candidate(candidatoId, electionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.cast(command);

        // Then
        ArgumentCaptor<AuditoriaEvento> auditCaptor = ArgumentCaptor.forClass(AuditoriaEvento.class);
        verify(auditPort).register(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getTipo()).isEqualTo("VOTE_ACCEPTED");
        assertThat(auditCaptor.getValue().getEleccionId()).isEqualTo(electionId);
        // anonymity: funcionario_id must NOT be in the audit event for VOTE_ACCEPTED
        assertThat(auditCaptor.getValue().getFuncionarioId()).isNull();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private VotingToken issuedToken(UUID tokenId, UUID electionId, Long funcionarioId) {
        return new VotingToken(
                tokenId,
                electionId,
                funcionarioId,
                "hash-placeholder",
                TokenStatus.ISSUED,
                Instant.now()
        );
    }

    private Election activaElection(UUID id) {
        return new Election(
                id,
                "ELEC-" + id.toString().substring(0, 8),
                "Eleccion Test",
                ElectionStatus.ACTIVA,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
    }

    private co.com.votapp.ws.electoral.domain.Candidate candidate(UUID id, UUID eleccionId) {
        return new co.com.votapp.ws.electoral.domain.Candidate(
                id, eleccionId, "Candidato Test", false, 1
        );
    }
}
