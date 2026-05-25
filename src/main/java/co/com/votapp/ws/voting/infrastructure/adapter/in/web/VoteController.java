package co.com.votapp.ws.voting.infrastructure.adapter.in.web;

import co.com.votapp.ws.voting.application.command.CastVoteCommand;
import co.com.votapp.ws.voting.domain.port.in.CastVoteUseCase;
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

@RestController
@RequestMapping("/api/v1/votes")
@Tag(name = "Votes", description = "Cast and manage votes using single-use tokens")
public class VoteController {

    private final CastVoteUseCase castVoteUseCase;

    public VoteController(CastVoteUseCase castVoteUseCase) {
        this.castVoteUseCase = castVoteUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Cast a vote",
            description = "Registers an anonymous vote for a candidate in a given election. "
                    + "Each token can only be used once — duplicate submissions return 409 Conflict. "
                    + "MVP single-category model: no categoryId.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vote cast successfully"),
            @ApiResponse(responseCode = "400", description = "Malformed request body"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Token already used")
    })
    public ResponseEntity<Void> castVote(@RequestBody CastVoteRequest request) {
        CastVoteCommand command = new CastVoteCommand(
                request.rawToken(),
                UUID.fromString(request.candidateId())
        );
        castVoteUseCase.cast(command);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public record CastVoteRequest(
            String rawToken,
            String candidateId
    ) {
    }
}
