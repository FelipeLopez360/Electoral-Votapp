package co.com.votapp.ws.voting.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain representation of a voting token.
 *
 * <p>The raw token secret is NEVER stored here or in the DB.
 * Only the SHA-256 hash is persisted.
 */
public record VotingToken(
        UUID id,
        UUID eleccionId,
        Long funcionarioId,
        String tokenHash,
        TokenStatus status,
        Instant issuedAt
) {
    public VotingToken {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
        if (funcionarioId == null) throw new IllegalArgumentException("funcionarioId must not be null");
        if (tokenHash == null || tokenHash.isBlank()) throw new IllegalArgumentException("tokenHash must not be blank");
        if (status == null) throw new IllegalArgumentException("status must not be null");
        if (issuedAt == null) throw new IllegalArgumentException("issuedAt must not be null");
    }
}
