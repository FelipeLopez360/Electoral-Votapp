package co.com.votapp.ws.voting.domain.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Input port: cast an anonymous multi-selection vote using an internal token UUID.
 *
 * <p>Portal-facing variant of the cast-vote flow: the rawToken is unrecoverable
 * (SHA-256 hashed), so the portal resolves the token by its internal UUID
 * (stored in the session context after ISSUED lookup).
 *
 * <p>The same atomic flow applies:
 * Redis SETNX lock → revalidate → dedupe → validate (count, blank exclusivity, membership)
 * → markUsed → insert N votos → markParticipation → audit → release.
 *
 * <p>Multi-vote contract: one lock, one {@code markUsed}, N {@code Vote} rows inserted.
 */
public interface CastVoteByTokenIdPort {

    /**
     * Cast one or more votes identified by the token's internal UUID.
     *
     * <p>The list is deduplicated before count and membership validation.
     * Blank vote mutual exclusivity is enforced: if the "Voto en Blanco" candidate
     * is present in the list, no other candidate may be included.
     *
     * @param tokenId      the internal UUID of the ISSUED voting token
     * @param candidatoIds the UUIDs of the selected candidates (at least one; duplicates tolerated)
     * @throws co.com.votapp.ws.common.exception.DomainException if the token is invalid,
     *         the election is not ACTIVA, the vote cannot be cast, count exceeds
     *         {@code maxVotosPorElector}, or blank vote exclusivity is violated
     */
    void castVote(UUID tokenId, List<UUID> candidatoIds);
}
