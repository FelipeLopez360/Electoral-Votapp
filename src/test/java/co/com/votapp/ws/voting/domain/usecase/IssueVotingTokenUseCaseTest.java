package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.application.command.IssueVotingTokenCommand;
import co.com.votapp.ws.voting.domain.IssuedVotingToken;
import co.com.votapp.ws.voting.domain.TokenStatus;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.in.IssueVotingTokenUseCase;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import co.com.votapp.ws.votereligibility.domain.port.out.VoterEligibilityRepositoryPort;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IssueVotingTokenUseCase - Token issuance business logic")
@ExtendWith(MockitoExtension.class)
class IssueVotingTokenUseCaseTest {

    @Mock
    private VotingTokenRepository votingTokenRepository;

    @Mock
    private VoterEligibilityRepositoryPort eligibilityRepository;

    @Mock
    private ParticipacionRepositoryPort participacionRepository;

    @Mock
    private ElectionRepositoryPort electionRepository;

    private IssueVotingTokenUseCase useCase;

    private static final UUID ELECCION_ID = UUID.randomUUID();
    private static final Long FUNCIONARIO_ID = 42L;

    @BeforeEach
    void setUp() {
        useCase = new IssueVotingTokenUseCaseImpl(
                votingTokenRepository, eligibilityRepository, participacionRepository, electionRepository);
    }

