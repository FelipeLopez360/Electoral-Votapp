package co.com.votapp.ws.voting.application.command;

import java.util.UUID;

/**
 * Command to cast an anonymous vote using a raw one-time token.
 *
 * <p>rawToken is the ephemeral secret provided by the voter.
 * The use case will hash it and look up the corresponding VotingToken.
 */
public record CastVoteCommand(
        String rawToken,
        UUID candidatoId
) {
    public CastVoteCommand {
        if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("rawToken must not be blank");
        if (candidatoId == null) throw new IllegalArgumentException("candidatoId must not be null");
    }
}
