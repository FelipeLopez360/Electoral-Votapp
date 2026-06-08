package co.com.votapp.ws.voting.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
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

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    private IssueVotingTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new IssueVotingTokenUseCaseImpl(votingTokenRepository, eligibilityRepository, participacionRepository);
    }

    @Test
    @DisplayName("Should return IssuedVotingToken with rawToken and tokenId when all conditions are met")
    void issue_shouldReturnIssuedToken_whenFuncionarioIsEligibleAndNoExistingToken() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        Long funcionarioId = 42L;
        var command = new IssueVotingTokenCommand(funcionarioId, eleccionId);

        when(eligibilityRepository.isEligible(funcionarioId)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(eleccionId, funcionarioId)).thenReturn(false);
        when(participacionRepository.hasParticipated(eleccionId, funcionarioId)).thenReturn(false);
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
        UUID eleccionId = UUID.randomUUID();
        Long funcionarioId = 42L;
        var command = new IssueVotingTokenCommand(funcionarioId, eleccionId);

        when(eligibilityRepository.isEligible(funcionarioId)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(eleccionId, funcionarioId)).thenReturn(false);
        when(participacionRepository.hasParticipated(eleccionId, funcionarioId)).thenReturn(false);
        when(votingTokenRepository.saveIssued(any(VotingToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        IssuedVotingToken issued = useCase.issue(command);

        // Then — the persisted token hash must NOT equal the rawToken
        ArgumentCaptor<VotingToken> captor = ArgumentCaptor.forClass(VotingToken.class);
        verify(votingTokenRepository).saveIssued(captor.capture());
        VotingToken persisted = captor.getValue();

        assertThat(persisted.tokenHash()).isNotEqualTo(issued.rawToken());
        assertThat(persisted.status()).isEqualTo(TokenStatus.ISSUED);
        assertThat(persisted.eleccionId()).isEqualTo(eleccionId);
        assertThat(persisted.funcionarioId()).isEqualTo(funcionarioId);
    }

    @Test
    @DisplayName("Should generate unique rawToken per invocation")
    void issue_shouldGenerateUniqueRawTokens_onEachCall() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        var command1 = new IssueVotingTokenCommand(1L, eleccionId);
        var command2 = new IssueVotingTokenCommand(2L, eleccionId);

        when(eligibilityRepository.isEligible(anyLong())).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(any(), anyLong())).thenReturn(false);
        when(participacionRepository.hasParticipated(any(), anyLong())).thenReturn(false);
        when(votingTokenRepository.saveIssued(any(VotingToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        IssuedVotingToken token1 = useCase.issue(command1);
        IssuedVotingToken token2 = useCase.issue(command2);

        // Then
        assertThat(token1.rawToken()).isNotEqualTo(token2.rawToken());
    }

    @Test
    @DisplayName("Should throw DomainException when funcionario is not eligible")
    void issue_shouldThrowDomainException_whenFuncionarioIsNotEligible() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        Long funcionarioId = 99L;
        var command = new IssueVotingTokenCommand(funcionarioId, eleccionId);

        when(eligibilityRepository.isEligible(funcionarioId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> useCase.issue(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("no está habilitado");

        verify(votingTokenRepository, never()).saveIssued(any());
    }

    @Test
    @DisplayName("Should throw DomainException when funcionario already has an ISSUED token for this election")
    void issue_shouldThrowDomainException_whenTokenAlreadyIssued() {
        // Given
        UUID eleccionId = UUID.randomUUID();
        Long funcionarioId = 42L;
        var command = new IssueVotingTokenCommand(funcionarioId, eleccionId);

        when(eligibilityRepository.isEligible(funcionarioId)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(eleccionId, funcionarioId)).thenReturn(true);

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
        UUID eleccionId = UUID.randomUUID();
        Long funcionarioId = 42L;
        var command = new IssueVotingTokenCommand(funcionarioId, eleccionId);

        when(eligibilityRepository.isEligible(funcionarioId)).thenReturn(true);
        when(votingTokenRepository.existsIssuedTokenFor(eleccionId, funcionarioId)).thenReturn(false);
        when(participacionRepository.hasParticipated(eleccionId, funcionarioId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> useCase.issue(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ya votó");

        verify(votingTokenRepository, never()).saveIssued(any());
    }
}