    // ─── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return IssuedVotingToken with rawToken and tokenId when all conditions are met")
    void issue_shouldReturnIssuedToken_whenFuncionarioIsInCensusAndElectionIsActiva() {
        // Given
        var command = new IssueVotingTokenCommand(FUNCIONARIO_ID, ELECCION_ID);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(activaElection()));
        when(eligibilityRepository.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(false);
        when(participacionRepository.hasParticipated(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(false);
        when(votingTokenRepository.saveIssued(any(VotingToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        IssuedVotingToken result = useCase.issue(command);

        // Then
        assertThat(result.rawToken()).isNotBlank();
        assertThat(result.tokenId()).isNotNull();
    }

    @Test
    @DisplayName("Should NOT persist rawToken — only the SHA-256 hash is saved")
    void issue_shouldPersistHashNotRawToken_whenTokenIsIssued() {
        // Given
        var command = new IssueVotingTokenCommand(FUNCIONARIO_ID, ELECCION_ID);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(activaElection()));
        when(eligibilityRepository.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(false);
        when(participacionRepository.hasParticipated(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(false);
        when(votingTokenRepository.saveIssued(any(VotingToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        IssuedVotingToken issued = useCase.issue(command);

        // Then — the persisted token hash must NOT equal the rawToken
        ArgumentCaptor<VotingToken> captor = ArgumentCaptor.forClass(VotingToken.class);
        verify(votingTokenRepository).saveIssued(captor.capture());
        VotingToken persisted = captor.getValue();

        assertThat(persisted.tokenHash()).isNotEqualTo(issued.rawToken());
        assertThat(persisted.status()).isEqualTo(TokenStatus.ISSUED);
        assertThat(persisted.eleccionId()).isEqualTo(ELECCION_ID);
        assertThat(persisted.funcionarioId()).isEqualTo(FUNCIONARIO_ID);
    }

    @Test
    @DisplayName("Should generate unique rawToken per invocation")
    void issue_shouldGenerateUniqueRawTokens_onEachCall() {
        // Given
        UUID anotherEleccionId = UUID.randomUUID();
        var command1 = new IssueVotingTokenCommand(1L, ELECCION_ID);
        var command2 = new IssueVotingTokenCommand(2L, anotherEleccionId);

        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(activaElection(ELECCION_ID)));
        when(electionRepository.findById(anotherEleccionId)).thenReturn(Optional.of(activaElection(anotherEleccionId)));
        when(eligibilityRepository.isEligibleForElection(anyLong(), any(UUID.class))).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(any(), anyLong())).thenReturn(false);
        when(participacionRepository.hasParticipated(any(), anyLong())).thenReturn(false);
        when(votingTokenRepository.saveIssued(any(VotingToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        IssuedVotingToken token1 = useCase.issue(command1);
        IssuedVotingToken token2 = useCase.issue(command2);

        // Then
        assertThat(token1.rawToken()).isNotEqualTo(token2.rawToken());
    }

    // ─── Election state guard ──────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw DomainException when election does not exist")
    void issue_shouldThrowDomainException_whenElectionNotFound() {
        // Given
        var command = new IssueVotingTokenCommand(FUNCIONARIO_ID, ELECCION_ID);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.issue(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Elección no encontrada");

        verify(votingTokenRepository, never()).saveIssued(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election is PROGRAMADA, not ACTIVA")
    void issue_shouldThrowDomainException_whenElectionIsProgramada() {
        // Given
        var command = new IssueVotingTokenCommand(FUNCIONARIO_ID, ELECCION_ID);
        when(electionRepository.findById(ELECCION_ID))
                .thenReturn(Optional.of(electionWithStatus(ELECCION_ID, ElectionStatus.PROGRAMADA)));

        // When & Then
        assertThatThrownBy(() -> useCase.issue(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no está ACTIVA");

        verify(votingTokenRepository, never()).saveIssued(any());
    }

    @Test
    @DisplayName("Should throw DomainException when election is FINALIZADA")
    void issue_shouldThrowDomainException_whenElectionIsFinalizada() {
        // Given
        var command = new IssueVotingTokenCommand(FUNCIONARIO_ID, ELECCION_ID);
        when(electionRepository.findById(ELECCION_ID))
                .thenReturn(Optional.of(electionWithStatus(ELECCION_ID, ElectionStatus.FINALIZADA)));

        // When & Then
        assertThatThrownBy(() -> useCase.issue(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no está ACTIVA");

        verify(votingTokenRepository, never()).saveIssued(any());
    }

    // ─── Census eligibility ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw DomainException when funcionario is NOT in census for this election")
    void issue_shouldThrowDomainException_whenFuncionarioNotInCensus() {
        // Given
        var command = new IssueVotingTokenCommand(FUNCIONARIO_ID, ELECCION_ID);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(activaElection()));
        when(eligibilityRepository.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> useCase.issue(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no está habilitado");

        verify(votingTokenRepository, never()).saveIssued(any());
    }

    @Test
    @DisplayName("Should issue token when census is empty (backward compatibility fallback)")
    void issue_shouldIssueToken_whenCensusIsEmptyAndFuncionarioIsGloballyEligible() {
        // Given — isEligibleForElection returns true when census is empty (adapter-level fallback)
        var command = new IssueVotingTokenCommand(FUNCIONARIO_ID, ELECCION_ID);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(activaElection()));
        when(eligibilityRepository.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(false);
        when(participacionRepository.hasParticipated(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(false);
        when(votingTokenRepository.saveIssued(any(VotingToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        IssuedVotingToken result = useCase.issue(command);

        // Then — token issued successfully
        assertThat(result.rawToken()).isNotBlank();
        assertThat(result.tokenId()).isNotNull();
    }

    // ─── Legacy eligibility checks ────────────────────────────────────────────

    @Test
    @DisplayName("Should throw DomainException when funcionario already has an ISSUED token for this election")
    void issue_shouldThrowDomainException_whenTokenAlreadyIssued() {
        // Given
        var command = new IssueVotingTokenCommand(FUNCIONARIO_ID, ELECCION_ID);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(activaElection()));
        when(eligibilityRepository.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.issue(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ya tiene un token");

        verify(votingTokenRepository, never()).saveIssued(any());
    }

    @Test
    @DisplayName("Should throw DomainException when funcionario has already voted in the election")
    void issue_shouldThrowDomainException_whenFuncionarioHasAlreadyVoted() {
        // Given
        var command = new IssueVotingTokenCommand(FUNCIONARIO_ID, ELECCION_ID);
        when(electionRepository.findById(ELECCION_ID)).thenReturn(Optional.of(activaElection()));
        when(eligibilityRepository.isEligibleForElection(FUNCIONARIO_ID, ELECCION_ID)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(false);
        when(participacionRepository.hasParticipated(ELECCION_ID, FUNCIONARIO_ID)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.issue(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ya votó");

        verify(votingTokenRepository, never()).saveIssued(any());
    }

    // ─── Test data factories ──────────────────────────────────────────────────

    private Election activaElection() {
        return activaElection(ELECCION_ID);
    }

    private Election activaElection(UUID id) {
        return electionWithStatus(id, ElectionStatus.ACTIVA);
    }

    private Election electionWithStatus(UUID id, ElectionStatus status) {
        return new Election(
                id,
                "EL-001",
                "Test Election",
                status,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );
    }
}
