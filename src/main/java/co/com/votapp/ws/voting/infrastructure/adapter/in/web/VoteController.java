package co.com.votapp.ws.voting.infrastructure.adapter.in.web;

import co.com.votapp.ws.voting.application.usecase.CastVoteUseCase;
import co.com.votapp.ws.voting.domain.Vote;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/votes")
public class VoteController {
    private final CastVoteUseCase castVoteUseCase;

    public VoteController(CastVoteUseCase castVoteUseCase) {
        this.castVoteUseCase = castVoteUseCase;
    }

    @PostMapping
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
