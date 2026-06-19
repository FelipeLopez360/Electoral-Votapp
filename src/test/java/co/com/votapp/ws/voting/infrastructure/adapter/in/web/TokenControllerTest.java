package co.com.votapp.ws.voting.infrastructure.adapter.in.web;

import co.com.votapp.ws.voting.domain.IssuedVotingToken;
import co.com.votapp.ws.voting.domain.port.in.IssueVotingTokenUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TokenController}.
 *
 * <p>No Spring context — collaborators are mocked via Mockito.
 * Verifies HTTP mapping, command assembly, and response projection.
 */
@DisplayName("TokenController - Token issuance REST adapter")
@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    @Mock
    private IssueVotingTokenUseCase issueVotingTokenUseCase;

    private TokenController controller;

    private static final UUID ELECTION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TOKEN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Long FUNCIONARIO_ID = 42L;
    private static final String RAW_TOKEN = "super-secret-raw-token-xyz";

    @BeforeEach
    void setUp() {
        controller = new TokenController(issueVotingTokenUseCase);
    }

    // ── issueToken ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 201 with rawToken and tokenId when issuance succeeds")
    void issueToken_shouldReturn201WithTokenData_whenIssuanceSucceeds() {
        // Given
        IssuedVotingToken issued = new IssuedVotingToken(RAW_TOKEN, TOKEN_ID);
        when(issueVotingTokenUseCase.issue(any())).thenReturn(issued);

        TokenController.IssueTokenRequest request = new TokenController.IssueTokenRequest(
                ELECTION_ID, FUNCIONARIO_ID
        );

        // When
        ResponseEntity<TokenController.IssueTokenResponse> response = controller.issueToken(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().rawToken()).isEqualTo(RAW_TOKEN);
        assertThat(response.getBody().tokenId()).isEqualTo(TOKEN_ID.toString());
    }

    @Test
    @DisplayName("Should build IssueVotingTokenCommand with UUID and funcionarioId from typed request")
    void issueToken_shouldBuildCommandWithCorrectFields_fromTypedRequest() {
        // Given
        IssuedVotingToken issued = new IssuedVotingToken(RAW_TOKEN, TOKEN_ID);
        when(issueVotingTokenUseCase.issue(any())).thenReturn(issued);

        // Using typed UUID directly — no more String → UUID.fromString() in controller
        TokenController.IssueTokenRequest request = new TokenController.IssueTokenRequest(
                ELECTION_ID, FUNCIONARIO_ID
        );

        // When
        controller.issueToken(request);

        // Then
        ArgumentCaptor<co.com.votapp.ws.voting.application.command.IssueVotingTokenCommand> captor =
                ArgumentCaptor.forClass(co.com.votapp.ws.voting.application.command.IssueVotingTokenCommand.class);
        verify(issueVotingTokenUseCase).issue(captor.capture());
        assertThat(captor.getValue().eleccionId()).isEqualTo(ELECTION_ID);
        assertThat(captor.getValue().funcionarioId()).isEqualTo(FUNCIONARIO_ID);
    }

    @Test
    @DisplayName("Should propagate domain exception when use case throws")
    void issueToken_shouldPropagateDomainException_whenUseCaseThrows() {
        // Given
        when(issueVotingTokenUseCase.issue(any()))
                .thenThrow(new co.com.votapp.ws.common.exception.DomainException("Funcionario not eligible"));

        TokenController.IssueTokenRequest request = new TokenController.IssueTokenRequest(
                ELECTION_ID, FUNCIONARIO_ID
        );

        // When & Then
        assertThatThrownBy(() -> controller.issueToken(request))
                .isInstanceOf(co.com.votapp.ws.common.exception.DomainException.class)
                .hasMessageContaining("not eligible");
    }

    // ── Task 4.2: UUID validation now at HTTP layer, not controller body ───────

    @Test
    @DisplayName("Should pass typed UUID directly to command — no manual UUID.fromString in controller")
    void issueToken_shouldPassUuidDirectlyToCommand_withoutStringParsing() {
        // Given — typed UUID, confirming no parsing happens inside the controller method
        IssuedVotingToken issued = new IssuedVotingToken(RAW_TOKEN, TOKEN_ID);
        when(issueVotingTokenUseCase.issue(any())).thenReturn(issued);

        UUID differentElectionId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        TokenController.IssueTokenRequest request = new TokenController.IssueTokenRequest(
                differentElectionId, FUNCIONARIO_ID
        );

        // When
        controller.issueToken(request);

        // Then — command must carry the exact UUID passed in (no re-parsing or mutation)
        ArgumentCaptor<co.com.votapp.ws.voting.application.command.IssueVotingTokenCommand> captor =
                ArgumentCaptor.forClass(co.com.votapp.ws.voting.application.command.IssueVotingTokenCommand.class);
        verify(issueVotingTokenUseCase).issue(captor.capture());
        assertThat(captor.getValue().eleccionId()).isEqualTo(differentElectionId);
    }
}
