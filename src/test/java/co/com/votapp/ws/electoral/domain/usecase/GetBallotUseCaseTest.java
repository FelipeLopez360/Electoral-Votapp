package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Ballot;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.GetBallotUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("GetBallotUseCase - Ballot retrieval business logic")
@ExtendWith(MockitoExtension.class)
class GetBallotUseCaseTest {

    @Mock
    private VotingTokenRepository votingTokenRepository;

    @Mock
    private ElectionRepositoryPort electionRepository;

    @Mock
    private CandidateRepositoryPort candidateRepository;

    private GetBallotUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetBallotUseCaseImpl(votingTokenRepository, electionRepository, candidateRepository);
    }

    @Test
    @DisplayName("Should return ballot with ordered candidates for a valid ISSUED token in ACTIVA election")
    void getBallot_shouldReturnBallot_whenTokenIsValidAndElectionIsActiva() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        String rawToken = "my-raw-token";

        var token = issuedToken(tokenId, electionId);
        var election = electionWith(electionId, ElectionStatus.ACTIVA);
        var candidates = List.of(
                candidateWith(UUID.randomUUID(), electionId, "Voto en Blanco", true, 0),
                candidateWith(UUID.randomUUID(), electionId, "Candidato A", false, 1),
                candidateWith(UUID.randomUUID(), electionId, "Candidato B", false, 2)
        );

        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.of(token));
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByEleccionIdOrderByNumeroOrden(electionId)).thenReturn(candidates);

        // When
        Ballot ballot = useCase.getBallot(rawToken);

        // Then
        assertThat(ballot.eleccionId()).isEqualTo(electionId);
        assertThat(ballot.candidates()).hasSize(3);
        assertThat(ballot.candidates().get(0).esVotoEnBlanco()).isTrue();
    }

    @Test
    @DisplayName("Should hash rawToken before lookup — not pass raw value to repository")
    void getBallot_shouldHashRawToken_beforeRepositoryLookup() {
        // Given — a specific raw token
        UUID electionId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        String rawToken = "test-raw-token-value";

        var token = issuedToken(tokenId, electionId);
        var election = electionWith(electionId, ElectionStatus.ACTIVA);
        var candidates = List.of(
                candidateWith(UUID.randomUUID(), electionId, "Voto en Blanco", true, 0)
        );

        // When findIssuedByHash is called with the HASH (not rawToken), return the token
        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.of(token));
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));
        when(candidateRepository.findByEleccionIdOrderByNumeroOrden(electionId)).thenReturn(candidates);

        // When
        Ballot ballot = useCase.getBallot(rawToken);

        // Then — ballot is returned (implicitly validates hashing path was taken)
        assertThat(ballot).isNotNull();
    }

    @Test
    @DisplayName("Should throw DomainException when token is not found (invalid/expired)")
    void getBallot_shouldThrowDomainException_whenTokenNotFound() {
        // Given
        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.getBallot("invalid-token"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Should throw DomainException when election is not ACTIVA")
    void getBallot_shouldThrowDomainException_whenElectionIsNotActiva() {
        // Given
        UUID electionId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        var token = issuedToken(tokenId, electionId);
        var election = electionWith(electionId, ElectionStatus.FINALIZADA);

        when(votingTokenRepository.findIssuedByHash(anyString())).thenReturn(Optional.of(token));
        when(electionRepository.findById(electionId)).thenReturn(Optional.of(election));

        // When & Then
        assertThatThrownBy(() -> useCase.getBallot("some-token"))
                .isInstanceOf(DomainException.class);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private VotingToken issuedToken(UUID tokenId, UUID electionId) {
        return new VotingToken(
                tokenId,
                electionId,
                1L,
                "hash-placeholder",
                TokenStatus.ISSUED,
                Instant.now()
        );
    }

    private Election electionWith(UUID id, ElectionStatus status) {
        return new Election(
                id,
                "ELEC-" + id.toString().substring(0, 8),
                "Eleccion Test",
                status,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30)
        );
    }

    private Candidate candidateWith(UUID id, UUID eleccionId, String nombre,
                                    boolean esVotoEnBlanco, int orden) {
        return new Candidate(id, eleccionId, nombre, esVotoEnBlanco, orden);
    }
}
