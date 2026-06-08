package co.com.votapp.ws.voting.infrastructure.adapter.in.web;

import co.com.votapp.ws.voting.application.command.IssueVotingTokenCommand;
import co.com.votapp.ws.voting.domain.IssuedVotingToken;
import co.com.votapp.ws.voting.domain.port.in.IssueVotingTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST adapter for voting token issuance.
 *
 * <p>Admin-only endpoint (HTTP Basic auth required). The rawToken returned here
 * is the ONLY time it is visible — it is never persisted. The caller must
 * deliver it to the voter immediately.
 */
@RestController
@RequestMapping("/api/v1/tokens")
@Tag(name = "Tokens", description = "Issue single-use voting tokens for eligible funcionarios")
public class TokenController {

    private final IssueVotingTokenUseCase issueVotingTokenUseCase;

    public TokenController(IssueVotingTokenUseCase issueVotingTokenUseCase) {
        this.issueVotingTokenUseCase = issueVotingTokenUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Issue a voting token",
            description = "Issues a single-use voting token for an eligible funcionario. "
                    + "The rawToken is shown ONCE — it is never persisted. "
                    + "Only valid for ACTIVA elections with an eligible, not-yet-voted funcionario. "
                    + "Requires admin credentials.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Token issued — rawToken returned once"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Funcionario not eligible, already voted, or election not ACTIVA")
    })
    public ResponseEntity<IssueTokenResponse> issueToken(@RequestBody IssueTokenRequest request) {
        IssueVotingTokenCommand command = new IssueVotingTokenCommand(
                request.funcionarioId(),
                UUID.fromString(request.eleccionId())
        );
        IssuedVotingToken result = issueVotingTokenUseCase.issue(command);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new IssueTokenResponse(result.rawToken(), result.tokenId().toString()));
    }

    // ── Request / Response records ──────────────────────────────────────────

    /**
     * Request body to issue a token.
     *
     * <p>{@code eleccionId} must be a valid UUID string.
     * {@code funcionarioId} is the DB primary key (SERIAL integer) of the funcionario.
     */
    public record IssueTokenRequest(
            String eleccionId,
            Long funcionarioId
    ) {}

    /**
     * Response containing the ephemeral rawToken and the internal tokenId.
     *
     * <p>The rawToken MUST be delivered to the voter immediately — it will never be
     * available again.
     */
    public record IssueTokenResponse(
            String rawToken,
            String tokenId
    ) {}
}
