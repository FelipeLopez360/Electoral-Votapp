package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.audit.domain.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.Vote;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for the updated {@link CastVoteByTokenIdUseCaseImpl} — multi-candidate voting.
 *
 * <p>RED phase: validates the new List-based castVote contract with:
 * - Deduplication of candidate IDs
 * - Count validation against maxVotosPorElector
 * - Blank vote mutual exclusivity
 * - N Vote rows inserted (one per candidate)
 */
@DisplayName("CastVoteByTokenIdUseCaseImpl - Multi-candidate voting")
@ExtendWith(MockitoExtension.class)
class CastVoteMultiCandidateTest {

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
                votingTokenRepository, tokenLockPort, electionRepository,
                candidateRepository, voteRepository, participacionRepository, auditPort
        );
    }

    @Test
    @DisplayName("Should accept a single candidate vote within limit (maxVotosPorElector=1)")
    void castVote_shouldAcceptSingleCandidate_whenWithinLimit() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID candidatoId = UUID.randomUUID();
        Election election = electionWith(eleccionId, true, 1);

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(issuedToken(tokenId, eleccionId)));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(candidatoId, eleccionId))
                .thenReturn(Optional.of(regularCandidate(candidatoId, eleccionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.castVote(tokenId, List.of(candidatoId));

        // Then — exactly one vote row
        verify(voteRepository, times(1)).save(any());
        verify(tokenLockPort).release(tokenId);
    }

    @Test
    @DisplayName("Should insert N vote rows when multiple candidates are selected within limit")
    void castVote_shouldInsertNVoteRows_whenMultipleCandidatesWithinLimit() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID cand1 = UUID.randomUUID();
        UUID cand2 = UUID.randomUUID();
        UUID cand3 = UUID.randomUUID();
        Election election = electionWith(eleccionId, true, 3);

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(issuedToken(tokenId, eleccionId)));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(cand1, eleccionId)).thenReturn(Optional.of(regularCandidate(cand1, eleccionId)));
        when(candidateRepository.findByIdAndEleccionId(cand2, eleccionId)).thenReturn(Optional.of(regularCandidate(cand2, eleccionId)));
        when(candidateRepository.findByIdAndEleccionId(cand3, eleccionId)).thenReturn(Optional.of(regularCandidate(cand3, eleccionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.castVote(tokenId, List.of(cand1, cand2, cand3));

        // Then — 3 vote rows inserted
        ArgumentCaptor<Vote> captor = ArgumentCaptor.forClass(Vote.class);
        verify(voteRepository, times(3)).save(captor.capture());
        List<Vote> votes = captor.getAllValues();
        assertThat(votes).extracting(Vote::getCandidateId).containsExactlyInAnyOrder(cand1, cand2, cand3);
    }

    @Test
    @DisplayName("Should reject vote when candidatoIds count exceeds maxVotosPorElector")
    void castVote_shouldRejectVote_whenCountExceedsMaxVotos() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID cand1 = UUID.randomUUID();
        UUID cand2 = UUID.randomUUID();
        Election election = electionWith(eleccionId, true, 1);  // max=1

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(issuedToken(tokenId, eleccionId)));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));

        // When & Then — 2 candidates but max=1
        assertThatThrownBy(() -> useCase.castVote(tokenId, List.of(cand1, cand2)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("maxVotosPorElector");

        verify(voteRepository, never()).save(any());
        verify(tokenLockPort).release(tokenId);
    }

    @Test
    @DisplayName("Should deduplicate candidate IDs before count check and vote insertion")
    void castVote_shouldDeduplicateCandidateIds_beforeCountAndInsert() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID cand1 = UUID.randomUUID();
        Election election = electionWith(eleccionId, true, 1);  // max=1

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(issuedToken(tokenId, eleccionId)));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(cand1, eleccionId)).thenReturn(Optional.of(regularCandidate(cand1, eleccionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When — duplicate IDs; after dedupe: 1 distinct candidate, within max=1
        useCase.castVote(tokenId, List.of(cand1, cand1, cand1));

        // Then — only 1 vote row (deduped)
        verify(voteRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should reject mixed ballot when blank vote candidate is included with other candidates")
    void castVote_shouldRejectMixedBallot_whenBlankVoteIsMixedWithOtherCandidates() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID blankId = UUID.randomUUID();
        UUID regularId = UUID.randomUUID();
        Election election = electionWith(eleccionId, true, 2);

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(issuedToken(tokenId, eleccionId)));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(blankId, eleccionId))
                .thenReturn(Optional.of(blankVoteCandidate(blankId, eleccionId)));
        when(candidateRepository.findByIdAndEleccionId(regularId, eleccionId))
                .thenReturn(Optional.of(regularCandidate(regularId, eleccionId)));

        // When & Then — blank + regular candidate is forbidden
        assertThatThrownBy(() -> useCase.castVote(tokenId, List.of(blankId, regularId)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("blank");

        verify(voteRepository, never()).save(any());
        verify(tokenLockPort).release(tokenId);
    }

    @Test
    @DisplayName("Should accept blank vote when submitted alone")
    void castVote_shouldAcceptBlankVoteAlone_whenPermitido() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID blankId = UUID.randomUUID();
        Election election = electionWith(eleccionId, true, 1);

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(issuedToken(tokenId, eleccionId)));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(blankId, eleccionId))
                .thenReturn(Optional.of(blankVoteCandidate(blankId, eleccionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.castVote(tokenId, List.of(blankId));

        // Then — accepted
        verify(voteRepository, times(1)).save(any());
        verify(tokenLockPort).release(tokenId);
    }

    @Test
    @DisplayName("Should release lock even when validation throws after lock acquired")
    void castVote_shouldReleaseLock_whenValidationThrowsAfterLockAcquired() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID cand1 = UUID.randomUUID();
        UUID cand2 = UUID.randomUUID();
        Election election = electionWith(eleccionId, true, 1);  // max=1

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(issuedToken(tokenId, eleccionId)));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.castVote(tokenId, List.of(cand1, cand2)))
                .isInstanceOf(DomainException.class);

        // Lock MUST be released even on validation failure
        verify(tokenLockPort).release(tokenId);
    }

    @Test
    @DisplayName("Should mark token as used exactly once regardless of candidate count")
    void castVote_shouldMarkTokenUsedOnce_forMultiCandidateVote() {
        // Given
        UUID tokenId = UUID.randomUUID();
        UUID eleccionId = UUID.randomUUID();
        UUID cand1 = UUID.randomUUID();
        UUID cand2 = UUID.randomUUID();
        Election election = electionWith(eleccionId, true, 2);

        when(votingTokenRepository.findById(tokenId)).thenReturn(Optional.of(issuedToken(tokenId, eleccionId)));
        when(tokenLockPort.acquire(tokenId)).thenReturn(true);
        when(electionRepository.findById(eleccionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByIdAndEleccionId(cand1, eleccionId)).thenReturn(Optional.of(regularCandidate(cand1, eleccionId)));
        when(candidateRepository.findByIdAndEleccionId(cand2, eleccionId)).thenReturn(Optional.of(regularCandidate(cand2, eleccionId)));
        when(votingTokenRepository.markUsed(any(), any(), any(), any())).thenReturn(true);

        // When
        useCase.castVote(tokenId, List.of(cand1, cand2));

        // Then — markUsed called exactly once, not per candidate
        verify(votingTokenRepository, times(1)).markUsed(eq(tokenId), any(), any(), any());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private VotingToken issuedToken(UUID tokenId, UUID eleccionId) {
        return new VotingToken(tokenId, eleccionId, 42L, "hash", TokenStatus.ISSUED, Instant.now());
    }

    private Election electionWith(UUID id, boolean permiteVotoBlanco, int maxVotos) {
        return new Election(
                id, "ELEC-" + id.toString().substring(0, 8), "Eleccion Test",
                ElectionStatus.ACTIVA,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30),
                permiteVotoBlanco, maxVotos
        );
    }

    private Candidate regularCandidate(UUID id, UUID eleccionId) {
        return new Candidate(id, eleccionId, "Regular Candidate", false, false, 1,
                null, null, null, null);
    }

    private Candidate blankVoteCandidate(UUID id, UUID eleccionId) {
        return new Candidate(id, eleccionId, "Voto en Blanco", true, false, 0,
                null, null, null, null);
    }
}
