package co.com.votapp.ws.voting.domain;

import java.util.UUID;

/**
 * Ephemeral result of issuing a voting token.
 *
 * <p>rawToken is the one-time secret returned to the caller and NEVER persisted.
 * tokenId is the internal UUID stored in tokens_votacion.id.
 */
public record IssuedVotingToken(
        String rawToken,
        UUID tokenId
) {
    public IssuedVotingToken {
        if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("rawToken must not be blank");
        if (tokenId == null) throw new IllegalArgumentException("tokenId must not be null");
    }
}
