package co.com.votapp.ws.voting.infrastructure.adapter.in.web;

import co.com.votapp.ws.voting.application.usecase.CastVoteUseCase;
import co.com.votapp.ws.voting.domain.Vote;
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

import java.time.Instant;

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
            description = "Registers a vote for a candidate in a given election category. "
                    + "Each token can only be used once — duplicate submissions return 409 Conflict.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vote cast successfully"),
            @ApiResponse(responseCode = "400", description = "Malformed request body"),
            @ApiResponse(responseCode = "401", description = "Authentication required — HTTP Basic credentials missing or invalid"),
            @ApiResponse(responseCode = "409", description = "Token already used — this vote token has already been redeemed")
    })
    public ResponseEntity<Void> castVote(@RequestBody CastVoteRequest request) {
        Vote vote = new Vote(
            request.tokenId(),
            request.electionId(),
            request.candidateId(),
            request.categoryId(),
            Instant.now()
        );
        castVoteUseCase.cast(vote);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public record CastVoteRequest(
        String tokenId,
        Long electionId,
        Long candidateId,
        Long categoryId
    ) {
    }
}

