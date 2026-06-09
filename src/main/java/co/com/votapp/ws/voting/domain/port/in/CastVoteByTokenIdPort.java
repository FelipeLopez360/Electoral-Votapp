package co.com.votapp.ws.voting.domain.port.in;

import java.util.UUID;

/**
 * Input port: cast an anonymous vote using an internal token UUID.
 *
 * <p>Portal-facing variant of {@link CastVoteUseCase}: the rawToken is
 * unrecoverable (SHA-256 hashed), so the portal resolves the token by its
 * internal UUID (stored in the session context after ISSUED lookup).
 *
 * <p>The same atomic flow applies:
 * Redis SETNX lock → revalidate → markUsed → insert vote → markParticipation → audit → release.
 */
public interface CastVoteByTokenIdPort {

    /**
     * Cast a vote identified by the token's internal UUID.
     *
     * @param tokenId    the internal UUID of the ISSUED voting token
     * @param candidatoId the UUID of the selected candidate
     * @throws co.com.votapp.ws.common.exception.DomainException if the token is invalid,
     *         the election is not ACTIVA, or the vote cannot be cast
     */
    void castVote(UUID tokenId, UUID candidatoId);
}
